package org.change.utils

object RepresentationConversion {
  def unpack(bytes : Int) = {
    List[Byte] (
      ((bytes >>> 24) & 0xff).asInstanceOf[Byte],
      ((bytes >>> 16) & 0xff).asInstanceOf[Byte],
      ((bytes >>>  8) & 0xff).asInstanceOf[Byte],
      ((bytes       ) & 0xff).asInstanceOf[Byte]
    )
  }
  def numberToIp(ip : Long) = {
    var startStr = ""
    val start = unpack(ip.asInstanceOf[Int])
    for (b <- start) {
      startStr += ((b & 0xFF).asInstanceOf[Int]).toString + "."
    }
    startStr = startStr.substring(0, startStr.length() - 1)
    startStr
  }
  
  def ipToNumber(ip: String): Long = {
    ip.split("\\.").map(Integer.parseInt(_)).foldLeft(0L)((a:Long, g:Int)=> a * 256 + g)
  }

  def macToNumber(mac: String): Long = {
    mac.toLowerCase.split(":").map(Integer.parseInt(_, 16)).foldLeft(0L)((a:Long, g:Int)=> a * 256 + g)
  }

  def macToNumberCiscoFormat(mac: String): Long = {
    mac.toLowerCase.split("\\.").map(Integer.parseInt(_, 16)).foldLeft(0L)((a:Long, g:Int)=> a * 65536 + g)
  }

  def ipAndMaskToInterval(ip: String, mask: String): (Long, Long) = {
    val ipv = ipToNumber(ip)
    val maskv = Integer.parseInt(mask)
    val addrS = 32 - maskv
    val lowerM = Long.MaxValue << addrS
    val higherM = Long.MaxValue >>> (maskv + 31)
    (ipv & lowerM, ipv | higherM)
  }

  def numberToIP(a: Long): String = {
    var s = (a % 256).toString
    var aRest = a >> 8
    for ( _ <- 0 until 3) {
      s = (aRest % 256) + "." + s
      aRest = aRest >> 8
    }
    s
  }
}
