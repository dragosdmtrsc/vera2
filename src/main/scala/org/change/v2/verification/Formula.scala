package org.change.v2.verification.Formula

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.ConstrainRaw
import org.change.v2.verification.{MapState, PolicyState}

/**
  * Created by matei on 17/01/17.
  */


trait Formula {
  def inner: Formula

  def subformulaOf(f: Formula): Boolean

  // a formula F is satisfied in the presence of some obligations, if no obligation is a subformula of F
  def satisfiedUnderObligations(o: Set[Formula]): Boolean = !((o.map((fp) => fp.subformulaOf(this))).reduce(_ || _))

  def isForall: Boolean = false

  var status: Status = Pending

  var state: PolicyState = null

  /*
  //report the reason for the state of this formula
  def report = {
    (this, status, state) match {
      case tr@(Forall(_), Falsified, MapState(_, _, _, _, portHistory)) => println("The formula was invalidated on branch " + state.history);
    }
  }*/

  def clear_valuation()

}

trait Status

case object Pending extends Status {
  override def toString: String = "P"
}

case object Satisfied extends Status {
  override def toString: String = "S"
}

case object Falsified extends Status {
  override def toString: String = "F"
}


case object Success extends Formula {
  override def inner: Formula = null

  override def clear_valuation(): Unit = {}

  override def subformulaOf(f: Formula): Boolean = f match {
    case Success => true
    case _ => false
  }
}

case object Fail extends Formula {
  override def inner: Formula = null

  override def clear_valuation(): Unit = {}

  override def subformulaOf(f: Formula): Boolean = f match {
    case Fail => true
    case _ => false
  }
}


case class Atomic(prog: Instruction) extends Formula {
  override def inner = null

  override def subformulaOf(f: Formula) = this.equals(f)

  override def clear_valuation() = {
    status = Pending
  }

  // equality does not work for any programs !!!!!!
  override def equals(that: Any): Boolean =
    that match {
      case Atomic(p) => (p, prog) match {
        case (ConstrainRaw(a, b, c), ConstrainRaw(x, y, z)) => a.apply(State.bigBang).equals(x.apply(State.bigBang))
      }
      case _ => false
    }

  override def toString: String = "[" + status + "]" + {
    prog match {
      case ConstrainRaw(a, b, c) => a.toString + b.toString
      case p => p.toString
    }
  }
}


case class Forall(f: Formula) extends Formula {
  override def inner: Formula = f

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Forall(fp) => f.equals(fp)
      case _ => false
    }

  override def isForall: Boolean = true

  override def toString: String = "A[" + status + "] " + f.toString


}

case class Exists(f: Formula) extends Formula {
  override def inner: Formula = f

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Exists(fp) => f.equals(fp)
      case _ => false
    }

  override def toString: String = "E[" + status + "] " + f.toString

}

case class Future(f: Formula) extends Formula {
  override def inner: Formula = f

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Future(fp) => f.equals(fp)
      case _ => false
    }

  override def toString: String = "F[" + status + "] " + f.toString
}

case class Globally(f: Formula) extends Formula {
  override def inner: Formula = f

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Globally(fp) => f.equals(fp)
      case _ => false
    }

  override def toString: String = "G[" + status + "] " + f.toString
}

case class Or(f: Formula, fp: Formula) extends Formula {
  override def inner: Formula = null

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation(); fp.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Or(g, gp) => f.equals(g) && fp.equals(gp)
      case _ => false
    }

  override def toString: String = "{" + f.toString + "} V[" + status + "] " + "{" + fp.toString + "}"
}

case class And(f: Formula, fp: Formula) extends Formula {
  override def inner: Formula = null

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation(); fp.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case And(g, gp) => f.equals(g) && fp.equals(gp)
      case _ => false
    }

  override def toString: String = "{" + f.toString + "} ^[" + status + "] " + "{" + fp.toString + "}"
}

/*
case class Until (f : Formula, fp:Formula) extends Formula {}


*/

case class Not(f: Formula) extends Formula {
  override def inner: Formula = f

  override def subformulaOf(f: Formula) = f.equals(this) || this.subformulaOf(f.inner)

  override def clear_valuation() = {
    status = Pending; f.clear_valuation()
  }

  override def equals(that: Any): Boolean =
    that match {
      case Not(fp) => f.equals(fp)
      case _ => false
    }

  override def toString: String = "~[" + status + "] " + f.toString
}