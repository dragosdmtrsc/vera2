package org.change.parser.p4.control

import org.change.v2.p4.model.Switch
import org.change.v3.semantics.SimpleMemory
import org.change.v3.semantics.context

class MergingSEFLSemantics(sw : Switch) extends SEFLSemantics(sw) {

  private def merge(all : List[SimpleMemory]) : List[SimpleMemory] = {
    val (fails, succs) = all.partition(_.errorCause.nonEmpty)
    (if (succs.nonEmpty) {
      val hd = succs.head
      val others = succs.tail
      others.foldLeft(hd)((acc, next) => {
        assert(acc.symbols.keySet == next.symbols.keySet)
        val delta = acc.symbols.zip(next.symbols).filter(h => {
          val ((k1, v1), (k2, v2)) = h
          assert(k1 == k2)
          val ast1 = v1.ast.get
          val ast2 = v2.ast.get
          !ast1.equals(ast2)
        }).map(_._1._1)
        val newconsts = delta.map(x => {
          x -> context.mkFreshConst(
            x,
            acc.symbols(x).ast.get.getSort
          )
        }).toMap
        val addAcc = newconsts.map(sym => {
          val ast = acc.symbols(sym._1).ast.get
          context.mkEq(ast, sym._2)
        }).toList
        val newacc = acc.addCondition(addAcc)
        val addNext = newconsts.map(sym => {
          val ast = next.symbols(sym._1).ast.get
          context.mkEq(ast, sym._2)
        }).toList
        val newnext = next.addCondition(addNext)
        val newsyms = newconsts.foldLeft(acc.symbols)((state, nv) => {
          state + (nv._1 -> state(nv._1).copy(ast = Some(nv._2)))
        })
        val newpc = context.mkOr(context.mkAnd(newacc.pathCondition:_*),
          context.mkAnd(newnext.pathCondition:_*)
        ) :: Nil
        acc.copy(symbols = newsyms, pathCondition = newpc)
      }) :: Nil
    } else {
      succs
    }) ++ fails
  }

  override def merge(crt: List[SimpleMemory],
                     other: List[SimpleMemory]): List[SimpleMemory] = {
    val all = crt ++ other
    merge(all)
  }
}
