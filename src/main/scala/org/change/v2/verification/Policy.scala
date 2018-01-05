package org.change.v2.verification

import org.change.v2.analysis.constraint.NOT
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions.{:~:, AllocateRaw, AllocateSymbol, AssignRaw, ConstrainNamedSymbol, ConstrainRaw, CreateTag, DeallocateNamedSymbol, DeallocateRaw, DestroyTag, Fork, Forward, If, InstructionBlock, NoOp, Fail => SEFLFail}
import org.change.v2.verification.Formula._
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

  //def mode = CustomMode :: Nil
  def mode = Nil

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
  def state(l:LocationId, t: Topology, links:Map[LocationId,LocationId]) =
    new MapState(l,t,links,State.allSymbolic)



  // very simplistic implementation of complement
  def complement (i : Instruction) : Instruction = {
    i match {
      case InstructionBlock(l) => Fork(l.map(complement))
      case ConstrainNamedSymbol(v,dc,c) => ConstrainNamedSymbol(v,:~:(dc),c) // negation of constrain
      case ConstrainRaw(a,dc,c) => ConstrainRaw(a,:~:(dc),c) // as above
      case _ => i // the complement leaves an instruction unchanged, for all other cases (for now)
    }
  }

  def isSatisfied (state: PolicyState, i : Instruction) : Boolean = {

    //
    //println("Is satisfied "+i+" on state ")
    //println(state.state.instructionHistory)
    var comp = complement(i)


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
  def show (i : Instruction, indent: Integer) : String = {
      def aux (sep : String, l : Iterable[Instruction], indent : Integer) = "{ "+newln+(l.map((x)=>show(x,indent+1)).reduce((x,y)=> x+("  "*indent)+sep+newln+y)) + ("  "*indent) + "} "+newln

      ("  "*indent) + {i match {
      case InstructionBlock(Nil) => "{;}"
      case Fork(Nil) => "{||}"
      case InstructionBlock(l) => aux(" ; ", l, indent)
      case Fork(l) => aux(" || ", l, indent)
      case ConstrainRaw(a,b,c) => a.toString+b.toString
      case ConstrainNamedSymbol(a,b,c) => a.toString+b.toString
      case AssignRaw(a,b,c) => a.toString+"="+b.toString
      case If(test,th,el) => "if ("+test.toString+") then {"+show(th,indent+1)+"} else {"+show(el,indent+1)+"}"
      case Forward(p)=> "Forward("+p.toString+")"
      case SEFLFail(_) => "Fail"
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
          case InstructionBlock(instructions) => transform(s, instructions.foldRight(thenWhat)((x, acc) => {
            If(x, acc, elseWhat)
          }))
          case Fork(instructions) => transform(s, instructions.foldRight(elseWhat)((x, acc) => {
            If(x, thenWhat, acc)
          }))
        }
      case _ => null
    }
  }


  // if a formula is declared pending, all its subformulae are declared pending, which prompts re-evaluation
  def make(f:Formula, s:Status) : Formula = {f.status = s; f}
  def clone(f:Formula) : Formula = f
  def clear(f:Formula) : Formula = {f.clear_valuation(); f}
  def reeval(f:Formula) : Formula = make(clear(f),Pending)



def check (f : Formula, p: Instruction, s : PolicyState, logger : PolicyLogger) : (Formula,PolicyLogger) = {

  verbose_print("Verifying " + f + "\n on program \n" + showp(p) + " port "+logger.codePort,CustomMode);

  if (s == UnsatisfState || s == FailedState)
    verbose_print("Failed branch (at port "+logger.currentPort+")"+" failed instruction "+logger.lastInstruction,LocMode);

   //matching after formula and program
   (s,f,p) match {
      // failed state
     case (UnsatisfState,Forall(Globally(_)),_) | (FailedState,Forall(Globally(_)),_) |
          (UnsatisfState,Exists(Globally(_)),_) | (FailedState,Exists(Globally(_)),_) => verbose_print ("FG(f) is true (failed state)\n",OverallMode); (make(f,Satisfied),logger)
     case (UnsatisfState,_,_) | (FailedState,_,_)=> verbose_print ("f is false (failed state)\n",OverallMode);
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
     case (_,_,InstructionBlock(Nil)) => logger.pathEnded(); (clone(f),logger) //clone returns a new formula with "s" as the witness state

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
                   check_sequence(f, s, Forward(loc), (f,ss,l) => {check(f,s.instructionAt(loc),ss,l)},logger)

                 case pair@(MapState(_,_,_,_), InstructionBlock(Forward(loc) :: _)) =>
                   check_sequence(f, s, Forward(loc), (f,ss,l) => {check(f,s.instructionAt(loc),ss,l)},logger)
                 case _ => { /*verbose_print("Skipping \n", MCMode);*/ (f,logger)}
               }


     case (_,_,InstructionBlock(pr :: rest)) => //take an instruction
     check_sequence(f,s,pr,(f,s,l) => {check(f,InstructionBlock(rest),s,l)},logger)


     case (_,_,Fork(Fork(xs)::rest)) => check(f,Fork(xs ++ rest),s,logger)
     case (_,_,Fork(lst)) => {val (fp,log) = lst.foldLeft((f,{logger.initFork; logger}))(
       (pair : (Formula,PolicyLogger),branch:Instruction) => {
         val (f, log) = pair
         (f,f.status) match {
           case (Exists(_),Satisfied) => verbose_print ("f is true (on a Fork branch)\n",OverallMode); (make (f, Satisfied),log)
           case (Forall(_),Pending) | (Forall(_),Falsified) => verbose_print ("f is false (on a Fork branch)\n",OverallMode); (make(f,Falsified),log)
           // if the truth-value cannot be established, then "s" (the state before executing the branch) is used to continue verification on another branch
           case (Exists(_),_) | (Forall(_),Satisfied) =>  //the formula must be made pending for the next branch verification
             {
               val (fp,lprime) = check(reeval(f),branch,s,log) //note that s is the same state built "at the Fork" and the status of the formula has been reset
               lprime.addPath
               // reporting
               //verbose_print("Explored branch "+branch+" at "+lprime.currentPort,LocMode);
               //lprime.getInstructionTrace

               if (fp.status == Pending) (make(fp,Falsified),lprime) //if an explored branch is pending, falsify it
               else (fp,lprime)
             }

           case (_,_) => throw new Exception ("Cannot evaluate non-temporal formula on a Fork ")
         }
       }
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
    //println("In check-sequence for "+p)
    //Thread.sleep(1000)

    val (sp,lp) = s.execute(p,logger)  // execute program pr in state s; if sp is unsatisfiable, this is treated by the subsequent "check" call

    // we simply continue execution
    if (!changesState(p)) { block(f,sp,lp); }

    else {
      //println("In evaluation of check-sequence for " + p)
      // otherwise, we check the current state, and possibly continue execution
      var (fval, lprime) = check_in_state(f, s, lp) //check(f,p,sp,lp) // verify the policy in this new state (
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
  def verify (f: Formula, start:LocationId ,topology: Topology, links:Map[LocationId,LocationId]) : (Boolean,PolicyLogger) = {

    //println("Initial state:"+state(start,topology,links).state)
    var (fp,logger) = check(f,topology(start),state(start,topology,links), new PolicyLogger(start))

    //logger.getInstructionTrace
    //println(logger.instructionTrace)

    (fp.status match {
      case Falsified => verbose_print("Formula is false",OverallMode); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false
    },logger)
  }

  // "top-level" verification procedure for plain SEFL code
  def verify (f : Formula, model : Instruction) : Boolean = {

    val (fp,l) = check(f,model,state, new PolicyLogger("start"));
    fp.status match {
      case Falsified => verbose_print("Formula is false",OverallMode); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false

    }
  }
}

