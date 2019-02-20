package org.change.v3.semantics

import z3.scala.Z3AST

case class SimpleMemoryObject(size : Int, ast : Option[Z3AST])
case class SimpleMemory(
                         pathCondition : Z3AST,
                         errorCause : Option[String] = None,
                         history : List[String] = Nil,
                         memTags : Map[String, Int] = Map.empty,
                         rawObjects : Map[Int, SimpleMemoryObject] = Map.empty,
                         symbols : Map[String, SimpleMemoryObject] = Map.empty
                       ) {}
