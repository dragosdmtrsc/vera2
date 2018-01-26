package org.change.v2.verification

import org.change.parser.p4.{ControlFlowInterpreter, anonymizeAndForward, anonymize}
import org.change.v2.analysis.constraint.NOT
import org.change.v2.analysis.executor.solvers.{Z3BVSolver, Z3Solver}
import org.change.v2.analysis.executor.{AbstractInstructionExecutor, CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.expression.concrete.{ConstantStringValue, ConstantValue}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:, AllocateRaw, AllocateSymbol, Assign, AssignRaw, Call, Constrain, ConstrainNamedSymbol, ConstrainRaw, CreateTag, DeallocateNamedSymbol, DeallocateRaw, DestroyTag, ExistsNamedSymbol, ExistsRaw, Fork, Forward, If, InstructionBlock, NoOp, NotExistsNamedSymbol, NotExistsRaw, Fail => SEFLFail}
import org.change.v2.p4.model.{ISwitchInstance, SwitchInstance}
import org.change.v2.verification.Formula._
import org.change.v2.util._
/**
 * Created by matei on 12/01/17.
 * TODO: optimise such that subformulae are not checked repeatedly. Suspended because it might make debugging harder
 */


object Policy {

  type Topology = Map[LocationId,Instruction]

  type Continuation = (Formula,PolicyState,PolicyLogger) => (Formula,PolicyLogger)

  //def mode = ModelCheckMode::Nil
  //def mode = MCMode::OverallMode::LocMode::Nil
  //def mode = OverallMode::LocMode::Nil
  //def mode = Nil

  def mode = CustomMode :: Nil
  //def mode = Nil

  def EF(f:Formula) = Exists(Future(f))
  def AF(f:Formula) = Forall(Future(f))
  def EG(f:Formula) = Exists(Globally(f))
  def AG(f:Formula) = Forall(Globally(f))

  def EF(p:Instruction) = Exists(Future(Atomic(p)))
  def AF(p:Instruction) = Forall(Future(Atomic(p)))
  def EG(p:Instruction) = Exists(Globally(Atomic(p)))
  def AG(p:Instruction) = Forall(Globally(Atomic(p)))

  def And(p:Instruction, pp:Instruction) : Formula = Formula.And(Atomic(p),Atomic(pp))
  def And(f:Formula, fp:Formula) : Formula = Formula.And(f,fp)

  def Or(p:Instruction, pp:Instruction) : Formula = Formula.Or(Atomic(p),Atomic(pp))
  def Or(f:Formula, fp:Formula) : Formula = Formula.Or(f,fp)

  def state : NoMapState = new NoMapState(State.allSymbolic)

  /*
  def natstate(l:LocationId, t: Topology, links:Map[LocationId,LocationId],exe:AbstractInstructionExecutor, ) = {

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
    )


    var (suc :: _, _) = exe.execute(ib,State.clean,true);

  }
  */
  def state(code: Map[LocationId,Instruction], links:Map[LocationId,LocationId],exe:AbstractInstructionExecutor) =
  {
    new MapState(code, links, State.allSymbolic, exe)
  }
  def state[T <: ISwitchInstance](res : ControlFlowInterpreter[T], exe:AbstractInstructionExecutor) = {

    val ib = InstructionBlock(
      res.allParserStatesInstruction()
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
    )

    var (suc :: _, _) = exe.execute(ib,State.clean,true);

    new MapState(res.instructions(),res.links(),suc,exe);
  }


  // very simplistic implementation of complement
  def complement (i : Instruction) : Instruction = {
    i match {
      case InstructionBlock(l) => Fork(l.map(complement))
      case Fork(l) => InstructionBlock(l.map(complement))
      case ConstrainNamedSymbol(v,dc,c) => ConstrainNamedSymbol(v,:~:(dc),c) // negation of constrain
      case ConstrainRaw(a,dc,c) => ConstrainRaw(a,:~:(dc),c) // as above
      case ExistsRaw(x) => NotExistsRaw(x)
      case ExistsNamedSymbol(x) => NotExistsNamedSymbol(x)
      case _ => i // the complement leaves an instruction unchanged, for all other cases (for now)
    }
  }

  def isSatisfied (state: PolicyState, i : Instruction) : Boolean = {


    //println("Is satisfied "+i+" on state ?")
    println(state.state.memory)
    var comp = complement(i)
    println("Complement "+comp)

    state.execute(comp) match {
      case FailedState => println("IS true"); true
      case _ => println("IS false"); false
    }

  }

