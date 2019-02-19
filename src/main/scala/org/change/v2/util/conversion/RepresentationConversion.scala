package org.change.v2.util.conversion

object RepresentationConversion {

  def ipToNumber(ip: String): Long = {
    ip.split("\\.").map(Integer.parseInt(_)).foldLeft(0L)((a: Long, g: Int) => a * 256 + g)
  }

  def ciscoMac(mac : String) : Option[Long] = {
    val rex = "([0-9a-fA-F]+)\\.([0-9a-fA-F]+)\\.([0-9a-fA-F]+)".r
    rex.findFirstMatchIn(mac).map(u => {
      val nr1 = java.lang.Long.parseLong(u.group(1), 16)
      val nr2 = java.lang.Long.parseLong(u.group(2), 16)
      val nr3 = java.lang.Long.parseLong(u.group(2), 16)
      (nr1 << 32) | (nr2 << 16) | nr3
    })
  }

  def regularMac(mac : String) : Option[Long] = {
    val rex = "([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+)".r
    rex.findFirstMatchIn(mac).map(u => {
      (0 until u.groupCount).foldLeft(0L)((acc, i) => {
        val g = u.group(i + 1)
        (acc << 8) | java.lang.Integer.parseInt(g, 16)
      })
    })
  }

  def macToNumber(mac: String): Long = {
    regularMac(mac).getOrElse(ciscoMac(mac).get)
  }

  /**
    * TODO: Check what is the Cisco format.
    *
    * @param mac
    * @return
    */
  def macToNumberCiscoFormat(mac: String): Long = {
    mac.toLowerCase.split("\\.").map(Integer.parseInt(_, 16)).foldLeft(0L)((a: Long, g: Int) => a * 65536 + g)
  }

  /**
    * Get yourself into CIDR mode
    */
  def ipAndMaskToInterval(masked: String): (Long, Long) = {
    val split = masked.split("/")
    ipAndMaskToInterval(split(0), Integer.parseInt(split(1)))
  }

  def ipAndMaskToInterval(ip: Long, mask: Int): (Long, Long) = {
    val ipv = ip
    val maskv = mask
    val addrS = 32 - maskv
    val lowerM = Long.MaxValue << addrS
    val higherM = Long.MaxValue >>> (maskv + 31)
    (ipv & lowerM, ipv | higherM)
  }

  def ipAndMaskToInterval(ip: String, mask: Int): (Long, Long) = {
    ipAndMaskToInterval(ipToNumber(ip), mask)
  }

  def isIp(ip : String): Boolean = "[0-9][0-9]?[0-9]?(\\.[0-9][0-9]?[0-9]?){3}".r.findFirstMatchIn(ip).nonEmpty

  def ipAndMaskToInterval(ip: String, mask: String): (Long, Long) = {
    ipAndMaskToInterval(ipToNumber(ip), Integer.parseInt(mask))
  }

  def ipAndExplicitMaskToInterval(ip: String, mask: String): (Long, Long) = {
    val ipv = ipToNumber(ip)
    val maskv = ipToNumber(mask)
    (ipv & maskv, (((ipv | (~maskv)) << 32) >>> 32))
  }

  def numberToIP(a: Long): String = {
    var s = (a % 256).toString
    var aRest = a >> 8
    for (_ <- 0 until 3) {
      s = (aRest % 256) + "." + s
      aRest = aRest >> 8
    }
    s
  }

  def unpack(bytes: Int) = {
    List[Byte](
      ((bytes >>> 24) & 0xff).asInstanceOf[Byte],
      ((bytes >>> 16) & 0xff).asInstanceOf[Byte],
      ((bytes >>> 8) & 0xff).asInstanceOf[Byte],
      ((bytes) & 0xff).asInstanceOf[Byte]
    )
  }

  def numberToMac(mac: Long) = {
    var startStr = ""
    var start = mac
    for (i <- 0 until 6) {
      val crt = start & 0xff
      start = start >> 8
      startStr = crt.toHexString + (if (i > 0) ":" else "") + startStr
    }
    startStr
  }


  def numberToIp(ip: Long) = {
    var startStr = ""
    val start = unpack(ip.asInstanceOf[Int])
    for (b <- start) {
      startStr += ((b & 0xFF).asInstanceOf[Int]).toString + "."
    }
    startStr = startStr.substring(0, startStr.length() - 1)
    startStr
  }
}
