package org.change.v2.analysis.memory

import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames._
import org.change.v2.util.conversion.RepresentationConversion._

import spray.json._
import org.change.v2.analysis.memory.jsonformatters.StateToJson._

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
case class State(memory: MemorySpace = MemorySpace.clean,
                 history: List[LocationId] = Nil,
                 errorCause: Option[ErrorCause] = None,
                 instructionHistory: List[Instruction] = Nil,
                 savedStack: Map[String, MemorySpace] = Map[String, MemorySpace]()) {

  def save(location: String): State = {
    val newstack = savedStack + (location -> memory)
    this.copy(savedStack = newstack)
  }

  def load(location: String): Option[State] = {
    savedStack.get(location).map(s => this.copy(memory = s))
  }

  def destroyPacket(): State = {
    copy(memory = memory.destroyPacket())
  }

  def location: LocationId = history.head
  def forwardTo(locationId: LocationId): State = State(memory, locationId :: history, errorCause, instructionHistory)
  def status = errorCause.getOrElse("OK")
  def addInstructionToHistory(i: Instruction) = State(memory, history, errorCause, i :: instructionHistory)

  def jsonString = {
    this.toJson.prettyPrint
  }
  override def toString = jsonString
}

object State {

  def clean = State(MemorySpace.clean)

  val tcpOptions = InstructionBlock(
    Allocate("OPT1"),
    Allocate("SIZE1"),
    Assign("OPT1",ConstantValue(1)),
    Assign("SIZE1",ConstantValue(1)),

    Allocate("OPT2"),
    Allocate("SIZE2"),
    Allocate("VAL2"),
    //      Assign("OPT2",ConstantValue(1)),
    Assign("OPT2",SymbolicValue()),
    Assign("SIZE2",ConstantValue(4)),
    //      Assign("SIZE2",SymbolicValue()),
    Assign("VAL2",SymbolicValue()),

    Allocate("OPT3"),
    Allocate("SIZE3"),
    Allocate("VAL3"),
    //      Assign("OPT3",ConstantValue(1)),
    Assign("OPT3",SymbolicValue()),
    Assign("SIZE3",ConstantValue(3)),
    //      Assign("SIZE3",SymbolicValue()),
    Assign("VAL3",ConstantValue(7)),

    Allocate("OPT5"),
    Allocate("SIZE5"),
    //      Assign("OPT5",ConstantValue(1)),
    Assign("OPT5",SymbolicValue()),
    Assign("SIZE5",ConstantValue(2)),
    //      Assign("SIZE5",SymbolicValue()),

    Allocate("OPT8"),
    Allocate("VAL8"),
    Allocate("SIZE8"),
    //      Assign("OPT8",ConstantValue(1)),
    Assign("OPT8",SymbolicValue()),
    Assign("SIZE8",ConstantValue(10)),
    //      Assign("SIZE8",SymbolicValue()),
    Assign("VAL8",SymbolicValue()),

    Allocate("OPT30"),
    Allocate("VAL30"),
    Allocate("SIZE30"),
    //      Assign("OPT30",ConstantValue(1)),
    Assign("OPT30",SymbolicValue()),
    Assign("SIZE30",ConstantValue(20)),
    Assign("VAL30",SymbolicValue())
  )

 val start = CreateTag("START",0)
 val eher = InstructionBlock(
   CreateTag("L2",Tag("START")-112),
   Allocate(Tag("L2")+EtherSrcOffset,48),
   Assign(Tag("L2")+EtherSrcOffset,SymbolicValue()),
   Allocate(Tag("L2")+EtherDstOffset,48),
   Assign(Tag("L2")+EtherDstOffset,SymbolicValue()),
   Allocate(Tag("L2")+EtherTypeOffset,16),
   Assign(Tag("L2")+EtherTypeOffset,ConstantValue(EtherProtoIP))
 )
 val ehervlan = InstructionBlock(
    CreateTag("L2",Tag("START")-144),
    Allocate(Tag("L2")+EtherSrcOffset,48),
    Assign(Tag("L2")+EtherSrcOffset,SymbolicValue()),
    Allocate(Tag("L2")+EtherDstOffset,48),
    Assign(Tag("L2")+EtherDstOffset,SymbolicValue()),
//    Assign(Tag("L2")+EtherDstOffset,ConstantValue(macToNumberCiscoFormat("0023.ebbb.f14d"))),
    Allocate(Tag("L2")+EtherTypeOffset,16),
    Assign(Tag("L2")+EtherTypeOffset,ConstantValue(EtherProtoVLAN)),
    Allocate(PCP,3),
    Assign(PCP,ConstantValue(0)),
    Allocate(DEI,1),
    Assign(DEI,ConstantValue(0)),
    Allocate(VLANTag,12),
    Assign(VLANTag,SymbolicValue()),
//    Assign(VLANTag,ConstantValue(301)),
   Allocate(Tag("L2")+EtherTypeOffset + 32,16),
   Assign(Tag("L2")+EtherTypeOffset + 32,ConstantValue(EtherProtoIP))
  )