  def isComposite (i: Instruction) : Boolean =
    i match {
      case InstructionBlock(_) => true
      case Fork(_) => true
      case _ => false
    }

  def showp (i:Instruction) = i.toString

  def show (i : Instruction) : String = show(i,0)

  /*
   case InstructionBlock(l) => "{"+(l.map(show).reduce((x,y)=> x+" ; "+y))+"}"
   case Fork(l) => "{"+(l.map(show).reduce((x,y)=> x+" || "+y))+"}"
   case ConstrainRaw(a,b,c) => a.toString+b.toString
   case AssignRaw(a,b,c) => a.toString+"="+b.toString
   case Forward(p)=> "Forward("+p.toString+")"
   */

  def newln = "\\n"
  def tab = "\\t"
  def show (i : Instruction, indent: Integer) : String = {
      def aux (sep : String, l : Iterable[Instruction], indent : Integer) = "{ "+newln+(l.map((x)=>show(x,indent+1)).reduce((x,y)=> x+(tab*indent)+sep+newln+y)) + (tab*indent) + "} "+newln

      (tab*indent) + {i match {
      case InstructionBlock(Nil) => "{;}"
      case Fork(Nil) => "{||}"
      case InstructionBlock(l) => aux(" ; ", l, indent)
      case Fork(l) => aux(" || ", l, indent)
      case ConstrainRaw(a,b,c) => a.toString+b.toString
      case ConstrainNamedSymbol(a,b,c) => a.toString+b.toString
      case AssignRaw(a,b,c) => a.toString+"="+b.toString
      //case If(test,th,el) => "if ("+test.toString+") then {"+show(th,indent+1)+"} else {"+show(el,indent+1)+"}"
      case If(test,th,el) => "if ("+show(test,0)+")"+newln+(tab*indent)+" then {"+show(th,indent+1)+"} "+newln+(tab*indent)+" else {"+show(el,indent+1)+"}"
      case Forward(p)=> "Forward("+p.toString+")"
      case SEFLFail(msg) => "Fail("+msg+")"
      case AllocateSymbol(s, sz) => "allocate("+s+")"
      case AllocateRaw(s,sz) => "allocate_raw("+s+")"
      case DeallocateRaw(s,_) => "deallocate("+s+")"
      case DeallocateNamedSymbol(s) => "deallocate_n("+s+")"
      case CreateTag(s,v) => "createTag("+s+")"
      case DestroyTag(s) => "destroyTag("+s+")"
      //case AssignNamedSymbol(a,b,c) => a.toString+"="+b.toString

      case NoOp => "NoOp"
      case x => x.toString//"???Unknown op???"
      }} + newln
  }

  //def changesState (i: Instruction) : Boolean = true
  def changesState (i: Instruction) : Boolean =
    i match {
      case Forward(_) => true
      case _ => false
    }

  trait PrintMode {};
  case object MCMode extends PrintMode;
  case object LocMode extends PrintMode;
  case object OverallMode extends PrintMode;
  case object CustomMode extends PrintMode;

  def verbose_print(s : String, t : PrintMode ) = if (mode.contains(t)) println(s)

  def atomic_check (p : Instruction, s : PolicyState, logger:PolicyLogger) : (Formula,PolicyLogger) =
    {//println("Checking "+p);//println(s.state.instructionHistory);

  (if (isSatisfied(s, p))
    {verbose_print (show(p)+" is true \n",MCMode);  make(Atomic(p),Satisfied)}
    else {verbose_print (show(p)+" is false \n",MCMode);  make(Atomic(p),Falsified)}, logger)}

  /* VG ( VG f )  if VG is pending, it is not false in the current state, hence the search continues.
     VG f - if f is pending, then if it is true in a future state, it will be true in the current (pending) state
   */

