package org.change.utils.graph


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
class LabeledGraph[T, Label](val edges : Map[T, List[(T, Label)]]) {
  private lazy val graphView: Graph[T] = new Graph[T](edges.mapValues(_.map(h => h._1)))
  private lazy val sccs = graphView.scc()
  private lazy val nodeorder = sccs.zipWithIndex.flatMap(h => {
    h._1.map(_ -> h._2)
  }).toMap
  def propagate[V](start : Map[T, V],
                   propagate : (V, T, T, Label) => Option[V],
                   merge : (T, V, V) => Option[V]) : Map[T, V] = {
    val q = mutable.PriorityQueue.empty[T](Ordering.fromLessThan((n1 : T, n2 :T) => {
      nodeorder(n1) >= nodeorder(n2)
    }))
    var crt = start
    start.keys.foreach(u => q.enqueue(u))
    while (q.nonEmpty) {
      val nxt = q.dequeue()
      for (h <- edges.getOrElse(nxt, Nil)) {
        val propd = propagate(crt(nxt), nxt, h._1, h._2)
        if (propd.nonEmpty) {
          if (crt.contains(h._1)) {
            val merged = merge(h._1, crt(h._1), propd.get)
            if (merged.nonEmpty) {
              crt = crt + (h._1 -> merged.get)
              q.enqueue(h._1)
            }
          } else {
            crt = crt + (h._1 -> propd.get)
            q.enqueue(h._1)
          }
        }
      }
    }
    crt
  }
}
class Graph[T](val edges : Map[T, List[T]]) {

  lazy val backward : Graph[T] = new Graph[T](edges.toIterable.flatMap(x => {
      x._2.map(h => h -> x._1)
    }).groupBy(x => x._1).mapValues(r => r.map(_._2).toList))

  override def toString: String = {
    val sb = new mutable.StringBuilder()
    for (e <- edges) {
      for (d <- e._2) {
        sb.append(e._1 + " -> " + d + "\n")
      }
    }
    sb.toString()
  }

  def sp(start : T, end : T, filter : T => Boolean = _ => true): Int = {
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
            if (filter(first)) dist.put(n, my + 1)
            else dist.put(n, my)
            bfq.enqueue(n)
          }
        }
      } else {
        found = true
      }
    }
    if (found)
      dist(end)
    else {
      if (!b) {
        Int.MaxValue
      } else {
        1
      }
    }
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
  def map[V](start : T, fun : T => V): Iterable[V] = {
    var visited = Set.empty[T]
    var q = List(start)
    var l = List.empty[V]
    while (q.nonEmpty) {
      val crt = q.head
      q = q.tail
      l = fun(crt) :: l
      visited = visited + crt
      for (x <- edges.getOrElse(crt, Nil)) {
        if (!visited.contains(x)) {
          q = x :: q
        }
      }
    }
    l
  }
  def induced(filter : T => Boolean): Graph[T] = {
    new Graph[T](edges.filter(x => filter(x._1)).mapValues(r => {
      r.filter(filter)
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

  def dominators(start : T) : Map[T, Set[T]] = {
    val in = mutable.Map.empty[T, Set[T]]
    var changes = true
    in(start) = Set.empty
    var q = mutable.Queue.empty[T]
    q.enqueue(start)
    while (q.nonEmpty) {
      val node = q.dequeue()
      val ins = in(node)
      val propagate = ins + node
      for (neigh <- edges.getOrElse(node, Nil)) {
        if (in.contains(neigh)) {
          val nxt = in(neigh)
          val newdoms = nxt.intersect(propagate)
          if (newdoms != nxt) {
            in(neigh) = newdoms
            q.enqueue(neigh)
          }
        } else {
          in(neigh) = propagate
          q.enqueue(neigh)
        }
      }
    }
    in.map(f => {
      f._1 -> (f._2 + f._1)
    }).toMap
  }

  private def traverse(graph: Graph[T], from : T, to : T): Set[T] = {
    var q = List(from)
    var s = Set.empty[T]
    while (q.nonEmpty) {
      val crt = q.head
      q = q.tail
      s = s + crt
      for (e <- graph.edges.getOrElse(crt, Nil)) {
        if (e != to && !s.contains(e))
          q = e :: q
      }
    }
    s + to
  }

  // a set of edges (n -> h) and a set of nodes L s.t.
  // let < the dominator relation
  // h < n and forall x\in L h < x and !(n < x)
  def loops(start : T) : Iterable[(T, T, Graph[T])] = {
    val doms = dominators(start)
    var l = List.empty[(T, T, Graph[T])]
    for (x <- doms) {
      for (domd <- x._2) {
        if (edges.getOrElse(x._1, Nil).contains(domd)) {
          val st = traverse(backward, x._1, domd)
          println(s"loop edge: ${x._1} -> $domd => { ${st.mkString(";")} }")
          l = (x._1, domd, induced(st.contains)) :: l
        }
      }
    }
    l
  }
  def rmEdges(them : Iterable[(T, T)]): Graph[T] = new Graph(them.foldLeft(edges)((acc, x) => {
      acc.get(x._1).map(v => {
        v.filter(e => e != x)
      })
      .flatMap(edges => if (edges.isEmpty) None else Some(x._1 -> edges))
      .map(h => acc + h)
      .getOrElse(acc)
    }))
  def rmLoops(start : T) : Graph[T] = rmEdges(loops(start).map(h => (h._1, h._2)))
}
