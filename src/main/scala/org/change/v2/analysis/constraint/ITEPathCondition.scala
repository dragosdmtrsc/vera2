package org.change.v2.analysis.constraint


trait ITENode
case class InfoNode(oP: OP, low : ITENode, high : ITENode) extends ITENode
object TrueNode extends ITENode
object FalseNode extends ITENode


case class ITEPathCondition(root : ITENode, reverseMap : Set[InfoNode], vars : Set[OP]) extends PathCondition[ITEPathCondition] {
  override def &&(condition: Condition) = {
    this && ITEPathCondition.fromCondition(condition)
  }

  override def ||(pathCondition: ITEPathCondition) = {
    val allVars = vars ++ pathCondition.vars
    allVars.foldLeft(this)((acc, x) => {
      acc
    })
  }



  override def &&(pathCondition: ITEPathCondition) = ???
}
object OpCompare {
  def apply(o1: OP, o2 : OP) : Int = {
    o1.toString.compareTo(o2.toString)
  }
}


object ITEPathCondition {
  def fromCondition(condition: Condition) : ITEPathCondition = condition match {
    case o : OP =>
      val inode = InfoNode(
        o, low = FalseNode, high = TrueNode
      )
      ITEPathCondition(inode, Set(inode), Set(o))
    case FAND(conditions) => conditions.foldLeft(ITEPathCondition(FalseNode, Set.empty, Set.empty))((acc, x) => {
      acc && fromCondition(x)
    })
    case FOR(conditions) => conditions.foldLeft(ITEPathCondition(FalseNode, Set.empty, Set.empty))((acc, x) => {
      acc || fromCondition(x)
    })
    case FNOT(condition) =>
      val nd = fromCondition(condition)
      val newRoot = nd.root match {
        case InfoNode(oP, low, high) => InfoNode(oP, high, low)
        case TrueNode => FalseNode
        case FalseNode => TrueNode
      }
      nd.copy(root = newRoot)
    case TRUE => ITEPathCondition(TrueNode, Set.empty, Set.empty)
    case FALSE => ITEPathCondition(FalseNode, Set.empty, Set.empty)
    case _ => ???
  }
}