  // this procedure verifies boolean formulae (and is independent on the program context verification, unlike temporal formulae
  // the check function represents the "program context" in which it appears
  def boolean_check (f:Formula, s:PolicyState, check : (Formula, PolicyState, PolicyLogger) => (Formula, PolicyLogger), logger: PolicyLogger) : (Formula,PolicyLogger) = {
    verbose_print("boolean check",MCMode)
    if (f.status != Pending) {verbose_print("already verified",MCMode);return (f,logger)} //this formula was already verified


    f match {
      case fp@And(f1, f2) => val (fpp,l) = check(f1, s,logger); fpp.status match {
        case Falsified => (make(fp,Falsified),l)
        // pending or satisfied
        case status => val (fpp,l) = check(f2, s,logger); fpp.status match {
          case Falsified => (make(fp,Falsified),l)
          case Satisfied => (make(fp,status),l) //whatever f1 returned is the new status
          case Pending => (make(fp,Pending),l) //if f2 is pending and f1 is not false, the AND is pending
        }
      }
      case fp@Or(f1, f2) => val (fpp,l) = check(f1, s,logger); fpp.status match {
        case Satisfied => (make(fp,Satisfied),l)
        case status => val (fpp,l) = check(f2,s,logger); fpp.status match {
          case Satisfied => (make(fp,Satisfied),l)
          case Falsified => (make(fp,status), l)
          case Pending => (make(fp,Pending), l)
        }
      }
      case fp@Not(f) => val (fpp,l) = check(f, s, logger); fpp.status match {
        case Falsified => (make(fp,Satisfied), l)
        case Satisfied => (make(fp,Falsified), l)
        case Pending => (make(fp,Pending), l)
      }

      case fp@Atomic(i) => atomic_check(i,s,logger)

      case Formula.Fail => (make(f,Falsified),logger)
      case Formula.Success => (make(f,Satisfied),logger)
    }
  }

  def transform (s: PolicyState, p : Instruction): Instruction = {
    p match {
      case If(testInstr: Instruction, thenWhat: Instruction, elseWhat: Instruction) =>
        testInstr match {
          // This is quite inappropriate
          case i @ ConstrainNamedSymbol(what, withWhat, _) =>
          {
            //verbose_print("What "+s.state.memory.verboseToString,OverallMode);

            withWhat instantiate s.state match {
              case Left(c) if s.state.memory.symbolIsAssigned(what) =>
                Fork(InstructionBlock(ConstrainNamedSymbol(what, withWhat), thenWhat),
                  InstructionBlock(ConstrainNamedSymbol(what, :~:(withWhat), Some(NOT(c))), elseWhat))
              case _ => elseWhat
            }
          }
          case rawi @ ConstrainRaw(what, withWhat, _) => what(s.state) match {
            case Some(i) => withWhat instantiate s.state match {
              case Left(c) if s.state.memory.canRead(i) =>
                Fork(InstructionBlock(ConstrainRaw(what, withWhat), thenWhat),
                  InstructionBlock(ConstrainRaw(what, :~:(withWhat), Some(NOT(c))), elseWhat))

              case _ => elseWhat
            }
            case None => elseWhat
          }
          case InstructionBlock(instructions) if instructions.nonEmpty => transform(s, instructions.foldRight(thenWhat)((x, acc) => {
            If(x, acc, elseWhat)
          }))
          case InstructionBlock(_) => thenWhat
          case Fork(instructions) if instructions.nonEmpty => transform(s, instructions.foldRight(elseWhat)((x, acc) => {
            If(x, thenWhat, acc)
          }))
          case Fork(_) => elseWhat
        }
      case _ => ???
    }
  }


