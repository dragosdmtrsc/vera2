package org.change.p4.control

import org.change.p4.control.types.{BVType, IntType, P4Type}
import org.change.p4.model.Switch
import org.change.p4.model.parser.{FieldRef, MaskedFieldRef}
import org.change.p4.model.table.{MatchKind, TableDeclaration, TableMatch}
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.JavaConverters._
import scala.collection.mutable

class FlowStruct(context : Z3Context,
                      table : TableDeclaration,
                      switch: Switch) extends P4Type {
  val allowed : Iterable[String] = if (table.hasProfile) {
    table.actionProfile().getActions.asScala
  } else {
    table.getAllowedActions.asScala.map(_.getActionName)
  }

  private val actNameToId = allowed.zipWithIndex.toMap

  private val enumKind = context.mkADTSorts(Seq(
    (s"Action_${table.getName}",
      allowed.toSeq,
      allowed.map(_ => Seq()).toSeq
    )
  )).head

  private def keySort(x : TableMatch): Z3Sort = {
    x.getReferenceKey match {
      case f : FieldRef =>
        context.mkBVSort(f.getFieldReference.getLength)
      case masked : MaskedFieldRef =>
        context.mkBVSort(masked.getReference.getFieldReference.getLength)
    }
  }
  private val matchToSorts = table.getMatches.asScala.map(x => {
    x.getMatchKind match {
      case MatchKind.Valid => (x, context.mkBoolSort() :: Nil)
      case MatchKind.Exact => (x, keySort(x) :: Nil)
      case MatchKind.Lpm => (x, keySort(x) :: keySort(x) :: Nil)
      case MatchKind.Ternary => (x, keySort(x) :: keySort(x) :: Nil)
      case MatchKind.Range => (x, keySort(x) :: keySort(x) :: Nil)
    }
  }).toList
  private val matchValueIndices = matchToSorts.foldLeft((0, Map.empty[TableMatch, (Int, Int)]))((acc, x) => {
    val (idx, crt) = acc
    (idx + x._2.size, crt + (x._1 -> (idx, idx + x._2.size - 1)))
  })

  private def matchValueOffset(tableMatch: TableMatch): Int =
    matchValueIndices._2(tableMatch)._1 + 3
  private def matchMaskOffset(tableMatch: TableMatch): Int =
    matchValueIndices._2(tableMatch)._2 + 3

  val matchKinds : List[Z3Sort] = matchToSorts.flatMap(_._2)
  val queryParms : List[Z3Sort] = matchToSorts.map(_._2.head)

  def p4typeToSort(p4Type: P4Type) : Z3Sort = p4Type match {
    case IntType => context.mkIntSort()
    case BVType(x) => context.mkBVSort(x)
  }
  val parmTypes = allowed.flatMap(act => {
    switch.action(act).getParameterList.asScala.map(parm => {
      (act, parm.getParamName) -> p4typeToSort(parm.getSort)
    })
  }).toList

  private val parmsZipped: Map[(String, String), Int] = parmTypes.zipWithIndex.map(x => x._1._1 -> x._2).toMap
  private def getOffset(action : String, parm : String): Int = {
    parmsZipped((action, parm)) + matchKinds.size + 3
  }

  private val structTypes =
    // flow exists
    context.mkBoolSort() ::
    // flow is default
    context.mkBoolSort() ::
    // action run
    enumKind._1 ::
    // match keys
    matchKinds ++
    //action parameters
    parmTypes.map(_._2)
  val flowSort: (Z3Sort, Z3FuncDecl, Seq[Z3FuncDecl]) = context.mkTupleSort(s"FlowDef_${table.getName}",
    structTypes:_*
  )
  //query function
  val queryFun: Z3FuncDecl = context.mkFuncDecl(s"query_${table.getName}",
    queryParms, flowSort._1
  )
  def isAction(action : String, value : Z3AST) : Z3AST =
    enumKind._3(actNameToId(action))(value)
  def actionTypeConst(action : String) : Z3AST =
    enumKind._2(actNameToId(action))()
  case class FlowInstance(z3AST: Z3AST) extends IFlowInstance{
    def exists() : Z3AST = flowSort._3.head(z3AST)
    def hits() : Z3AST = flowSort._3(1)(z3AST)
    def actionRun() : Z3AST = flowSort._3(2)(z3AST)
    def getMatchValue(tableMatch: TableMatch) : Z3AST =
      flowSort._3(matchValueOffset(tableMatch))(z3AST)
    def getMatchMask(tableMatch: TableMatch) : Z3AST =
      flowSort._3(matchMaskOffset(tableMatch))(z3AST)
    def getActionParam(action : String, parmName : String): Z3AST =
      flowSort._3(getOffset(action, parmName))(z3AST)
    def getActionParamType(action : String, parmName : String) : P4Type = {
      switch.action(action).getParameterList
        .asScala.find(_.getParamName == parmName).get.getSort
    }
  }
  def query(asts : List[Z3AST]) : FlowInstance =
    FlowInstance(queryFun.apply(asts:_*))
  def from(ast : Z3AST): FlowInstance = {
    if (ast.getSort != flowSort._1)
      throw new IllegalArgumentException(s"expecting ${flowSort._1}, got ${ast.getSort}")
    FlowInstance(ast)
  }
  def sort() : Z3Sort = flowSort._1
}

object FlowStruct {
  private val flowStructLedger: mutable.Map[(Z3Context, String), FlowStruct] = mutable.Map.empty

  def apply(context: Z3Context,
            table: TableDeclaration,
            switch: Switch): FlowStruct = {
    flowStructLedger.getOrElseUpdate((context, table.getName), new FlowStruct(context, table, switch))
  }
}
