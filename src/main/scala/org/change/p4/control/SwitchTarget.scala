package org.change.parser.p4.control

case class SwitchTarget(inputPorts : Set[Int])
object SwitchTarget {
  def fromRegex(regex : String): SwitchTarget = {
    SwitchTarget((0 until 255).filter(p => {
      p.toString.matches(regex)
    }).toSet)
  }
  def fromEnumeration(it : Iterable[Int]) = SwitchTarget(it.toSet)
}