  // if a formula is declared pending, all its subformulae are declared pending, which prompts re-evaluation
  def make(f:Formula, s:Status) : Formula = {f.status = s; f}
  def clone(f:Formula) : Formula = f
  def clear(f:Formula) : Formula = {f.clear_valuation(); f}
  def reeval(f:Formula) : Formula = make(clear(f),Pending)



def check (f : Formula, p: Instruction, s : PolicyState, logger : PolicyLogger) : (Formula,PolicyLogger) = {

  //verbose_print("Verifying " + f + "\n on program \n" + showp(p) + " port "+logger.currentPort,CustomMode);
  println(">>>>>>>>Verifying formula on program \n",p,"\n")

  if (s == UnsatisfState || s == FailedState)
    verbose_print("Failed branch (at port "+logger.currentPort+")"+" failed instruction ",LocMode);

   //matching after formula and program
   (s,f,p) match {
      // failed state
     case (UnsatisfState,Forall(Globally(_)),_) | (FailedState,Forall(Globally(_)),_) |
          (UnsatisfState,Exists(Globally(_)),_) | (FailedState,Exists(Globally(_)),_) => verbose_print ("XG(f) is true (failed state)\n",CustomMode); (make(f,Satisfied),logger)
     case (UnsatisfState,_,_) | (FailedState,_,_)=> verbose_print ("f is false (failed state)\n",CustomMode);
       //verbose_print("Program:"+last_instruction+"\n",OverallMode);
       (make(f,Falsified),logger)

     // if the formula is boolean, we evaluate it, using 'check'
     case (_,And(_,_),_) | (_,Or(_,_),_) | (_,Not(_),_)  => boolean_check(f,s,(f,s,l) => check(f,p,s,l),logger)

     //flattening nested instruction blocks
     case (_,_,InstructionBlock(InstructionBlock(l) :: rest))
        => check(f, InstructionBlock(l ++ rest), s, logger) // flatten nested instruction blocks

     // instructionBlock basis cases
     case (_,Forall(Globally(_)),InstructionBlock(Nil)) |
          (_,Exists(Globally(_)),InstructionBlock(Nil)) |
          (_,Success,            InstructionBlock(Nil)) => verbose_print ("f is true (InstructionBlock ended)\n",OverallMode); (make(f,Satisfied),logger)

     case (_,Formula.Fail,InstructionBlock(Nil)) => verbose_print("f is false (Implicit formula)",OverallMode); (make(f,Falsified),logger)


     // this case should be reinspected (for F operator)
     case (_,_,InstructionBlock(Nil)) => logger.InstructionBlockEnded(); (clone(f),logger) //clone returns a new formula with "s" as the witness state

     case (_,_,InstructionBlock(Fork(l) :: Nil)) => check(f,Fork(l),s,logger)
     case (_,_,InstructionBlock(Fork(l) :: rest)) // distribute Fork instructions
         => check(f,
           Fork(l.map(p => InstructionBlock(p :: rest)))
           ,s, logger)
     case (_,_,InstructionBlock(If(test,then,els) :: rest)) => check(f,InstructionBlock(transform(s,If(test,then,els))::rest),s,logger)

       // the semantics of Forward, or Forward in an instruction block is essentially the same
       // (the rest of the instruction block, after Forward, is ignored)
     case (_,_,InstructionBlock(Forward(_) :: _)) |
          (_,_,Forward(_)) => (s,p) match {
                 // the witness state assigned to f is "sp" (containing the new location)
                 case (MapState(_,_,_,_), Forward(loc)) =>
                   //println("Will check "+s.instructionAt(loc))
                   check_sequence(f, s, Forward(loc), (f,ss,l) => {check(f,s.instructionAt(loc),ss,l)},logger)

                 case pair@(MapState(_,_,_,_), InstructionBlock(Forward(loc) :: _)) =>
                   //println("IB Will check "+s.instructionAt(loc))
                   check_sequence(f, s, Forward(loc), (f,ss,l) => {check(f,s.instructionAt(loc),ss,l)},logger)
                 case _ => { /*verbose_print("Skipping \n", MCMode);*/ (f,logger)}
               }


     case (_,_,InstructionBlock(pr :: rest)) => //take an instruction
       //println("InstructionBlock !!");
     check_sequence(f,s,pr,(f,s,l) => {check(f,InstructionBlock(rest),s,l)},logger)

       case(_,_,InstructionBlock(x)) =>
        check(f,InstructionBlock(x.toList),s,logger)

     case (_,_,Fork(Fork(xs)::rest)) => check(f,Fork(xs ++ rest),s,logger)
     case (_,_,Fork(lst)) => {//println("FORK!!");
       val (fp,log,status) = lst.foldLeft((f,{logger.initFork; logger},
        //set the initial status in the accumulator
         f match {
          case(Exists(_)) => Pending;
            case(Forall(_)) => Satisfied;
        }
       ))(
       (triple : (Formula,PolicyLogger,Status),branch:Instruction) => {

         //println("Branch "+branch)
         val (f, log, status : Status) = triple
         (f,status) match {
           case (Exists(_),Satisfied) => verbose_print ("f is true (on a Fork branch)\n",CustomMode); (make (f, Satisfied),log,Satisfied)
           case (Forall(_),Pending) | (Forall(_),Falsified) => verbose_print ("f is false (on a Fork branch)\n",CustomMode); (make(f,Falsified),log, Falsified)
           // if the truth-value cannot be established, then "s" (the state before executing the branch) is used to continue verification on another branch
           case (Exists(_),_) | (Forall(_),Satisfied) =>  //the formula must be made pending for the next branch verification
             {
               val (fp,lprime) = check(reeval(f),branch,s,log) //note that s is the same state built "at the Fork" and the status of the formula has been reset
               lprime.addPath
               // reporting
               verbose_print("Explored branch "+branch+" at "+lprime.currentPort+" status "+fp.status,CustomMode);
               //lprime.getInstructionTrace

               //if (fp.status == Pending) (make(fp,Falsified),lprime,Falsified) //if an explored branch is pending, falsify it
              // else (fp,lprime,Pending)

               (fp,lprime,fp.status) //: (Formula,PolicyLogger,Status)

             }

           case (_,_) => throw new Exception ("Cannot evaluate non-temporal formula on a Fork ")
         }

       } //: (Formula,PolicyLogger,Status)
     ); log.endFork; (fp,log) }


       // IF implementation
     case (_,_,If(testInstr: Instruction, thenWhat: Instruction, elseWhat:Instruction)) =>
       check(f,transform(s,p),s,logger)

       // *  *  *  *  *  *  *  *
       // non-branching programs
       // *  *  *  *  *  *  *  *

       // if the current instruction does not change state, the status of f is unchanged
     case _ => if (changesState(p)) check_in_state(f,s,logger) else (f,logger)

   }
 }

