package util

import org.change.v2.util.conversion.RepresentationConversion._
import org.scalatest.{FlatSpec, Matchers}


/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
class UtilTests extends FlatSpec with Matchers {

  "IP and mask to interval" should "produce the same result no matter what notation is used" in {
    val netAddr = "172.16.2.0"

    val explicitMask = "255.255.255.0"
    val shortNotation = "24"

    ipAndMaskToInterval(netAddr, shortNotation) should be(ipAndExplicitMaskToInterval(netAddr, explicitMask))
  }

}
