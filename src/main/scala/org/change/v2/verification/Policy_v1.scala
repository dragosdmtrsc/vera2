package org.change.v2.verification

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.verification.Formula._

/**
 * Created by matei on 12/01/17.
 * TODO: optimise such that subformulae are not checked repeatedly. Suspended because it might make debugging harder
 */
object Policy_v1 {

  def verbose = true

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

  // very simplistic implementation of complement
  def complement (i : Instruction) : Instruction = {
    i match {
      case InstructionBlock(l) => Fork(l.map(complement))
      case ConstrainNamedSymbol(v,dc,c) => ConstrainNamedSymbol(v,:~:(dc),c) // negation of constrain
      case ConstrainRaw(a,dc,c) => ConstrainRaw(a,:~:(dc),c) // as above
      case _ => i // the complement leaves an instruction unchanged, for all other cases (for now)
    }
  }

  def isSatisfied (state: State, i : Instruction ) : Boolean = {
    var comp = complement(i)
    var (success,failed) = comp.apply(state)
    success.isEmpty //
  }

  def isComposite (i: Instruction) : Boolean =
    i match {
      case InstructionBlock(_) => true
      case Fork(_) => true
      case _ => false
    }


  def show (i : Instruction) : String = i match {
    case InstructionBlock(l) => "{"+(l.map(show).reduce((x,y)=> x+" ; "+y))+"}"
    case Fork(l) => "{"+(l.map(show).reduce((x,y)=> x+" || "+y))+"}"
    case ConstrainRaw(a,b,c) => a.toString+b.toString
    case AssignRaw(a,b,c) => a.toString+"="+b.toString

  }


  def changesState (i: Instruction) : Boolean = true

  def verbose_print(s : String ) = if (verbose) println(s)

  def atomic_check (p : Instruction, s : State) : Formula =
    if (isSatisfied(s, p)) make(Atomic(p),Satisfied) else make(Atomic(p),Falsified)

  def seqcheck (f: Formula, s : State): Formula = {
    verbose_print("Verifying formula "+f)
    if (f.status != Pending) return f //this formula was already verified

    //verbose_print("In state "+s)
    f match {
      case fp@(Forall(Globally(_)) | Exists(Globally(_))) =>
        var fpp = fp.inner.inner // the inner formula
        var fx = seqcheck(fpp,s)
        fx.status match {
          // if fpp is false, then "XG fpp" is false
          case Falsified => make(fp,Falsified)
          // the current formula needs to be checked in future states
          case _ => reeval(fp)
        }

      case fp@(Forall(Future(_)) | Exists(Future(_))) =>
        var fpp = fp.inner.inner // the inner formula
        var fx = seqcheck(fpp,s)

        fx.status match {
          // if fpp is true, then "XF fpp" is true
          case Satisfied => make(fp,Satisfied)
          // if fpp is false, then "XF fpp" is an obligation
          // if fpp is pending, then "XF fpp" is pending
          case _ => reeval(fp)

        }
      case Forall(_) | Exists(_) => throw new Exception("Not a CTL formula")

      case Atomic(p) => atomic_check(p,s)

        // must be a boolean formula
      case fp => boolean_check(fp,s,seqcheck)

    }
  }

  // this procedure verifies boolean formulae (and is independent on the program context verification, unlike temporal formulae
  // the check function represents the "program context" in which it appears
  def boolean_check (f:Formula, s:State, check : (Formula, State) => Formula) : Formula = {
    verbose_print("boolean check")
    if (f.status != Pending) {verbose_print("already verified");return f} //this formula was already verified


    f match {
      case fp@And(f1, f2) => check(f1, s).status match {
        case Falsified => make(fp,Falsified)
        // pending or satisfied
        case status => check(f2, s).status match {
          case Falsified => make(fp,Falsified)
          case Satisfied => make(fp,status) //whatever f1 returned is the new status
          case Pending => make(fp,Pending) //if f2 is pending and f1 is not false, the AND is pending
        }
      }
      case fp@Or(f1, f2) => check(f1, s).status match {
        case Satisfied => make(fp,Satisfied)
        case status => check(f2,s).status match {
          case Satisfied => make(fp,Satisfied)
          case Falsified => make(fp,status)
          case Pending => make(fp,Pending)
        }
      }
      case fp@Not(f) => check(f, s).status match {
        case Falsified => make(fp,Satisfied)
        case Satisfied => make(fp,Falsified)
        case Pending => make(fp,Pending)
      }

      case fp@Atomic(i) => atomic_check(i,s)
    }
  }

