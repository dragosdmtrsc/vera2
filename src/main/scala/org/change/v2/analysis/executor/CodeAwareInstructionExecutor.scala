package org.change.v2.analysis.executor

import java.util.UUID

import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.types.LongType

/**
  * Created by dragos on 16.10.2017.
  */
class CodeAwareInstructionExecutor(val program : Map[String, Instruction],
                                   solver : Solver) extends OVSExecutor(solver) {

  override def executeForward(instruction: Forward, s: State, v: Boolean): (List[State], List[State]) = {
    val crt = super.executeForward(instruction, s, v)._1.head
    if (!program.contains(crt.history.head)) {
      (List[State](crt), Nil)
    } else {
      val crtI = program(crt.history.head)
      this.execute(crtI, crt, v)
    }
  }

  def executeInternal(instruction : Instruction, state : State, verbose : Boolean) : (List[State], List[State]) = {
    this.execute(instruction, state, verbose)
  }

  override def executeInstructionBlock(instruction: InstructionBlock, s: State, v: Boolean): (List[State], List[State]) =
    instruction.instructions.toList match {
      case Forward(place) :: tail => this.executeInternal(Forward(place), s, v)
      case InstructionBlock(is) :: tail => this.executeInternal(InstructionBlock(is ++ tail), s, v)
      case SuperFork(forkBlocks) :: tail =>
        this.executeInternal(SuperFork(forkBlocks.map(f => InstructionBlock(f :: tail))), s, v)
      case If (a, b, c) :: tail => this.execute(If(a, InstructionBlock(b :: tail), InstructionBlock(c :: tail)), s, v)
      case Fork(forkBlocks) :: tail => this.executeInternal(Fork(forkBlocks.map(f => InstructionBlock(f :: tail))), s, v)
      case head :: tail => super.executeInstructionBlock(InstructionBlock(head, InstructionBlock(tail)), s, v)
      case _ => super.executeInstructionBlock(instruction, s, v)
    }

  def +(pair : (String, Instruction)) : CodeAwareInstructionExecutor = {
    new CodeAwareInstructionExecutor(program = program + pair, solver = solver)
  }

  override def executeExoticInstruction(instruction: Instruction, s: State, v: Boolean): (List[State], List[State]) = {
    instruction match {
      case t : Translatable => this.execute(t.generateInstruction(), s, v)
      case Call(fun) => this.executeForward(Forward(fun), s, v)
      case Unfail(u) => val (ok, failed) = executeInternal(u, s, v)
        (ok ++ failed.map(x => x.copy(errorCause = None).forwardTo(s"Fail(${x.errorCause})")), Nil)
      case Let(string, u) => val (ok, failed) = executeInternal(u, s, v)
        (ok.map(x => {
          val symbols = x.memory.symbols.map( r => {
            string + "." + r._1 -> r._2
          }) ++ x.memory.rawObjects.map( r => {
            string + ".packet[" + r._1 + "]" -> r._2
          })
          s.copy(instructionHistory = x.instructionHistory,
            history = x.history,
            memory = x.memory.memTags.foldLeft(s.memory.copy(symbols = symbols ++ s.memory.symbols))((acc, r) => {
              acc.assignNewValue(s"$string.tags[${r._1}]", ConstantValue(r._2), LongType).get
            }))
        }), failed)
      case sf : SuperFork =>
        this.executeFork(Fork(sf.instructions.map(x => {
          InstructionBlock(
            Assign("PPid", :@("Pid")),
            Assign("Pid", ConstantValue(UUID.randomUUID().getMostSignificantBits)),
            x
          )
        })), s, v)
      //        handleSuperFork(sf, s, v)
      //        this.executeFork(Fork(sf.instructions), s, v)
      case _ => super.executeExoticInstruction(instruction, s, v)
//        instruction(s, v)
    }
  }

  def handleFork(instructions : List[Instruction], s : State, verbose : Boolean) : (List[State], List[State]) = {
    val peri = instructions.map(i => {
      this.executeInternal(Unfail(i), s, verbose)._1
    })

    def cartesianProduct[T](in: Seq[Seq[T]]): Seq[Seq[T]] = {
      @scala.annotation.tailrec
      def loop(acc: Seq[Seq[T]], rest: Seq[Seq[T]]): Seq[Seq[T]] = {
        rest match {
          case Nil =>
            acc
          case seq :: remainingSeqs =>
            // Equivalent of:
            // val next = seq.flatMap(i => acc.map(a => i+: a))
            val next = for {
              i <- seq
              a <- acc
            } yield i +: a
            loop(next, remainingSeqs)
        }
      }

      loop(Seq(Nil), in.reverse)
    }
    val cart = cartesianProduct(peri.filter(_.nonEmpty))
    (cart.map(y => {
      val uuid = UUID.randomUUID().toString
      y.zipWithIndex.foldLeft(s.copy(history = Nil, instructionHistory = Nil))((acc, x) => {
        val mappedSymbols = x._1.memory.symbols.map(r => s"$uuid.${x._2}.${r._1}" -> r._2)
        val mappedRaws  =x._1.memory.rawObjects.map(r => s"$uuid.${x._2}.packet[${r._1}]" -> r._2)
        acc.copy(memory = acc.memory.copy(
          symbols = acc.memory.symbols ++ mappedRaws ++ mappedSymbols
        ), history = x._1.history ++ acc.history,
          instructionHistory = x._1.instructionHistory ++ acc.instructionHistory)
      })
    }).filter(x => this.isSat(x.memory)).toList, Nil)
  }

  def handleSuperFork(fork: SuperFork, s: State, verbose: Boolean): (List[State], List[State]) = {
    val instructions = fork.instructions
    handleFork(instructions, s, verbose)
  }


  override def toString: String = {
    CodeRewriter(program)
  }
}

