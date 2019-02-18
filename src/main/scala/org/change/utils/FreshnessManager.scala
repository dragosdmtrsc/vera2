package org.change.utils

object FreshnessManager {
  var m  = Map.empty[String, Int]
  def next(x : String = "tmp"): String = {
    if (!m.contains(x)) {
      m = m + (x -> 1)
      s"${x}0"
    } else {
      val last = m(x)
      m = m + (x -> (last + 1))
      s"$x$last"
    }
  }
}
