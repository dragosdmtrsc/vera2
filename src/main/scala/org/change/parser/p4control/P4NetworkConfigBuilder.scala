package org.change.parser.p4control

import org.antlr.v4.runtime._;
import org.antlr.v4.runtime.tree._;
import generated.p4control.P4GrammarBaseListener
import generated.p4control.P4GrammarParser
import generated.p4control.P4GrammarParser._
import org.change.v2.abstractnet.generic._
import org.change.v2.abstractnet.generic.NetworkConfig
import org.change.v2.abstractnet.generic.GenericElement
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.instructions.FloatingConstraint
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.constraint.Constraint
import org.change.v2.analysis.expression.abst._
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive._

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.mutable.{ArrayBuffer,ListBuffer}
//import scala.collection.jcl.IdentityHashMap

class P4NetworkConfigBuilder(val configName: Option[String] = None) extends P4GrammarBaseListener {

  private val currentPort = -1
  private var currentTableName:ListBuffer[String] = new ListBuffer[String]
  private var currentInstructions:ListBuffer[ListBuffer[Instruction]] = new ListBuffer[ListBuffer[Instruction]]
  private var currentTableInvocation = 0;

  private val elements = new ArrayBuffer[GenericElement]()
  private var pathBuilder: ArrayBuffer[PathComponent] = _
  private val foundPaths = ArrayBuffer[List[PathComponent]]()
  val ports:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]();
  val portsLiveBlocks:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]();

  val values:ParseTreeProperty[Integer] = new ParseTreeProperty[Integer]();
  val constraints:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]();
  val symbols:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]();
  val expressions:ParseTreeProperty[FloatingExpression] = new ParseTreeProperty[FloatingExpression]();

  val blocks:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]();
  val blocksLast:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]();

  override def enterControl_function_declaration(ctx:Control_function_declarationContext){
    println("Enter control function "+ ctx.control_fn_name.getText());
  }

  override def exitControl_function_declaration(ctx:Control_function_declarationContext){
    ports += (ctx.control_fn_name.getText() -> blocks.get(ctx.control_block))

    println("\n\n------------------------------\nGenerated SEFL CODE for function "+ ctx.control_fn_name.getText() +"\n------------------------------\n")
    for ((x,y) <- ports){
      println(x+":");
      for ( z <- y)
        println("\t"+z);
    }

    if(currentInstructions.length!=0){
      System.out.println("Not expecting instructions at exit of ctrl function!\n"+currentInstructions);
    }
  }

  override def enterControl_block(ctx:Control_blockContext){
    currentInstructions.prepend(new ListBuffer[Instruction])
    //associate current instructions to the context
    blocks.put(ctx,currentInstructions.head)
  }

  override def exitControl_block(ctx:Control_blockContext){
    blocksLast.put(ctx,currentInstructions.head)
    currentInstructions.remove(0)
  }

  override def exitApply_table_call(ctx:Apply_table_callContext){
    val portName = ctx.table_name.getText()+"_"+currentTableInvocation
    println("Apply matched " + ctx.table_name.getText());

    currentInstructions.head.append(Forward(portName))

    //here we should call Radu's action parsing code instead
    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(portName+"_output"));

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
    val portName = ctx.table_name.getText()+"_"+currentTableInvocation
    currentInstructions.head.append(Forward(portName));
    currentTableName.prepend(portName)

    currentInstructions.remove(0)

    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(currentTableName.head+"_output"));

    currentTableInvocation+=1
    //here we should call Radu's action parsing code instead
    //currentInstructions.head.append(Forward(ctx.table_name.getText()+"_output"));
  }

  override def exitApply_and_select_block(ctx:Apply_and_select_blockContext){
    println("Apply and select bmatched " + ctx.table_name.getText());

    //adding fork if there are multiple forward instructions

    val z = ports(currentTableName.head).reverse.takeWhile(
      _ match {
        case x:Forward => true
        case _ => false
      })

    if (z.length>1){
      ports(currentTableName.head).trimEnd(z.length);
      ports(currentTableName.head).append(Fork(z));
    }

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (currentTableName.head+"_output" -> currentInstructions.head)
    currentTableName.remove(0);
  }

  override def enterAction_case(ctx:Action_caseContext){
    //must make room for control block instructions!
    //this will create a new control block with a specific port.

  }

  override def exitAction_case(ctx:Action_caseContext){
    val portName = currentTableName.head + "_" + ctx.action_or_default().getText()

    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName));
    //currentInstructions.prepend(new ListBuffer[Instruction])

    ctx.action_or_default.getText() match {
      case "default" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case _ =>
        //lookup action name in the table, if found add the following
        ports += (portName  -> blocks.get(ctx.control_block))
    }

    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
  }

  override def exitHit_miss_case(ctx:Hit_miss_caseContext){
    val portName = currentTableName.head + "_" + ctx.hit_or_miss.getText()

    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName));

    ctx.hit_or_miss.getText() match {
      case "hit" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case "miss" =>
        ports += (portName -> blocks.get(ctx.control_block))
    }

    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
  }

  override def exitField_name(ctx:Field_nameContext) {
    println("Matched field name "+ctx.getText());
  }

  override def exitConst_value(ctx:Const_valueContext) {
    println("Matched const value "+ctx.getText());

    values.put(ctx,ctx.getText().toInt);
  }

  override def exitControl_statement(ctx:Control_statementContext){
    if (ctx.control_fn_name!=null){
      currentInstructions.head.append(Forward(ctx.control_fn_name.getText()));
    }
  }