object CodeRewriter {
  def extractFinalSuccesfulCodes(root : Instruction, map : Map[String, Instruction]) : Set[String] = root match {
    case InstructionBlock(is) => is.flatMap(extractFinalSuccesfulCodes(_, map)).toSet
    case Fork(is) => is.flatMap(extractFinalSuccesfulCodes(_, map)).toSet
    case If(a, b, c) => extractFinalSuccesfulCodes(b, map) ++ extractFinalSuccesfulCodes(c, map)
    case Forward(place) => if (!map.contains(place)) Set[String](place) else Set[String]()
    case t: Translatable => extractFinalSuccesfulCodes(t.generateInstruction(), map)
    case _ => Set[String]()
  }

  def extractFinalSuccesfulCodes(map : Map[String, Instruction]) : Set[String] = {
    map.flatMap(x => extractFinalSuccesfulCodes(x._2, map)).toSet
  }


  def extractVariables(instruction: Instruction) : Map[String, Int] = instruction match {
    case AssignNamedSymbol(id, exp, _) => Map[String, Int](id -> 64) ++ (exp match {
      case v : Symbol => Map[String, Int](v.id -> 64)
      case _ => Map[String, Int]()
    })
    case AssignRaw(_, exp, _) => exp match {
      case v : Symbol => Map[String, Int](v.id -> 64)
      case _ => Map[String, Int]()
    }
    case AllocateSymbol(sb, size) => Map[String, Int](sb -> size)
    case InstructionBlock(instructions) => instructions.flatMap(extractVariables).toMap
    case Fork(forkBlocks) => forkBlocks.flatMap(extractVariables).toMap
    case If(a, b, c) => extractVariables(b) ++ extractVariables(c)
    case t : Translatable => extractVariables(t.generateInstruction())
    case _ => Map[String, Int]()
  }

  def apply(map: Map[String, Instruction]): String = {
    val include = "#include <stdint.h>\n#include " + s"""\"sym.h\"\n""" + "#include <klee.h>\n"
    val vars = "struct state {\n" + map.flatMap(u => extractVariables(u._2)).toMap.map(r => "uint64_t " + normalize(r._1) + ";").mkString("\n") +
      "\nstruct packet packet;" +
      "\n};\nstruct state *copy_state(struct state *in) {\n" +
      "struct state *news = memcpy(malloc(sizeof(struct state)), in, sizeof(struct state));\n" +
      "news->packet=copy_packet(news->packet);\nreturn news;\n}"
    val kvs = for {
      kv <- map
    }
      yield "void " + normalize(kv._1) + "(struct state *in) {\n" + apply(kv._2, 4) + "\n}"
    val succesful = extractFinalSuccesfulCodes(map)
    include + vars +"\n" +  (map.map(x => "void " + normalize(x._1) + "(struct state *in);").toSet ++
      succesful.map(x => "void " + normalize(x) + "(struct state *in);")
      ).mkString("\n") + "\n" +
      (kvs.filter(r=> !r.trim.isEmpty) ++
        succesful.map(x => "void " + normalize(x) + "(struct state *in) {\n" + s"""success(in, \"$x\");""" + "\n}")
        ).mkString("\n")
  }

