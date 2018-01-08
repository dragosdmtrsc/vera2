package org.change.parser.p4.parser

// A Java program to print topological sorting of a DAG
import java.io._
import java.util
import java.util._

// This class represents a directed graph using adjacency
// list representation
object Graph {
  // Driver method
  def main(args: Array[String]) {
    // Create a graph given in the above diagram
    val g: Graph = new Graph(6)
    g.addEdge(5, 2)
    g.addEdge(5, 0)
    g.addEdge(4, 0)
    g.addEdge(4, 1)
    g.addEdge(2, 3)
    g.addEdge(3, 1)
    System.out.println("Following is a Topological " + "sort of the given graph")
    g.topologicalSort()
  }
}

class Graph(var V: Int)
{
  private var  adj = new Array[LinkedList[Integer]](V)
  var i: Int = 0
  while (i < V) {
    adj(i) = new LinkedList[Integer] {
      i += 1; i
    }
  }
  // Function to add an edge into the graph
  def addEdge(v: Int, w: Int) {
    adj(v).add(w)
  }

  // A recursive function used by topologicalSort
  def topologicalSortUtil(v: Int, visited: Array[Boolean], stack: Stack[Integer]) {
    // Mark the current node as visited.
    visited(v) = true
    var i: Integer = null
    // Recur for all the vertices adjacent to this
    // vertex
    val it: Iterator[Integer] = adj(v).iterator
    while (it.hasNext) {
      {
        i = it.next
        if (!visited(i)) topologicalSortUtil(i, visited, stack)
      }
    }
    // Push current vertex to stack which stores result
    stack.push(new Integer(v))
  }

  // The function to do Topological Sort. It uses
  // recursive topologicalSortUtil()
  def topologicalSort() : Stack[Integer] = {
    val stack: Stack[Integer] = new Stack[Integer]()
    // Mark all the vertices as not visited
    val visited: Array[Boolean] = new Array[Boolean](V)
    var i = 0
    while (i < V) {
      visited(i) = false
      i += 1
    }
    // Call the recursive helper function to store
    // Topological Sort starting from all vertices
    // one by one
    i = 0
    while (i < V) {
      if (!visited(i)) topologicalSortUtil(i, visited, stack)
      i += 1
    }
    stack
  }
}