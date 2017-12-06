package org.change.v2.util

class StringIt extends Iterator[String] {
  var theNum = 0
  lazy val dict = {
    List.range('a', 'z') ++ List.range('A', 'Z')
  }

  def hasNext: Boolean = true

  def next: String = {
    var str = ""
    var now = theNum % dict.size
    var left = theNum / dict.size
    do {
      str = str + dict(now)
      now = left % dict.size
      left = left / dict.size
    } while (left > 0)
    theNum = theNum + 1
    str
  }
}