  //verifies a formula in a final state of a path
  def final_check (f: Formula, s : State): Formula = {
    verbose_print("Verifying in final state formula "+f)
    if (f.status != Pending) return f //this formula was already verifieds

    //verbose_print("In state "+s)
    f match {
      case fp@(Forall(Globally(_)) | Exists(Globally(_)) | Forall(Future(_)) | Exists(Future(_))) => //temporal operators are evaluated in the final state.
        var fpp = fp.inner.inner // the inner formula
        final_check(fpp, s).status match {
          case Satisfied => make(fp,Satisfied)
          case _ => make (fp,Falsified)         // if the formula is still pending or false in the final state, then is is false
        }

      case fp@(Atomic(i)) => atomic_check(i,s)
      // must be a boolean formula
      case fp => boolean_check(fp, s, final_check)
    }
  }


  // if a formula is declared pending, all its subformulae are declared pending, which prompts re-evaluation
  def make(f:Formula, s:Status) : Formula = {f.status = s; f}
  def clear(f:Formula) : Formula = {f.clear_valuation(); f}
  def reeval(f:Formula) : Formula = make(clear(f),Pending)


  def check (o : Formula, p: Instruction, s : State) : Formula = {
    verbose_print("Verifying " + o + "\n on program \n" + show(p))

    //matching after formula and program
    (o,p) match {
      // if the formula is boolean, we evaluate it
      case (And(_,_),_) | (Or(_,_),_) | (Not(_),_)  => boolean_check(o,s,(f,s) => check(f,p,s))
                                                                                        //this is a 'continuation', which 'stores' the program to check


      // instructionBlock situations
      case (_,InstructionBlock(pr :: Nil)) => check(o, pr, s) //single-instruction
      case (_,InstructionBlock(InstructionBlock(l) :: prp :: rest))
      => check(o, InstructionBlock(l ++ (prp :: rest)), s) // flatten nested instruction blocks
      case (_,InstructionBlock(Fork(l) :: rest)) // distribute Fork instructions
      => check(o,
        Fork(l.map(p => InstructionBlock(p :: rest)))
        , s)

      case (_,InstructionBlock(pr :: prp :: rest)) => //just an instruction
        if (!changesState(pr)) {
          // if this instruction does not change state, we simply execute it and continue
          pr.apply(s) match {
            case (Nil, _) => make(o,Satisfied) //the path is unsuccessful, hence the formula is trivially true
            case (sp :: _, _) => check(o, InstructionBlock(prp :: rest), sp) // execute one instruction and continue
          }
        } else {
          // if at this instruction we need to look at the formulae, we check each of our obligations
          // and generate new obligations (newo)

          pr.apply(s) match {
            case (Nil, _) => make(o,Satisfied) //the path is unsuccessful, the formula is trivially true
            case (sp :: _, _) => {
              val f = seqcheck(o,sp)
              if (f.status == Falsified || f.status == Satisfied) //truth-value established
                f
              else
                check(f, InstructionBlock(prp :: rest), sp)
            }
          }



        }

      case (_,Fork(pr::Nil)) => verbose_print("Checking final branch:\n"+show(pr)); check (o,pr,s) // single-instruction fork
      case (_,Fork(pr::rest)) => verbose_print("Checking branch:\n"+show(pr)); (o, check (o,pr,s).status) match { //execute the first branch of the Fork
        case (Exists(_),Satisfied) => make(o,Satisfied)
        case (Forall(_),Pending) | (Forall(_),Falsified) => make(o,Falsified)
        case (Exists(_),_) | (Forall(_),Satisfied) => check(reeval(o),Fork(rest),s) //the formula must be made pending for the next branch verification
      }



      // a non-branching instruction at this level ends execution
      case (_,pr) => pr.apply(s) match {
        case (Nil, _) => make(o,Satisfied) // unsuccesful path
        case (sp::_ , _) => final_check(o,sp)
      }

    }
  }

  // "top-level" verification procedure
  def verify (f : Formula, model : Instruction) : Boolean = {
    check(f,model,State.bigBang).status match {
      case Falsified => verbose_print("Formula is false"); false     // should report the failing path
      case Satisfied => verbose_print("Formula is true"); true
      case Pending => verbose_print("There are still pending subformulae"); false

    }
  }

}

