package org.change.p4.control

trait P4EventListener {
  // what refers to a CFG object -
  // ControlStatement, P4Action with arguments etc.
  // state is a query which stands for the current state
  // on is executed when what is converged
  def on(what: Object, state: P4Query): Unit

  // what refers to a transition-like event (e.g. edge in CFG,
  // control node in P4Actions), state is the current state
  // which will get transformed into a new query
  // referenceTransform is the reference transformation
  // as per P4 semantics
  // TODO: please re-think - assign extra semantics to a transition
//  def transform(what : Object,
//                state : P4Value,
//                referenceTransform : Option[P4Query]) : P4Query
}
