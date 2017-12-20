package org.change.v2.verification

import java.io.{File, PrintWriter}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.v2.abstractnet.optimized.macswitch.OptimizedSwitch
import org.change.v2.abstractnet.optimized.router.OptimizedRouter

import scala.io
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward, If, InstructionBlock}
import org.change.v2.executor.clickabstractnetwork.AggregatedBuilder.executorFromFolder
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.verification.Tester.Topo

/**
  * Created by mateipopovici on 14/12/17.
  */
object Printer {


  def header =
    """
      |<!doctype html>
      |<html>
      |<head>
      |    <title>Network | Interaction events</title>
      |
      |    <script src="http://visjs.org/dist/vis.js"></script>
      |  <link href="http://visjs.org/dist/vis.css" rel="stylesheet" type="text/css" />
      |
      |    <style type="text/css">
      |        #mynetwork {
      |            width: 1200px;
      |            height: 900px;
      |            border: 1px solid lightgray;
      |        }
      |    </style>
      |</head>
      |<body>
      |
      |<p>Use VisJS to diagram the Control-Flow-Graph (CFG) of a function from
      |a program you wish to analyze.</p>
      |<p><div id="mynetwork"></div><br /></p>
      |
      |<script type="text/javascript">
      |
      |  var options = {
      |  manipulation: false,
      |  height: '150%',
      |  layout: {
      |    hierarchical: {
      |      enabled: true,
      |      levelSeparation: 300
      |    }
      |  },
      |  physics: {
      |    hierarchicalRepulsion: {
      |      nodeDistance: 300
      |    }
      |  }
      |};
      |
      |var nodes = [
    """.stripMargin

  def middle =
    """
      |];
      |
      |
      |//
      |// Note: there are a couple of node id's present here which do not exist
      |// - cfg_0x00417563
      |// - cfg_0x00403489
      |// - cfg_0x0042f03f
      |//
      |// The edges with these id's will not load into the Network instance.
      |//
      |var edges = [
    """.stripMargin

  def footer =
    """
      |];
      |
      |
      |
      |
      |
      |
      |var container = document.getElementById('mynetwork');
      |var data = {'nodes': nodes, 'edges': edges}
      |var gph = new vis.Network(container, data, options);
      |
      |</script>
      |
      |
      |</body>
      |</html>
      |
    """.stripMargin


  def seek (port : LocationId, i : Instruction): List[(LocationId,LocationId)] = {
    i match {
      case InstructionBlock(l) => l.map((instr)=>seek(port,instr)).fold(Nil)((x,y)=>x++y)
      case Fork(l) => l.map((instr)=>seek(port,instr)).fold(Nil)((x,y)=>x++y)
      case If(_,th,el) => seek(port,th)++seek(port,el)
      case Forward(str) => List((port,str))
      case _ => Nil
    }
  }
//Policy.show(i)
  def getNode (port:LocationId, i:Instruction) : String = {
    var code = Policy.show(i)
    /*
    if (code.length >= 5000)
      code = code.substring(0,5000)
    */
    "{'id': '"+port+"', 'size': 150, 'label': \"Port:"+port+"\\n"+code+"\", 'color': \"#FFCFCF\", 'shape': 'box', 'font': {'face': 'monospace', 'align': 'left'}}"
  }

  def getNode (port:LocationId) : String = {
    "{'id': '"+port+"', 'size': 150, 'label': '\"Port:"+port+"\\n\"', 'color': \"#0000FF\", 'shape': 'box', 'font': {'face': 'monospace', 'align': 'left'}}"
  }

