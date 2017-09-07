package org.change.parser.p4.control

import java.util
import java.util.UUID

import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}
import generated.parse.p4.P4GrammarParser._
import org.antlr.v4.runtime.tree._
import org.change.parser.p4.ValueSpecificationParser
import org.change.v2.abstractnet.generic.{GenericElement, NetworkConfig, _}
import org.change.v2.analysis.expression.abst._
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{FloatingConstraint, _}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
//import scala.collection.jcl.IdentityHashMap

class P4NetworkConfigBuilder extends P4GrammarBaseListener {

  private val currentPort = -1
  private var currentTableName:ListBuffer[String] = new ListBuffer[String]
  private var currentInstructions:ListBuffer[ListBuffer[Instruction]] = new ListBuffer[ListBuffer[Instruction]]
  private var currentTableInvocation = 0

  private val elements = new ArrayBuffer[GenericElement]()
  private var pathBuilder: ArrayBuffer[PathComponent] = _
  private val foundPaths = ArrayBuffer[List[PathComponent]]()
  private val ports:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]()
  private val portsLiveBlocks:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]()

  private val values:ParseTreeProperty[Integer] = new ParseTreeProperty[Integer]()
  private val constraints:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]()
  private val symbols:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]()
  private val expressions:ParseTreeProperty[FloatingExpression] = new ParseTreeProperty[FloatingExpression]()

  private val blocks:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]()
  private val blocksLast:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]()

  override def enterControl_function_declaration(ctx:Control_function_declarationContext){
    println("Enter control function "+ ctx.control_fn_name.getText)
    ctx.controlFunctionName = ctx.control_fn_name().NAME().getText
    ctx.control_block().parent = s"control.${ctx.controlFunctionName}"
  }

  override def exitControl_function_declaration(ctx:Control_function_declarationContext){
    ports += (ctx.control_fn_name.getText -> blocks.get(ctx.control_block))

    this.instructions.put(s"control.${ctx.controlFunctionName}", Forward(s"control.${ctx.controlFunctionName}" + "[0]"))

    println("\n\n------------------------------\nGenerated SEFL CODE for function "+ ctx.control_fn_name.getText +"\n------------------------------\n")
    for ((x,y) <- ports){
      println(x+":")
      for ( z <- y)
        println("\t"+z)
    }

    if(currentInstructions.length!=0){
      System.out.println("Not expecting instructions at exit of ctrl function!\n"+currentInstructions)
    }
  }

  override def enterControl_block(ctx:Control_blockContext) {
    currentInstructions.prepend(new ListBuffer[Instruction])
    //associate current instructions to the context
    blocks.put(ctx,currentInstructions.head)
    var  i =0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (cs <- ctx.control_statement()) {
      cs.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  val instructions : Map[String, Instruction]  = Map[String, Instruction]()
  val links : Map[String, String] = Map[String, String]()
  override def exitControl_block(ctx:Control_blockContext){
    blocksLast.put(ctx,currentInstructions.head)
    currentInstructions.remove(0)
    var  i = 0
    for (cs <- ctx.control_statement()) {
      ctx.instructions.add(cs.instruction)
      // the parent should add the mappings to get them straight
      this.instructions.put(s"${ctx.parent}[$i]", cs.instruction)
      if (i + 1 < ctx.control_statement().size()) {
        this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}[${i + 1}]")
      } else {
        this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}.out")
      }
      i = i + 1
    }
  }

  override def enterControl_statement(ctx: Control_statementContext): Unit = {
    if (ctx.apply_table_call() != null) {
      ctx.apply_table_call().parent = ctx.parent
    } else if (ctx.apply_and_select_block() != null) {
      ctx.apply_and_select_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent
    }
  }

  override def exitControl_statement(ctx:Control_statementContext){
    if (ctx.control_fn_name!=null){
      val cfName = ctx.control_fn_name.getText
      val execId = UUID.randomUUID().toString
      ctx.instruction = Forward(s"$cfName.$execId")
      this.links.put(s"control.$cfName.out.$execId", ctx.parent + ".out")
      currentInstructions.head.append(ctx.instruction)
    } else if (ctx.apply_table_call() != null) {
      ctx.instruction = ctx.apply_table_call().instruction
    } else if (ctx.apply_and_select_block() != null) {
      ctx.instruction = ctx.apply_and_select_block().instruction
    } else if (ctx.if_else_statement() != null) {
      ctx.instruction = ctx.if_else_statement().instruction
    } else {
      throw new IllegalStateException("Got null for all statements")
    }
  }

  override def exitApply_table_call(ctx:Apply_table_callContext){
    val execId = UUID.randomUUID().toString
    val portName = s"table.${ctx.table_name().getText}.in.$execId"
    println("Apply matched " + ctx.table_name.getText)

    ctx.instruction = Forward(s"table.${ctx.table_name().getText}.in.$execId")
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", ctx.parent + ".out")

    currentInstructions.head.append(Forward(portName))

    //here we should call Radu's action parsing code instead
    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(portName+"_output"))

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName+"_output" -> currentInstructions.head)

    currentTableInvocation+=1
  }

