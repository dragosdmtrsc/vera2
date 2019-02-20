package org.change.v3.syntax

trait Intable
/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
case class Tag(name: String) extends Intable {
  def +(other: TagExp): TagExp = TagExp(this :: other.plusTags, other.minusTags , other.rest)
  def +(other: Int): TagExp = TagExp(List(this), Nil , other)
  def -(other: TagExp): TagExp = TagExp(other.plusTags, this :: other.minusTags , other.rest)
  def -(other: Int): TagExp = TagExp(List(this), Nil , -other)
  override def toString: String = name
}

case class TagExp(plusTags: List[Tag], minusTags: List[Tag], rest: Int) extends Intable {
  def +(other: TagExp): TagExp = TagExp(this.plusTags ++ other.plusTags, this.minusTags ++ other.minusTags , this.rest + other.rest)
  def +(other: Tag): TagExp = TagExp(other :: this.plusTags, this.minusTags, rest)
  def +(other: Int): TagExp = TagExp(plusTags, minusTags , other + rest)

  def -(other: TagExp): TagExp = TagExp(this.plusTags ++ other.minusTags, this.minusTags ++ other.plusTags , this.rest - other.rest)
  def -(other: Tag): TagExp = TagExp(this.plusTags, other :: this.minusTags, rest)
  def -(other: Int): TagExp = TagExp(plusTags, minusTags , rest-other)
  override def toString: String = plusTags.mkString("+") + (if (minusTags.nonEmpty) "-" + minusTags.mkString("-") else "") + TagExp.IntImprovements(rest)
}
case class IntImprovements(value : Int) extends Intable

object TagExp {
  val brokenTagExpErrorMessage = "Cannot resolve expression to an int address"
  implicit def apply(value : Int): IntImprovements = IntImprovements(value)
}