  def getEdge (in:LocationId, out:LocationId) : String = {
    "{'from': \""+in+"\", 'to': \""+out+"\", 'arrows': 'to', 'physics': false, 'smooth': {'type': 'cubicBezier'}}"
  }
  /*

  i match {
      case InstructionBlock(Nil) => "{;}"
      case Fork(Nil) => "{||}"
      case InstructionBlock(l) => aux(" ; ", l, indent)
      case Fork(l) => aux(" || ", l, indent)
      case ConstrainRaw(a,b,c) => a.toString+b.toString
      case ConstrainNamedSymbol(a,b,c) => a.toString+b.toString
      case AssignRaw(a,b,c) => a.toString+"="+b.toString
      case If(test,th,el) => "if ("+test.toString+") then {"+show(th,indent+1)+"} else {"+show(el,indent+1)+"}"
      case Forward(p)=> "Forward("+p.toString+")"
      case SEFLFail(_) => "Fail"
      case AllocateSymbol(s, sz) => "allocate("+s+")"
      case AllocateRaw(s,sz) => "allocate_raw("+s+")"
      case DeallocateRaw(s,_) => "deallocate("+s+")"
      case DeallocateNamedSymbol(s) => "deallocate_n("+s+")"
      case CreateTag(s,v) => "createTag("+s+")"
      case DestroyTag(s) => "destroyTag("+s+")"
      case AssignNamedSymbol(a,b,c) => a.toString+"="+b.toString
      case NoOp => "NoOp"
      }} + "\n"
   */


  // a Viz Node
  //{'id': 'cfg_0x00405a2e', 'size': 150, 'label': "0x00405a2e:\nmov    DWORD PTR ss:[esp + 0x000000b0], 0x00000002\nmov    DWORD PTR ss:[ebp + 0x00], esi\ntest   bl, 0x02\nje     0x00405a49<<Insn>>\n", 'color': "#FFCFCF", 'shape': 'box', 'font': {'face': 'monospace', 'align': 'left'}},

  // a Viz link
  // {'from': "cfg_0x00405a2e", 'to': "cfg_0x00405a39", 'arrows': 'to', 'physics': false, 'smooth': {'type': 'cubicBezier'}},



  def vizPrinter (t : Topo, output: String)  = {


    var links = Set[(String,String)]()
    t._2 foreach {case (link1,link2) => links += ((link1,link2):(String,String)) }

    var code : Map[String,Instruction] = t._1
    code foreach{ case (port:String,i:Instruction)  => links = links ++ seek(port,i) }

    /*
    var strNodes = ""
    code foreach{ case (port:String,i:Instruction) => strNodes += getNode(port,i)+",\n"}
    t._2 foreach {case (link,_) => strNodes += getNode(link)+",\n"}
    strNodes=strNodes.substring(0,strNodes.length()-2)

    var strLinks = ""
    links foreach {case (link1,link2) => strLinks += getEdge(link1,link2)+",\n"}
    strLinks=strLinks.substring(0,strLinks.length()-2)

*/
//    println(strNodes)

//    println(strLinks)


    val writer = new PrintWriter(new File(output))

    writer.write(header)

    var strNodes = ""
    code foreach{ case (port:String,i:Instruction) => writer.write(getNode(port,i)+",\n") }
    t._2 foreach {case (link,_) => writer.write(getNode(link)+",\n") }
    //strNodes=strNodes.substring(0,strNodes.length()-2)

    writer.write(middle)

    var strLinks = ""
    links foreach {case (link1,link2) => writer.write(getEdge(link1,link2)+",\n")}
    //strLinks=strLinks.substring(0,strLinks.length()-2)

    writer.write(footer)

    writer.close()

    //code.map((port:LocationId,i:Instruction) => seek(port,i))



    //print(links)
  }

  def main(args: Array[String]): Unit = {

    val dataPlaneFolder = "src/main/resources/asa"
    var exe = executorFromFolder(new File(dataPlaneFolder), Map(
      "switch" -> OptimizedSwitch.trivialSwitchNetworkConfig _,
      "click" -> {f => ClickToAbstractNetwork.buildConfig(f, prefixedElements = true)},
      "router" -> OptimizedRouter.trivialRouterNetwrokConfig _
    )).setLogger(JsonLogger)

    vizPrinter((exe.instructions,exe.links),"ast-port.html")

  }

}
