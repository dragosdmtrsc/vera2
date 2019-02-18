package org.change.utils.graph

import org.change.v2.p4.model.parser.{ExtractStatement, Statement}

import scala.collection.mutable

case class SCCContext[T](lowlinks : mutable.Map[T, Int],
                      index : mutable.Map[T, Int],
                      onStack : mutable.Set[T],
                      stack : mutable.Stack[T],
                      st : mutable.ListBuffer[List[T]],
                      var globalIndex : Int) {
  def next() : Int = {
    globalIndex = globalIndex + 1
    globalIndex - 1
  }
}
class Graph[T](val edges : Map[T, List[T]]) {

  override def toString: String = {
    val sb = new mutable.StringBuilder()
    for (e <- edges) {
      for (d <- e._2) {
        sb.append(e._1 + " -> " + d + "\n")
      }
    }
    sb.toString()
  }

  def sp(start : T, end : T): Int = {
    val b = start.equals(end)
    var bfq = mutable.Queue.empty[T]
    val dist = mutable.Map.empty[T, Int]
    if (b) {
      bfq.enqueue(getNeighs(start):_*)
      getNeighs(start).foreach(dist.put(_, 1))
    } else {
      dist.put(start, 0)
      bfq.enqueue(start)
    }
    var found = false
    while (bfq.nonEmpty && !found) {
      val first = bfq.dequeue()
      if (!first.equals(end)) {
        for (n <- getNeighs(first)) {
          if (!dist.contains(n)) {
            val my = dist(first)
            dist.put(n, my + 1)
            bfq.enqueue(n)
          }
        }
      } else {
        found = true
      }
    }
    if (found)
      dist(end)
    else Int.MaxValue
  }

  private def getNeighs(n : T): List[T] = if (edges contains n) edges(n) else Nil
  def subGraph(filter : T => Boolean) : Graph[T] = {
    def span(start : List[T]): List[T] = {
      var candidates = start
      var goods = List.empty[T]
      var visited = Set.empty[T]
      while (candidates.nonEmpty) {
        val h = candidates.head
        candidates = candidates.tail
        visited = visited + h
        if (filter(h)) {
          goods = h :: goods
        } else {
          for (n <- getNeighs(h))
            if (!(visited contains n))
              candidates = n :: candidates
        }
      }
      goods
    }
    new Graph[T](edges.filter(x => filter(x._1)).map(edge => {
      edge._1 -> span(edge._2)
    }))
  }

  private def strongConnect(ctx : SCCContext[T])(v : T): Unit = {
    val idx = ctx.next()
    ctx.index.put(v, idx)
    ctx.lowlinks.put(v, idx)
    ctx.stack.push(v)
    ctx.onStack.add(v)

    if (edges contains v) {
      for (w <- edges(v)) {
        if (!ctx.index.contains(w)) {
          strongConnect(ctx)(w)
          val my = ctx.lowlinks(v)
          val his = ctx.lowlinks(w)
          ctx.lowlinks.put(v, Math.min(my, his))
        } else if (ctx.onStack contains w) {
          val my = ctx.lowlinks(v)
          val his = ctx.index(w)
          ctx.lowlinks.put(v, Math.min(my, his))
        } else {
          // nada
        }
      }
    }
    val ll = ctx.lowlinks(v)
    if (ll == idx) {
      var scc = List.empty[T]
      var w = ctx.stack.pop()
      ctx.onStack.remove(w)
      scc = w :: scc
      while (!w.equals(v)) {
        w = ctx.stack.pop()
        ctx.onStack.remove(w)
        scc = w :: scc
      }
      ctx.st.append(scc)
    }
  }

  private def sccInternal(lowlinks : mutable.Map[T, Int],
                  index : mutable.Map[T, Int],
                  onStack : mutable.Set[T],
                  stack : mutable.Stack[T])(): List[List[T]] = {

    val init = mutable.ListBuffer.empty[List[T]]
    val ctx = SCCContext(lowlinks, index, onStack, stack, init, 0)
    for (t <- edges) {
      if (!index.contains(t._1))
        strongConnect(ctx)(t._1)
    }
    init.toList
  }
  def scc() : List[List[T]] = {
    sccInternal(mutable.Map.empty,
      mutable.Map.empty,
      mutable.Set.empty,
      new mutable.Stack[T])()
  }
}