  def generateFexp(floatingExpression: FloatingExpression) : String = floatingExpression match {
    case :+:(a, b) => generateFexp(a) + "+" + generateFexp(b)
    case :-:(a, b) => generateFexp(b) + "-" + generateFexp(b)
    case ConstantValue(a, _, _) => a + ""
    case ConstantStringValue(a) => a.hashCode + ""
    case Symbol(id) => "in->" + normalize(id)
    case Address(a) => s"get_raw(in->packet, ${solveIntable(a)})"
    case t : SymbolicValue => normalize(s"sym_${t.id}")
  }

  def generateConstraint(where : String, floatingConstraint: FloatingConstraint) : String = floatingConstraint match {
    case :==:(a) => s"$where == ${generateFexp(a)}"
    case :<=:(a) => s"$where <= ${generateFexp(a)}"
    case :>=:(a) => s"$where >= ${generateFexp(a)}"
    case :<:(a) => s"$where < ${generateFexp(a)}"
    case :>:(a) => s"$where > ${generateFexp(a)}"
    case :~:(c) => s"!(${generateConstraint(where, c)})"
    case :&:(a, b) => s"(${generateConstraint(where, a)} && ${generateConstraint(where, b)})"
    case :|:(a, b) => s"(${generateConstraint(where, a)} || ${generateConstraint(where, b)})"
  }

  def solveIntable(intable : Intable) : String = intable match {
    case te : TagExp => val plus = if (te.plusTags.nonEmpty)
      te.plusTags.map(solveIntable(_)).mkString("+")
    else
      "0"
      val minus = if (te.minusTags.nonEmpty)
        te.minusTags.map(solveIntable(_)).mkString("+")
      else
        "0"
      s"${te.rest}+" +
        s"$plus" +
        s"-($minus)"
    case t : Tag => s"""solve_tag(in->packet, \"${t.name}\")"""
    case t : IntImprovements => "" + t.value
  }

  def normalize(place : String) = place.
    replace(".", "_").
    replace("/", "_").
    replace("[", "_").
    replace("]", "_").replace("{", "_").replace("}", "_").replace("-", "_")


  def mapConstrain(i : Instruction, indentLevel : Int) = i match {
    case ConstrainRaw(a, floatingConstraint, _) => " " * indentLevel + "assert(" + generateConstraint(s"get_raw(in->packet, ${solveIntable(a)})", floatingConstraint) + ");"
    case ConstrainNamedSymbol(id, floatingConstraint, _) => " " * indentLevel + "assert(" + generateConstraint("in->" + normalize(id.toString), floatingConstraint) + ");"
  }

  def apply(i : Instruction, indentLevel : Int = 0): String = {
    val indent = " " * indentLevel
    i match {
      case If(a, b, c) => s"${indent}if (${apply(a, 0)}) {\n" + apply(b, indentLevel + 1) +
        s"\n$indent} else {\n" + apply(c, indentLevel + 1) + s"\n$indent}"
      case InstructionBlock(instructions) => s"$indent{\n" +
        instructions.map(u => u match {
          case cns : ConstrainNamedSymbol => mapConstrain(u, indentLevel + 1)
          case cr : ConstrainRaw => mapConstrain(u, indentLevel + 1)
          case _ => apply(u, indentLevel + 1)
        }).mkString("\n") +
        s"\n$indent}"
      case Fork(forkBlocks) => forkBlocks.map(x => {
        val rnd = normalize(s"nx_${UUID.randomUUID().toString}")
        indent + s"struct state *$rnd=copy_state(in);\n" +
          apply(x, indentLevel) + s"\n$indent" + s"in=$rnd;"
      }).mkString("\n")

      case AssignRaw(a, exp, t) => exp match {
        case s : SymbolicValue => indent + s"uint64_t " + normalize(s"sym_${s.id}") + ";\n" +
          indent + "klee_make_symbolic(&" + normalize(s"sym_${s.id}") +
          ", sizeof(" + normalize(s"sym_${s.id}") + "), " + normalize(s"sym_${s.id}") + ");\n" +
          indent + s"assign_raw(in->packet, ${solveIntable(a)}," +
          normalize(s"sym_${s.id}") + ");"
        case _ => indent + s"assign_raw(in->packet, ${solveIntable(a)}," + generateFexp(exp) + ");"
      }
      case AssignNamedSymbol(id, exp, t) => exp match {
        case s : SymbolicValue => indent + s"uint64_t " + normalize("sym_${s.id}") + ";\n" + indent +
          indent + "klee_make_symbolic(&" + normalize(s"sym_${s.id}") +
          ", sizeof(" + normalize(s"sym_${s.id}") + "), " + "\"" + normalize(s"sym_${s.id}") + ");\n" +
          s"in->${normalize(id.toString)}=" + normalize("sym_${s.id}") + ";"
        case _ => indent + "in->" + normalize(id.toString) + "=" + generateFexp(exp) + ";"
      }
      case ConstrainRaw(a, floatingConstraint, _) => indent + generateConstraint(s"get_raw(in->packet, ${solveIntable(a)})", floatingConstraint)
      case Forward(place) => indent + s"""${normalize(place)}(in);"""
      case ConstrainNamedSymbol(id, floatingConstraint, _) => indent + generateConstraint("in->" + normalize(id.toString), floatingConstraint)
      case CreateTag(name, value) => indent + s"""create_tag(in->packet, \"$name\", ${solveIntable(value)});"""
      case AllocateSymbol(symb, size) => indent + s"in->$symb = allocate(${size});"
      case AllocateRaw(a, size) => indent + s"allocate(in->packet, ${solveIntable(a)}, $size);"
      case DeallocateNamedSymbol(a) => ""
      //        indent + s"deallocate($a);"
      case DeallocateRaw(a, size) => indent + s"deallocate_raw(in->packet, ${solveIntable(a)}, $size);"
      case Fail(errMsg) => indent + s"""fail(in, \"$errMsg\");"""
      case NoOp => indent + s""
      case t : Translatable => apply(t.generateInstruction(), indentLevel)
//      case Save(location) => indent + s"""$location=*in;"""
//      case Load(location) => indent + s"""*in = $location;"""
      case Unfail(_) => indent
      case Let(_, _) => indent
      case SuperFork(forkBlocks) => forkBlocks.map(x => {
        val rnd = normalize(s"nx_${UUID.randomUUID().toString}")
        indent + s"struct state *$rnd=copy_state(in);\n" +
          apply(x, indentLevel) + s"\n$indent" + s"in=$rnd;"
      }).mkString("\n")
      case _ => throw new UnsupportedOperationException(s"Can't translate $i")
    }
  }
}


