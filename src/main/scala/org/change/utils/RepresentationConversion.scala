package org.change.utils

import java.net.{Inet6Address, InetAddress}

object RepresentationConversion {
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

  def ipToNumber(ip: String): Long = {
    ip.split("\\.")
      .map(Integer.parseInt)
      .foldLeft(0L)((a: Long, g: Int) => a * 256 + g)
  }

  def ip6ToNumber(ip: String): BigInt = {
    val ia = InetAddress.getByName(ip)
    val byteArr: Array[Byte] = ia.getAddress
    var ipNumber = BigInt(0)
    if (ia.isInstanceOf[Inet6Address]) {
      ipNumber = BigInt(1, byteArr)
    }
    ipNumber
  }

  def macToNumber(mac: String): Long = {
    mac.toLowerCase
      .split(":")
      .map(Integer.parseInt(_, 16))
      .foldLeft(0L)((a: Long, g: Int) => a * 256 + g)
  }

  def macToNumberCiscoFormat(mac: String): Long = {
    mac.toLowerCase
      .split("\\.")
      .map(Integer.parseInt(_, 16))
      .foldLeft(0L)((a: Long, g: Int) => a * 65536 + g)
  }

  def ipAndMaskToInterval(ip: String, mask: String): (Long, Long) = {
    val ipv = ipToNumber(ip)
    val maskv = Integer.parseInt(mask)
    val addrS = 32 - maskv
    val lowerM = Long.MaxValue << addrS
    val higherM = Long.MaxValue >>> (maskv + 31)
    (ipv & lowerM, ipv | higherM)
  }
}