  def check_sequence (f : Formula, s : PolicyState, p : Instruction, block : Continuation, logger:PolicyLogger) : (Formula,PolicyLogger) = {
    println("In check-sequence for "+p)
    //Thread.sleep(1000)

    val (sp,lp) = s.execute(p,logger)  // execute program pr in state s; if sp is unsatisfiable, this is treated by the subsequent "check" call

    // we simply continue execution
    if (!changesState(p)) { block(f,sp,lp); }

    else {
      //println("We evaluate policy in state "+sp.state.memory)

      //println("In evaluation of check-sequence for " + p)
      // otherwise, we check the current state, and possibly continue execution
      var (fval, lprime) = check_in_state(f, sp, lp) //check(f,p,sp,lp) // verify the policy in this new state (
      // if the current instruction does not change state, fval = f

      //println("Current status is:"+fval.status)
      //println("Formula:"+fval)


      (fval.status, f) match {
        // s, p;rest |= XG f  iff  s,p |= XG f   and p(s),rest |= XG f
        // the "false" case
        case (Falsified, Forall(Globally(_))) | (Falsified, Exists(Globally(_))) =>
          // the witness state is the current state
          verbose_print("Linear: f is false \n", OverallMode); (make(f, Falsified), lprime)
        // otherwise continue verification

        // the "true" case
        case (Satisfied, Forall(Future(_))) | (Satisfied, Exists(Future(_))) =>
          println("Linear: f is true"); (make(f, Satisfied), lprime)

        // the valuation of fval is reset, but the witness state is preserved and passed on
        case (_, Forall(Globally(_))) | (_, Exists(Globally(_))) | (_, Forall(Future(_))) | (_, Exists(Future(_))) => {/*println("Reeval");*/ block(reeval(fval), sp, lprime)}

        case (_, Forall(_)) | (_, Exists(_)) => throw new Exception("Not a CTL formula")

        // the rest
        case (_, _) => (fval, lprime) // if the formula is not temporal, it has been checked;

      }
    }

  }


  def check_in_state(f : Formula, s: PolicyState, logger:PolicyLogger) : (Formula,PolicyLogger) = {
    f match {
      // temporal formula on non-branching program
      case Exists(Globally(_)) | Forall(Globally(_)) | Exists(Future(_)) | Forall(Future(_))
      => val (fp,l) = check_in_state(f.inner.inner, s,logger)
        (make(f, fp.status),l)
      case And(_, _) | Or(_, _) | Not(_) => boolean_check(f, s, check_in_state,logger)
      case Atomic(fp) => atomic_check(fp, s,logger)
      case Success => (make(f, Satisfied), logger)
      case Formula.Fail => (make (f, Falsified), logger)
      case _ => throw new Exception("non-branching program on non-temporal formula: " + f)
    }
  }
  // "top-level" verification procedure for topologies
  /*
  def verify (f: Formula, start:LocationId ,topology: Topology, links:Map[LocationId,LocationId]) : (Boolean,PolicyLogger) = {

    //
    var exe:AbstractInstructionExecutor = new OVSExecutor(new Z3BVSolver);

    //println("Initial state:"+state(start,topology,links).state)
    var log = new PolicyLogger(start)
    var (fp,logger) = check(f,topology(start),state(topology,links,exe), log)

    //logger.getInstructionTrace
    //println(logger.instructionTrace)

    (fp.status match {
      case Falsified => verbose_print("Formula is false",OverallMode); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false
    },logger)
  }*/

