package org.change.v2.analysis.memory

case class Triple[T](success: List[T], failed: List[T], continue: List[T]) {
  def this() = this(Nil, Nil, Nil)
  def +(n: Triple[T]): Triple[T] =
    new Triple[T](success ++ n.success,
                  failed = failed ++ n.failed,
                  continue = continue ++ n.continue)
  def finals() = new Triple[T](success, failed, Nil)

  def map(fun: (T => T)): Triple[T] =
    copy(success = success.map(fun),
         failed = failed.map(fun),
         continue = continue.map(fun))
  def filter(fun : T => Boolean) : Triple[T] =
    copy(success = success.filter(fun), continue = continue.filter(fun), failed = failed.filter(fun))
  def all() : List[T] = flat() ++ continue
  def flat() : List[T] = success ++ failed
}

object Triple {
  def empty[T] : Triple[T] = new Triple[T]()
  def startFrom[T](a: T) =
    new Triple[T](success = Nil, failed = Nil, continue = a :: Nil)
  def fail[T](a : T) = new Triple[T](success = Nil, failed = a :: Nil, continue = Nil)
  def success[T](a : T) = new Triple[T](success = a :: Nil, failed = Nil, continue = Nil)
}