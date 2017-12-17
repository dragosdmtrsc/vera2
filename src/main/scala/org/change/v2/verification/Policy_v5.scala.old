package org.change.v2.verification

import org.change.v2.analysis.constraint.NOT
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions.{Fail => SEFLFail, _}
import org.change.v2.verification.Formula._


/**
 * Created by matei on 12/01/17.
 * TODO: optimise such that subformulae are not checked repeatedly. Suspended because it might make debugging harder
 */


object Policy_v5 {

  type Topology = Map[LocationId,Instruction]

  var last_instruction : Instruction = null

  var lastState : PolicyState = null

  //def mode = ModelCheckMode::Nil
  def mode = MCMode::OverallMode::LocMode::Nil
  //def mode = OverallMode::LocMode::Nil
  //def mode = LocMode::Nil

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

  def state : NoMapState = new NoMapState(State.bigBang)
  def state(l:LocationId, t: Topology, links:Map[LocationId,LocationId]) =
    new MapState(l,t,links,State.bigBang,l::Nil)



  // very simplistic implementation of complement
  def complement (i : Instruction) : Instruction = {
    i match {
      case InstructionBlock(l) => Fork(l.map(complement))
      case ConstrainNamedSymbol(v,dc,c) => ConstrainNamedSymbol(v,:~:(dc),c) // negation of constrain
      case ConstrainRaw(a,dc,c) => ConstrainRaw(a,:~:(dc),c) // as above
      case _ => i // the complement leaves an instruction unchanged, for all other cases (for now)
    }
  }

  def isSatisfied (state: PolicyState, i : Instruction ) : Boolean = {
    var comp = complement(i)
    state.execute(comp) match {
      case FailedState => true
      case _ => false
    }
  }

  def isComposite (i: Instruction) : Boolean =
    i match {
      case InstructionBlock(_) => true
      case Fork(_) => true
      case _ => false
    }


  def show (i : Instruction) : String = show(i,0)

  /*
   case InstructionBlock(l) => "{"+(l.map(show).reduce((x,y)=> x+" ; "+y))+"}"
   case Fork(l) => "{"+(l.map(show).reduce((x,y)=> x+" || "+y))+"}"
   case ConstrainRaw(a,b,c) => a.toString+b.toString
   case AssignRaw(a,b,c) => a.toString+"="+b.toString
   case Forward(p)=> "Forward("+p.toString+")"
   */

  def show (i : Instruction, indent: Integer) : String = {
      def aux (sep : String, l : Iterable[Instruction], indent : Integer) = "{ \n"+(l.map((x)=>show(x,indent+1)).reduce((x,y)=> x+("  "*indent)+sep+"\n"+y)) + ("  "*indent) + "} \n"

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
      case AllocateSymbol(s) => "allocate("+s+")"
      case AllocateRaw(s,sz) => "allocate_raw("+s+")"
      case DeallocateRaw(s,_) => "deallocate("+s+")"
      case DeallocateNamedSymbol(s) => "deallocate_n("+s+")"
      case CreateTag(s,v) => "createTag("+s+")"
      case DestroyTag(s) => "destroyTag("+s+")"
      case AssignNamedSymbol(a,b,c) => a.toString+"="+b.toString
      case NoOp => "NoOp"
      }} + "\n"
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

  def verbose_print(s : String, t : PrintMode ) = if (mode.contains(t)) println(s)

  def atomic_check (p : Instruction, s : PolicyState) : Formula =
    if (isSatisfied(s, p)) {verbose_print (show(p)+" is true \n",MCMode); make(Atomic(p),Satisfied,s)} else {verbose_print (show(p)+" is false \n",MCMode); make(Atomic(p),Falsified,s)}



  /* VG ( VG f )  if VG is pending, it is not false in the current state, hence the search continues.
     VG f - if f is pending, then if it is true in a future state, it will be true in the current (pending) state


   */


  // this procedure verifies boolean formulae (and is independent on the program context verification, unlike temporal formulae
  // the check function represents the "program context" in which it appears
  def boolean_check (f:Formula, s:PolicyState, check : (Formula, PolicyState) => Formula) : Formula = {
    verbose_print("boolean check",MCMode)
    if (f.status != Pending) {verbose_print("already verified",MCMode);return f} //this formula was already verified


    f match {
      case fp@And(f1, f2) => check(f1, s).status match {
        case Falsified => make(fp,Falsified,s)
        // pending or satisfied
        case status => check(f2, s).status match {
          case Falsified => make(fp,Falsified,s)
          case Satisfied => make(fp,status,s) //whatever f1 returned is the new status
          case Pending => make(fp,Pending,s) //if f2 is pending and f1 is not false, the AND is pending
        }
      }
      case fp@Or(f1, f2) => check(f1, s).status match {
        case Satisfied => make(fp,Satisfied,s)
        case status => check(f2,s).status match {
          case Satisfied => make(fp,Satisfied,s)
          case Falsified => make(fp,status,s)
          case Pending => make(fp,Pending,s)
        }
      }
      case fp@Not(f) => check(f, s).status match {
        case Falsified => make(fp,Satisfied,s)
        case Satisfied => make(fp,Falsified,s)
        case Pending => make(fp,Pending,s)
      }

      case fp@Atomic(i) => atomic_check(i,s)
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
        }
      case _ => null
    }
  }


  // if a formula is declared pending, all its subformulae are declared pending, which prompts re-evaluation
  def make(f:Formula, s:Status, st:PolicyState) : Formula = {f.status = s; f.state = st; f}
  def clone(f:Formula, st:PolicyState) : Formula = {f.state = st; f}
  def clear(f:Formula) : Formula = {f.clear_valuation(); f}
  def reeval(f:Formula, st:PolicyState) : Formula = make(clear(f),Pending,st)



