package org.change.v2.abstractnet.linux.iptables.old

import org.change.v2.abstractnet.linux.iptables.old.SimpleMatch


case class TcpMatch() extends SimpleMatch
{
}

case class UdpMatch() extends SimpleMatch
{
}

case class IcmpMatch() extends SimpleMatch
{
}

case class MacMatch() extends SimpleMatch
{
}

case class Ipv4Match() extends SimpleMatch
{
}

case class Ipv6Match() extends SimpleMatch 
{
  
}

case class SrcAddrMatch() extends SimpleMatch
{
  
}

case class DstAddrMatch() extends SimpleMatch
{
  
}

case class ConnmarkMatch() extends SimpleMatch
{
  
}

case class Icmp6Match() extends SimpleMatch
{
  
}

case class ConntrackMatch() extends SimpleMatch
{
  
}

case class MarkMatch() extends SimpleMatch
{
  
}

case class CommentMatch() extends SimpleMatch
{
  
}

case class PhysdevMatch() extends SimpleMatch
{
  
}

case class SetMatch() extends SimpleMatch
{
  
}

case class StateMatch() extends SimpleMatch
{
  
}

case class ProtocolMatch() extends SimpleMatch
{
  
}

case class InIfaceMatch() extends SimpleMatch
{
  
}

case class FragmentMatch() extends SimpleMatch
{
  
}

case class CtMatch() extends SimpleMatch()
{
}