/*if_else_statement : 'if' '(' bool_expr ')' control_block ( else_block )? ;

else_block : 'else' control_block
           | 'else' if_else_statement ;
 */

  override def exitIf_else_statement(ctx:If_else_statementContext){
    //bool_expr should be a constrain instruction we can use in the IF.
    val labelName = "if_"+currentTableInvocation;
    currentTableInvocation += 1;

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
      ));

    blocksLast.get(ctx.control_block).append(Forward(labelName))
    if (ctx.else_block!=null)
      blocksLast.get(ctx.else_block.control_block).append(Forward(labelName))

    currentInstructions.remove(0);
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (labelName -> currentInstructions.head)
  }

  override def exitElse_block(ctx:Else_blockContext){
    blocks.put(ctx,blocks.get(ctx.control_block));
  }

//exp : exp bin_op exp # compound_exp
//      | un_op exp # unary_exp
//      | field_ref # field_red_exp
//      | value # value_exp
//      | '(' exp ')' # par_exp ;

  override def exitCompound_exp(ctx:Compound_expContext){
    println("FIXME Compound exp "+ctx.getText());
  };

  override def exitUnary_exp(ctx:Unary_expContext){
    println("FIXME Unary exp "+ctx.getText());
  };

  override def exitField_red_exp(ctx:Field_red_expContext){
    //need to convert the field reference to a number; the current code will issue metadata accesses
    expressions.put(ctx,:@(ctx.getText()))
  };

  override def exitValue_exp(ctx:Value_expContext){
    expressions.put(ctx,ConstantValue(ctx.getText().toInt));
  }

  override def exitPar_exp(ctx:Par_expContext){
    println("Matched par expression "+ctx.getText());
    expressions.put(ctx,expressions.get(ctx.exp));
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
    blocks.get(ctx).append(Constrain(ctx.header_ref.getText,:==:(ConstantValue(1))))
  };

  override def exitCompound_bool_expr(ctx:Compound_bool_exprContext){
    println("Matched compound bool expr. FIXME ");

    // bool_op can be "and" or "or"
    blocks.put(ctx,new ListBuffer[Instruction]);


    ctx.bool_op.getText match {
      case "and" =>
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(0)))
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(1)))

      case "or" =>
        blocks.get(ctx).append(
          Fork(
            InstructionBlock(blocks.get(ctx.bool_expr(0))),
            InstructionBlock(blocks.get(ctx.bool_expr(1)))
          )
        )
    }
  };

  override def exitPar_bool_expr(ctx:Par_bool_exprContext){
    blocks.put(ctx,blocks.get(ctx.bool_expr))
  };

  override def exitRelop_bool_expr(ctx:Relop_bool_exprContext){
    println("Matched relop bool expr " + expressions.get(ctx.exp(0))+ " " + expressions.get(ctx.exp(1)));

    val exp1 = expressions.get(ctx.exp(0))
    val exp2 = expressions.get(ctx.exp(1))

    blocks.put(ctx,new ListBuffer[Instruction])

    exp1 match {
      case Symbol(x) =>
        println ("Found symbol "+x);

        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" => println ("!= relop");
            constraints.put(ctx, :~:(:==:(exp2)))
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" => println ("< relop");
            constraints.put(ctx, :<:(exp2))            
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))

          case ">" => println ("> relop");
            constraints.put(ctx, :>:(exp2))            
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))

          case _ => println("Unknown relop "+ctx.rel_op);
        }

      case Address(x) =>
        println("Found address "+x);
        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" => println ("!= relop");
            constraints.put(ctx, :~:(:==:(exp2)))            
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" => println ("< relop");
            constraints.put(ctx, :<:(exp2))            
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))

          case ">" => println ("> relop");
            constraints.put(ctx, :>:(exp2))            
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))

          case _ => println("Unknown relop "+ctx.rel_op);
        }

      case _ => println ("FIXME RELOP_EXPR")
    }
    //exp1 must be either metadata or field ref
    //exp2 can be either constant, etc. 
  };

  override def exitNegated_bool_expr(ctx:Negated_bool_exprContext){
    val exp = constraints.get(ctx.bool_expr)
    constraints.put(ctx, :~:(exp))
  };

  override def exitConst_bool(ctx:Const_boolContext){
    println("Matched const bool expr."); 

    values.put(ctx, (if (ctx.getText().equalsIgnoreCase("true")) 1 else 0));
  };

  def buildNetworkConfig() = new NetworkConfig(configName, elements.map(element => (element.name, element)).toMap, foundPaths.toList)

  private def buildElementName(elementName: String): String =  elementName

}