   val ip = InstructionBlock(

    CreateTag("L3", StartTag + 0),

   Allocate(IPVersion, 4),
   Assign(IPVersion, SymbolicValue()),

   Allocate(Proto, 8),
   Assign(Proto, SymbolicValue()),

   Allocate(IPSrc, 32),
   Constrain(IPSrc, :>=:(ConstantValue(0))),
   Assign(IPSrc, SymbolicValue()),

   Allocate(IPDst, 32),
   Constrain(IPDst, :>=:(ConstantValue(0))),

   Allocate(TTL, 8),
   Assign(TTL, ConstantValue(255)),

   Allocate(IPLength, 16),
   Assign(IPLength, SymbolicValue()),

   Allocate(IPHeaderLength, 4),
   Assign(IPHeaderLength, SymbolicValue()),

   Allocate(HeaderChecksum,16),
   Assign(HeaderChecksum, SymbolicValue()),

   Allocate(IPID, 16),
   Assign(IPID, SymbolicValue())
 )

  def ipSymb(ttlSymbolic : Boolean) : Instruction = InstructionBlock(
    CreateTag("L3", StartTag + 0),
    Allocate(IPVersion, 4),
    Assign(IPVersion, SymbolicValue()),
    Allocate(Proto, 8),
    Assign(Proto, SymbolicValue()),
    Allocate(IPSrc, 32),
    Assign(IPSrc, SymbolicValue()),
    Allocate(IPDst, 32),
    Assign(IPDst, SymbolicValue()),
    Allocate(TTL, 8),
    Assign(TTL, if (!ttlSymbolic)
      ConstantValue(255)
    else SymbolicValue("ttl")),
    Allocate(IPLength, 16),
    Assign(IPLength, SymbolicValue()),

    Allocate(IPHeaderLength, 4),
    Assign(IPHeaderLength, SymbolicValue()),

    Allocate(HeaderChecksum,16),
    Assign(HeaderChecksum, SymbolicValue()),

    Allocate(IPID, 16),
    Assign(IPID, SymbolicValue())
  )
  def ipSymb(): Instruction = ipSymb(false)
   val transport = InstructionBlock(
    CreateTag("L4", L3Tag + 160),

   Assign(Proto, ConstantValue(TCPProto)),

   Allocate(TcpSrc, 16),
//   Assign(TcpSrc, SymbolicValue()),
   Assign(TcpSrc, ConstantValue(80)),
//   Constrain(TcpSrc, :&:(:>=:(ConstantValue(1000)), :<=:(ConstantValue(65536)))),

   Allocate(TcpDst, 16),
//   Assign(TcpDst, ConstantValue(80)),
   Assign(TcpDst, ConstantValue(60000)),
//   Assign(TcpDst, SymbolicValue()),
//   Constrain(TcpDst, :&:(:>=:(ConstantValue(0)), :<=:(ConstantValue(65536)))),

   Allocate(TcpSeq, 32),
   Assign(TcpSeq, SymbolicValue()),

   Allocate(TcpAck, 32),
   Assign(TcpAck, SymbolicValue()),

   Allocate(TcpDataOffset, 4),
   Assign(TcpDataOffset, ConstantValue(160)),

   Allocate(TcpReserved,3),
   Assign(TcpReserved,SymbolicValue()),

    Allocate(TcpFlagNS, 1),
    Assign(TcpFlagNS, ConstantValue(0)),
    Allocate(TcpFlagCWR, 1),
    Assign(TcpFlagCWR, ConstantValue(0)),
    Allocate(TcpFlagECE, 1),
    Assign(TcpFlagECE, ConstantValue(0)),
    Allocate(TcpFlagURG, 1),
    Assign(TcpFlagURG, ConstantValue(0)),
    Allocate(TcpFlagACK, 1),
    Assign(TcpFlagACK, SymbolicValue()),
    Allocate(TcpFlagACK, 1),
    Assign(TcpFlagACK, SymbolicValue()),
    Allocate(TcpFlagSYN, 1),
    Assign(TcpFlagSYN, SymbolicValue()),
    Allocate(TcpFlagRST, 1),
    Assign(TcpFlagRST, ConstantValue(0)),
    Allocate(TcpFlagPSH, 1),
    Assign(TcpFlagPSH, ConstantValue(0))
  )
   val end = CreateTag("END", L4Tag + 12000)
  val tunnel = InstructionBlock(
    CreateTag("Tunnel", Tag("L2") - 96),
    Allocate(Tag("Tunnel"), 32)
  )

 def allSymbolic = InstructionBlock(
  start,
  ehervlan,
  ipSymb,
  transport,
  end,
  tcpOptions
 )(State.clean, true)._1.head
  def bigBang() : State = bigBang(true)

 def bigBang(verbose : Boolean): State = {
   val afterBigBang = InstructionBlock (
     start,
     ip,
     transport,
     //CreateTag("PAYLOAD", :+:(L4Tag,:@(TcpDataOffset)),
     //Allocate(Tag("PAYLOAD"),12000),
     //Assign(Tag("PAYLOAD"),SymbolicValue()),
     end
   )(State(MemorySpace.clean), verbose)

   afterBigBang._1.head
 }
}