/*
 apply_and_select_block : 'apply' '(' table_name ')' '{' ( case_list )? '}' ;
 case_list : action_case+ # case_list_action
          | hit_miss_case+  # case_list_hitmiss;
 action_case : action_or_default control_block ;
 action_or_default : action_name | 'default' ;
 hit_miss_case : hit_or_miss control_block ;
 hit_or_miss : 'hit' | 'miss' ;
*/

  override def enterApply_and_select_block(ctx:Apply_and_select_blockContext){
    val portName = s"table.${ctx.table_name().getText}.in.${UUID.randomUUID().toString}"
    ctx.case_list().parent = ctx.parent

    currentInstructions.head.append(Forward(portName))
    currentTableName.prepend(portName)

    currentInstructions.remove(0)

    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(currentTableName.head+"_output"))

    currentTableInvocation+=1
    //here we should call Radu's action parsing code instead
    //currentInstructions.head.append(Forward(ctx.table_name.getText()+"_output"));
  }


  override def exitApply_and_select_block(ctx:Apply_and_select_blockContext){
    println("Apply and select bmatched " + ctx.table_name.getText)

    //adding fork if there are multiple forward instructions
    // TODO: Wire it up
    val execId = UUID.randomUUID().toString
    this.instructions.put(ctx.parent, Forward(s"table.${ctx.table_name().getText}.in.$execId"))
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", s"${ctx.parent}.select")
    this.instructions.put(s"${ctx.parent}.select",
      ctx.case_list().instructions.map(_.asInstanceOf[If]).foldRight(NoOp : Instruction)((x, acc) => {
        If (x.testInstr, x.thenWhat, acc)
      })
    )
    this.links.put(s"${ctx.parent}[${ctx.case_list().instructions.size() - 1}].out", s"${ctx.parent}.out")
    val z = ports(currentTableName.head).reverse.takeWhile(
      _ match {
        case x:Forward => true
        case _ => false
      })

    if (z.length>1){
      ports(currentTableName.head).trimEnd(z.length)
      ports(currentTableName.head).append(Fork(z))
    }

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (currentTableName.head+"_output" -> currentInstructions.head)
    currentTableName.remove(0)
  }

  override def enterCase_list_action(ctx: Case_list_actionContext): Unit = {
    var i = 0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (ac <- ctx.action_case()) {
      ac.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  override def exitCase_list_action(ctx: Case_list_actionContext): Unit = {
    var i = 0
    for (ac <- ctx.action_case()) {
      ctx.instructions.add(ac.instruction)
      i = i + 1
    }
  }

  override def enterCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (ac <- ctx.hit_miss_case()) {
      ac.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  override def exitCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    for (ac <- ctx.hit_miss_case()) {
      ctx.instructions.add(ac.instruction)
      i = i + 1
    }
  }

  override def enterHit_miss_case(ctx: Hit_miss_caseContext): Unit = {
    ctx.control_block().parent = ctx.parent
  }

  override def exitHit_miss_case(ctx:Hit_miss_caseContext){
    val portName = currentTableName.head + "_" + ctx.hit_or_miss.getText

    ctx.instruction = If (Constrain(currentTableName.head + ".Hit", :==:(ConstantValue(if (ctx.hit_or_miss().getText == "hit") 1 else 0))),
      Forward(ctx.parent + "[0]")
    )
    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName))

    ctx.hit_or_miss.getText match {
      case "hit" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case "miss" =>
        ports += (portName -> blocks.get(ctx.control_block))
    }

    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
  }

  override def enterAction_case(ctx:Action_caseContext){
    //must make room for control block instructions!
    //this will create a new control block with a specific port.
    ctx.control_block().parent = ctx.parent
  }

  override def exitAction_case(ctx:Action_caseContext){
    val portName = currentTableName.head + "_" + ctx.action_or_default().getText
    ctx.instruction = If (Constrain(ctx.action_or_default().getText + ".Fired", :==:(ConstantValue(1))),
      Forward(ctx.parent + "[0]")
    )
    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName))
    //currentInstructions.prepend(new ListBuffer[Instruction])

    ctx.action_or_default.getText match {
      case "default" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case _ =>
        //lookup action name in the table, if found add the following
        ports += (portName  -> blocks.get(ctx.control_block))
    }

    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
  }



  /*if_else_statement : 'if' '(' bool_expr ')' control_block ( else_block )? ;

  else_block : 'else' control_block
             | 'else' if_else_statement ;
   */

  override def enterIf_else_statement(ctx: If_else_statementContext): Unit = {
    ctx.control_block().parent = ctx.parent + "[if]"
    if (ctx.else_block() != null) {
      ctx.else_block().parent = ctx.parent + "[else]"
    }
  }

  override def exitIf_else_statement(ctx:If_else_statementContext){
    //bool_expr should be a constrain instruction we can use in the IF.
    val labelName = "if_"+currentTableInvocation
    currentTableInvocation += 1

    ctx.instruction = If (
      InstructionBlock(
        blocks.get(ctx.bool_expr)
      ),
      Forward(ctx.parent + "[if][0]"),
      Forward(s"${ctx.parent}[else][0]")
    )

    this.links.put(ctx.parent + "[if].out", s"${ctx.parent}.out")
    this.links.put(ctx.parent + "[else].out", s"${ctx.parent}.out")

    val constr = blocks.get(ctx.bool_expr).head
    blocks.get(ctx.bool_expr).remove(0)

    val negated = for (x <- blocks.get(ctx.bool_expr)) yield {
      x match {
        case ConstrainNamedSymbol(a,c,d) => ConstrainNamedSymbol(a, :~:(c),d)
        case ConstrainRaw(a,c,d) => ConstrainRaw(a, :~:(c),d)
      }
    }

    currentInstructions.head.append(
      If (constr,
        InstructionBlock(blocks.get(ctx.bool_expr) ++ blocks.get(ctx.control_block)),
        if (ctx.else_block==null) NoOp
        else InstructionBlock(negated ++ blocks.get(ctx.else_block))
      ))

    blocksLast.get(ctx.control_block).append(Forward(labelName))
    if (ctx.else_block!=null)
      blocksLast.get(ctx.else_block.control_block).append(Forward(labelName))

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (labelName -> currentInstructions.head)
  }

  override def enterElse_block(ctx: Else_blockContext): Unit = {
    if (ctx.control_block() != null) {
      ctx.control_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent

    }
  }

  override def exitElse_block(ctx:Else_blockContext){
    if (ctx.control_block() != null) {
      //Nothing to do here, the control_block() will place
      // instructions in the table correctlys
//      ctx.control_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent
      //TODO: Here, please insert into the instruction table parent[0]
      //referencing the if_else_statement().instruction() and some links perhaps
      this.instructions.put(ctx.parent + "[0]", ctx.if_else_statement().instruction)
    }
    blocks.put(ctx,blocks.get(ctx.control_block))
  }


  override def exitField_name(ctx:Field_nameContext) {
    println("Matched field name "+ctx.getText)
  }

  override def exitConst_value(ctx: P4GrammarParser.Const_valueContext): Unit =
  //TODO: Support the width field
    ctx.constValue = (if (ctx.getText().startsWith("-")) -1 else 1) *
      ctx.unsigned_value().unsignedValue

  override def exitHexadecimalUValue(ctx: P4GrammarParser.HexadecimalUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.hexToInt(ctx.Hexadecimal_value().getText.substring(2))

  override def exitDecimalUValue(ctx: P4GrammarParser.DecimalUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.decimalToInt(ctx.Decimal_value().getText)

  override def exitBinaryUValue(ctx: P4GrammarParser.BinaryUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.binaryToInt(ctx.Binary_value().getText)


//exp : exp bin_op exp # compound_exp
//      | un_op exp # unary_exp
//      | field_ref # field_red_exp
//      | value # value_exp
//      | '(' exp ')' # par_exp ;

  override def exitCompound_exp(ctx:Compound_expContext){
    println("FIXME Compound exp "+ctx.getText)
  }

  override def exitUnary_exp(ctx:Unary_expContext){
    println("FIXME Unary exp "+ctx.getText)
  }

  override def exitField_red_exp(ctx:Field_red_expContext){
    //need to convert the field reference to a number; the current code will issue metadata accesses
    expressions.put(ctx,:@(ctx.getText))
  }

  override def exitValue_exp(ctx:Value_expContext){
    expressions.put(ctx,ConstantValue(ctx.getText.toInt))
  }

  override def exitPar_exp(ctx:Par_expContext){
    println("Matched par expression "+ctx.getText)
    expressions.put(ctx,expressions.get(ctx.exp))
  }

//bool_expr : 'valid' '(' header_ref ')' # valid_bool_expr
//	  | bool_expr bool_op bool_expr # compound_bool_expr
//	  | 'not' bool_expr # negated_bool_expr
//	  | '(' bool_expr ')' # par_bool_expr
//	  | exp rel_op exp # relop_bool_expr
//	  | 'true' # const_bool
//  | 'false' # const_bool;

  override def exitValid_bool_expr(ctx:Valid_bool_exprContext){
    //check with radu - need valid instruction
    blocks.put(ctx,new ListBuffer[Instruction])

    //the header parsing code should create the metadata named as the header (including [index]!?))
    blocks.get(ctx).append(Constrain(ctx.header_ref.getText + ".IsValid",:==:(ConstantValue(1))))
  }

  override def exitCompound_bool_expr(ctx:Compound_bool_exprContext){
    println("Matched compound bool expr. FIXME ")

    // bool_op can be "and" or "or"
    blocks.put(ctx,new ListBuffer[Instruction])


    ctx.bool_op.getText match {
      case "and" =>
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(0)))
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(1)))

      case "or" =>
        blocks.get(ctx).append(
          Fork(
            List[Instruction](InstructionBlock(blocks.get(ctx.bool_expr(0))),InstructionBlock(blocks.get(ctx.bool_expr(1)))
          )
          )
        )
    }
  }

  override def exitPar_bool_expr(ctx:Par_bool_exprContext){
    blocks.put(ctx,blocks.get(ctx.bool_expr))
  }

  override def exitRelop_bool_expr(ctx:Relop_bool_exprContext){
    println("Matched relop bool expr " + expressions.get(ctx.exp(0))+ " " + expressions.get(ctx.exp(1)))

    val exp1 = expressions.get(ctx.exp(0))
    val exp2 = expressions.get(ctx.exp(1))

    blocks.put(ctx,new ListBuffer[Instruction])

    exp1 match {
      case Symbol(x) =>
        println ("Found symbol "+x)

        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" => println ("!= relop")
            constraints.put(ctx, :~:(:==:(exp2)))
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" => println ("< relop")
            constraints.put(ctx, :<:(exp2))            
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))

          case ">" => println ("> relop")
            constraints.put(ctx, :>:(exp2))            
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))

          case _ => println("Unknown relop "+ctx.rel_op);
        }

      case Address(x) =>
        println("Found address "+x)
        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" => println ("!= relop")
            constraints.put(ctx, :~:(:==:(exp2)))            
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" => println ("< relop")
            constraints.put(ctx, :<:(exp2))            
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))

          case ">" => println ("> relop")
            constraints.put(ctx, :>:(exp2))            
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))

          case _ => println("Unknown relop "+ctx.rel_op);
        }

      case _ => println ("FIXME RELOP_EXPR")
    }
    //exp1 must be either metadata or field ref
    //exp2 can be either constant, etc. 
  }

  override def exitNegated_bool_expr(ctx:Negated_bool_exprContext){
    val exp = constraints.get(ctx.bool_expr)
    constraints.put(ctx, :~:(exp))
  }

  override def exitConst_bool(ctx:Const_boolContext){
    println("Matched const bool expr.")

    values.put(ctx, (if (ctx.getText.equalsIgnoreCase("true")) 1 else 0))
  }

  def buildNetworkConfig() = new NetworkConfig(None, elements.map(element => (element.name, element)).toMap, foundPaths.toList)

}