def check (f : Formula, p: Instruction, s : PolicyState) : Formula = {

  verbose_print("Verifying " + f + "\n on program \n" + show(p),OverallMode);
  println("Current history "+s.history)
  /*
  if (s.isInstanceOf[MapState] || s.isInstanceOf[NoMapState])
    verbose_print("State :"+s.state.instructionHistory,OverallMode)
*/

   //matching after formula and program
   (s,f,p) match {
      // failed state
     case (FailedState,Forall(Globally(_)),_) | (FailedState,Exists(Globally(_)),_) => verbose_print ("FG(f) is true (failed state)\n",OverallMode); make(f,Satisfied,s)
     case (FailedState,_,_) => verbose_print ("f is false (failed state)\n",OverallMode); //verbose_print("Program:"+last_instruction+"\n",OverallMode);
       make(f,Falsified,s)

     // if the formula is boolean, we evaluate it, using 'check'
     case (_,And(_,_),_) | (_,Or(_,_),_) | (_,Not(_),_)  => boolean_check(f,s,(f,s) => check(f,p,s))

     //flattening nested instruction blocks
     case (_,_,InstructionBlock(InstructionBlock(l) :: rest))
        => check(f, InstructionBlock(l ++ rest), s) // flatten nested instruction blocks

     // instructionBlock basis cases
     case (_,Forall(Globally(_)),InstructionBlock(Nil)) |
          (_,Exists(Globally(_)),InstructionBlock(Nil)) |
          (_,Success,InstructionBlock(Nil)) => verbose_print ("f is true (InstructionBlock ended)\n",OverallMode); make(f,Satisfied,s)


     // this case should be reinspected (for F operator)
     case (_,_,InstructionBlock(Nil)) => println("At the end of IB, history is:"+f.state.history+" versus "+s.history); clone(f,s)

     case (_,_,InstructionBlock(Fork(l) :: rest)) // distribute Fork instructions
         => check(f,
           Fork(l.map(p => InstructionBlock(p :: rest)))
           , s)
     case (_,_,InstructionBlock(If(test,then,els) :: rest)) => check(f,InstructionBlock(transform(s,If(test,then,els))::rest),s)


     case (_,_,InstructionBlock(Forward(_) :: _)) |
          (_,_,Forward(_)) =>
           var fp = f
           if (changesState(Forward(""))) fp = check_in_state(f,s)
           (s,p) match {
             case pair@(MapState(_,_,_,_,_), Forward(loc)) => var sp = s.forward(loc); var fpf = check(clone(fp,sp),s.instructionAt(loc),sp); println("Previous history is "+fpf.state.history); fpf
             case pair@(MapState(_,_,_,_,_), InstructionBlock(Forward(loc) :: _)) => var sp = s.forward(loc); var fpf = check(clone(fp,sp),s.instructionAt(loc),sp); println("Previous history is "+fpf.state.history); fpf
             case _ => {verbose_print("Skipping \n", MCMode);f}
           }

     case (_,_,InstructionBlock(pr :: rest)) => //take an instruction
       val sp = s.execute(pr)
       var fval = check(f,pr,sp) // execute program pr in state s, then check it
       if (fval.state != null)
          println("Verified first part of IB:"+pr+" Computed history is "+fval.state.history+" versus "+sp.history)

       (fval.status,f) match {
         // s, p;rest |= XG f  iff  s,p |= XG f   and p(s),rest |= XG f
         case (Falsified,Forall(Globally(_))) | (Falsified,Exists(Globally(_))) => verbose_print ("f is false (failed check of an instruction in an IB)\n",OverallMode); make(f,Falsified,fval.state)
           // otherwise continue verification
         case (_,Forall(Globally(_))) | (_,Exists(Globally(_))) => check(reeval(fval,fval.state),InstructionBlock(rest),sp)

         case (Satisfied,Forall(Future(_))) | (Satisfied,Exists(Future(_))) => verbose_print ("f is true (successful check of an instruction in an IB)\n",OverallMode); make(f,Satisfied,fval.state)
         case (_,Forall(Future(_))) | (_,Exists(Future(_))) => var fpf = check(reeval(fval,fval.state),InstructionBlock(rest),sp); println("Finished checking "+InstructionBlock(rest)+" history is "+fpf.state.history); fpf

         case (_,Forall(_)) | (_,Exists(_)) => throw new Exception("Not a CTL formula")

         case (_,_) => fval // if the formula is not temporal, it has been checked;

       }


     case (_,Forall(_),Fork(Nil)) => verbose_print("Final branch checked\n",MCMode); verbose_print ("f is true (finished a Fork)\n",OverallMode); make(f,Satisfied,lastState)
     case (_,Exists(_),Fork(Nil)) => verbose_print("Final branch checked\n",MCMode); verbose_print ("f is false (finished a Fork)\n",OverallMode); make(f,Falsified,lastState)

     case (_,_,Fork(pr::rest)) => verbose_print("Checking branch:\n"+show(pr),MCMode);
       val fp = check(f,pr,s)
       lastState = fp.state
       (f,fp.status) match {
         case (Exists(_),Satisfied) => verbose_print ("f is true (on a Fork branch)\n",OverallMode); make (f, Satisfied, fp.state)
         case (Forall(_),Pending) | (Forall(_),Falsified) => verbose_print ("f is false (on a Fork branch)\n",OverallMode); make(f,Falsified, fp.state)
         case (Exists(_),_) | (Forall(_),Satisfied) =>  check(reeval(f,s),Fork(rest),s) //the formula must be made pending for the next branch verification

         case (_,_) => throw new Exception ("Cannot evaluate non-temporal formula on a Fork ")

       }

       // IF implementation
     case (_,_,If(testInstr: Instruction, thenWhat: Instruction, elseWhat:Instruction)) =>
       check(f,transform(s,p),s)

       /*
       // forwarding instruction received
     case (_,_,Forward(loc)) => s match {
       case sp@MapState(_,_,_,_) => check(f,sp.instructionAt(loc),sp.forward(loc))
       case _ => verbose_print("Ignoring Forward instr.",LocMode); check(f,NoOp,s)

     }
     */
       // *  *  *  *  *  *  *  *
       // non-branching programs
       // *  *  *  *  *  *  *  *

       // if the current instruction does not change state, the status of f is unchanged
     case _ => {
       if (changesState(p)) check_in_state(f,s) else f
       /*

       var fp : Formula = null;
       if (changesState(p)) fp = check_in_state(f,s) else fp = f
       fp


       (s,p) match {
         case pair@(MapState(_,_,_,_,_), Forward(loc)) => var sp = s.forward(loc); var fpf = check(clone(fp,sp),s.instructionAt(loc),sp); println("Previous history is "+fpf.state.history); fpf
         case _ => {verbose_print("Skipping \n", MCMode);fp}
       }*/

     }

       /*
       (f,p) match {
             // temporal formula on non-branching program
           case (Exists(Globally(_)),_) | (Forall(Globally(_)),_) |
                (Exists(Future(_)),_) | (Forall(Future(_)),_)
                => make(f,check(f.inner.inner,p,s).status)

           case (Atomic(fp),_) => atomic_check(fp,s)
           case _ => throw new Exception ("non-branching program on non-temporal formula: "+f)

         }
         */
   }
 }

  def check_in_state(f : Formula, s: PolicyState) : Formula = {
    f match {
      // temporal formula on non-branching program
      case Exists(Globally(_)) | Forall(Globally(_)) | Exists(Future(_)) | Forall(Future(_))
      => make(f, check_in_state(f.inner.inner, s).status, s)
      case And(_, _) | Or(_, _) | Not(_) => boolean_check(f, s, check_in_state)
      case Atomic(fp) => atomic_check(fp, s)
      case _ => throw new Exception("non-branching program on non-temporal formula: " + f)
    }
  }
  // "top-level" verification procedure for topologies
  def verify (f: Formula, start:LocationId ,topology: Topology, links:Map[LocationId,LocationId]) : Boolean = {

    var fp = check(f,topology(start),state(start,topology,links))

    fp.status match {
      case Falsified => verbose_print("Formula is false",OverallMode); println("History: "+fp.state.history); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); println("History: "+fp.state.history); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false

    }
  }


  // "top-level" verification procedure for plain SEFL code
  def verify (f : Formula, model : Instruction) : Boolean = {

    check(f,model,state).status match {
      case Falsified => verbose_print("Formula is false",OverallMode); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true",OverallMode); true
      case Pending => verbose_print("There are still pending subformulae",OverallMode); false

    }
  }

}