object RewriteLogic {

  def apply(instructions : List[Instruction]) : List[Instruction] = instructions match {
    case Nil => List[Instruction](NoOp)
    case InstructionBlock(instrs) :: tail => List[Instruction](apply(InstructionBlock(instrs ++ tail)))
    case ConstrainNamedSymbol(a, b, c) :: tail => List[Instruction](If (ConstrainNamedSymbol(a, b, c),
      apply(InstructionBlock(tail)),
      Fail("Assertion failed")))
    case ConstrainRaw(a, b, c) :: tail => List[Instruction](If (ConstrainRaw(a, b, c),
      apply(InstructionBlock(tail)),
      Fail("Assertion failed")))
    case Fork(forkBlocks) :: tail => List[Instruction](Fork(forkBlocks.map(i => apply(InstructionBlock(i :: tail)))))
    case (t : Translatable) :: tail => List[Instruction](apply(InstructionBlock(t.generateInstruction() :: tail)))
    case If (a, b, c) :: tail => List[Instruction](apply(If (a, InstructionBlock(b :: tail), InstructionBlock(c :: tail))))
    //        InstructionBlock(If (a, apply(b), apply(c)), apply(InstructionBlock(tail)))
    case SuperFork(forkBlocks) :: tail => List[Instruction](SuperFork(forkBlocks.map(i => apply(InstructionBlock(i :: tail)))))
    case Forward(place) :: tail => List[Instruction](Forward(place))
    case head :: tail => head :: apply(tail)
    case _ => throw new IllegalArgumentException(s"Can't handle this instruction $instructions")
  }

  def apply(instruction : Instruction) : Instruction = instruction match {
    case InstructionBlock(instructions) => InstructionBlock(apply(instructions.toList))
    case If (a, b, c) => If(a, apply(b), apply(c))
    case Fork(forkBlocks) => Fork(forkBlocks.map(apply))
    case SuperFork(forkBlocks) => SuperFork(forkBlocks.map(apply))
    case (t : Translatable) => apply(t.generateInstruction())
    case Let(place, instruction) => Let(place, apply(instruction))
    case Unfail(instruction) => Unfail(apply(instruction))
    case _ => instruction
  }

  def apply(program : Map[String, Instruction]) : Map[String, Instruction] = {
    //    program.map(kv => kv._1 -> apply(kv._2))
    program
  }
}

object CodeAwareInstructionExecutor {
  def apply(program: Map[String, Instruction],
            solver: Solver): CodeAwareInstructionExecutor = new CodeAwareInstructionExecutor(RewriteLogic(program), solver)
  def apply(program: Map[String, Instruction], links : Map[String, String],
            solver: Solver): CodeAwareInstructionExecutor =
    new CodeAwareInstructionExecutor(RewriteLogic(program ++ links.map( x => x._1 -> Forward(x._2))), solver)
}