  def defaultExecutor = new OVSExecutor(new Z3BVSolver);

  def verify (f: Formula, start:LocationId, code: Map[LocationId,Instruction], links: Map[LocationId,LocationId]) : PolicyLogger = {

    var exe:AbstractInstructionExecutor = defaultExecutor

    val ib = InstructionBlock(
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue(start))
    )
    //build all possible start states
    var (l, _) = exe.execute(ib,State.clean,true);
    var s = l(0)

    var (fp,logger) = check(f,code(start),new MapState(code,links,s,exe), new PolicyLogger(start))
    println("Formula is ",fp.status)
    logger
  }


  private def natAndReverse[T<:ISwitchInstance](res: ControlFlowInterpreter[T]) = {
    import org.change.v2.analysis.memory.TagExp.IntImprovements
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val newinstrs = codeAwareInstructionExecutor.program
    val reverseBlock = createReverse("reverse")
    val fullInstrs = (reverseBlock ++ newinstrs) + ("router.output.2" -> anonymizeAndForward("reverse.input.1")) +
      ("reverse.output.1" -> anonymizeAndForward("router.input.2"))
    fullInstrs
  }
  def createReverse(withName : String): Map[String, Instruction] = {
    val dir = "inputs/reverse-p4/"
    val p4 = s"$dir/reverse.p4"
    val dataplane = s"$dir/commands-rev.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), withName)
    CodeAwareInstructionExecutor.flattenProgram(res.instructions(), res.links())
  }
  def verifyP4AndReverse[T<:ISwitchInstance] (f: Formula, start: LocationId, ib : Instruction,
                                             res : ControlFlowInterpreter[T]) : List[PolicyLogger] = {
    import org.change.v2.analysis.memory.TagExp.IntImprovements
    val newib = InstructionBlock(
      CreateTag("START", 0),
      Call("router.generator.parse_ethernet.parse_ipv4.parse_tcp"),
      Call("router.parser.parse_ethernet.parse_ipv4.parse_tcp"),
      // aici se adauga constrangerile pe campurile parsate
      // e.g. Constrain("ipv4....
      // sau asignarile Assign("x", "ipv4.dstAddr")
      // NB: ca sa fie disponibile in continuare, campurile asignate, trebuiesc trecute in multimea de campuri
      // din instructiunea anonymize de mai jos
      Instruction(anonymize(_, Set()))
    )
    verifyP4(f, start, ib, natAndReverse(res), Map.empty)
  }

  def verifyP4(f: Formula, start: LocationId, ib : Instruction,
                                    instructions : Map[String, Instruction],
                                    lks : Map[String, String] = Map.empty) : List[PolicyLogger] = {
    var exe:AbstractInstructionExecutor = CodeAwareInstructionExecutor(instructions, lks, new Z3BVSolver)
    /*
    val ib = InstructionBlock(
      res.allParserStatesInline(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue(start))
    )
    */
    //println("initial "+ib)
    //build all possible start states
    var (l, fa) = exe.execute(ib,State.clean,true);
    //println(">>>>",l(0).instructionHistory)
    //println("OK:",l)
    //println("Fail:",fa)

    var links = lks // - "router.buffer.out"


    //l = l.head :: Nil
    //l = l.tail

    // return a list of loggers

    l.map ((s:State) => {
      var (x,y) = check(f,instructions(start),new MapState(instructions, links,s,exe),new PolicyLogger(start))
      println("\n =================\n Formula is ",x.status)
      y
    })
  }


  def verifyP4[T<:ISwitchInstance] (f: Formula, start: LocationId, ib : Instruction, res : ControlFlowInterpreter[T]) : List[PolicyLogger] = {
    verifyP4(f, start, ib, res.instructions(), res.links())
  }

  /*
  // "top-level" verification procedure for plain SEFL code
  def verify (f : Formula, model : Instruction) : Boolean = {

    val (fp,l) = check(f,model,state, new PolicyLogger("start"));
    fp.status match {
      case Falsified => verbose_print("Formula is false",OverallMode); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false

    }
  }
  */
}

