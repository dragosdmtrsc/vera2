package org.change.utils.prettifier

import org.change.v2.analysis.constraint.AND
import org.change.v2.analysis.constraint.Constraint
import org.change.v2.analysis.constraint.E
import org.change.v2.analysis.constraint.EQ_E
import org.change.v2.analysis.constraint.GT
import org.change.v2.analysis.constraint.GTE
import org.change.v2.analysis.constraint.GTE_E
import org.change.v2.analysis.constraint.GT_E
import org.change.v2.analysis.constraint.LT
import org.change.v2.analysis.constraint.LTE
import org.change.v2.analysis.constraint.LTE_E
import org.change.v2.analysis.constraint.LT_E
import org.change.v2.analysis.constraint.NOT
import org.change.v2.analysis.constraint.OR
import org.change.v2.analysis.constraint.Range
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.expression.concrete.nonprimitive.Minus
import org.change.v2.analysis.expression.concrete.nonprimitive.Plus
import org.change.v2.analysis.expression.concrete.nonprimitive.Reference
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.memory.Value
import org.change.v2.analysis.memory.ValueStack
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.types.NumericType
import org.change.v2.analysis.types._



class StatePrettifier {
  
  def prettifyType(eType : NumericType) = {
    eType match {
      case LongType =>
        "Long"
      case IP4Type =>
        "IPv4"
      case MACType => 
        "MACAddress"
      case ProtoType =>
        "ProtocolType"
      case VLANType =>
        "VLANId"
      case PortType =>
        "PortType"
      case _ => ""
    }
  }
  
  def prettifyConstraint(ct : Constraint) : String = {
    ""
  }
  
  
  def prettifyExpression(expr : Expression) : String = {
    ""
  }
  
  def prettifyValue(value : Value) = {
    val cts = value.cts
    val exp = value.e
    val eType = value.eType
    val eTypeString = prettifyType(eType)
    val expString = prettifyExpression(exp)
    val ctsString = cts.map (prettifyConstraint).foldLeft("")( (acc, s) => {
      "(assert ($ctsString $expString))"
    })
    
  }
  
  
  def prettifyRawObjects(symbols : Map[String, MemoryObject],
      rawObjects : Map[Int, MemoryObject],
      memTags : Map[String, Int]) : String = {
    rawObjects.foldLeft("")((acc, b) => {
      val startPos = b._1
      val theValue = b._2.value
      val name = {
          val theTag = memTags.find((r) => r._2 == startPos)
          if (theTag.isEmpty)
          {
            ""
          }
          else
          {
            theTag.get._1
          }
        }
      val actual = name + ": [" + startPos + "," + (b._2.size + startPos) + "] = {" +
      (
        if (theValue.isEmpty)
          ""
        else {
         
          
          theValue.map { s => "Expression = " + prettify(s.e) + "\n" + 
            "Type = " + s.eType.getClass.getSimpleName + "\n" + 
            "In Set " + s.cts.foldLeft("")((acc, r) => {
                r.asSet(s.eType).foldLeft("")((setString, u) =>
                  {
                    val sep = if (setString != "") " U " else ""
                    setString + sep + "[" + u._1 + "," + u._2 + "]"
                  })
            }) + "\n}"
          }
        }
      )
      val sep = (
      if (acc == "")
        ""
      else
        "\n"
      )
      acc + sep + actual
    })
  }
  
  
  def prettify (v : Any, options : Option[Map[String, Any]] = None) : String = {
    val current = options.getOrElse(Map[String, Any]( "level" -> 0 ))
    val level = current.getOrElse("level", 0).asInstanceOf[Int]
    val already = current + ("level" -> (level + 1))
    val oAlready = Option(already)
    already.getOrElse("already", Map[String, Any]())
    val str = v match {
      case v: String =>
        v
      case Some(value) =>
        prettify(value, oAlready)
      case None =>
        ""
      case State(memory, history, errorCause,instructionHistory) =>
          "memory : {\n" + prettify(memory, oAlready) + "\n},"
//          "history : {\n" + prettify(history, oAlready) + "\n}," +
//          "error : {\n" + prettify(errorCause, oAlready) + "\n}," 
//          "Instruction history\n" + prettify(instructionHistory)
      case MemorySpace(symbols, rawObjects, memTags) =>
//          "Symbols : {\n" + prettify(symbols, oAlready) + "\n}," +
          "Tags : {\n" + prettifyRawObjects(symbols, rawObjects, memTags) + "\n},"
      case MemoryObject(valueStack, size) =>
          "size : " + size + "," +
          "vstack : " + prettify(valueStack, oAlready)
      case ValueStack(head :: tail) =>
          prettify(head :: tail, oAlready) + ","
      case ValueStack(Nil) =>
          ""
      case Value(expression, kind, constraints) =>
          "expression : {\n" + prettify(expression, oAlready) + "\n}," +
          "kind : {" + kind.getClass.getSimpleName + "\n}\n" +
          "constraints : \n" + prettify(constraints, oAlready) + "\n,"
      case head :: tail => {
        head match  {
          case _ => {
            val lst = head :: tail
            "[" + lst.zipWithIndex.map { s => prettify(s._1, oAlready) }.fold("")((s, acc) => {
            acc + ",{" + s + "}"
            }) + "]"
          }
        }
      }
      case Nil =>
        ""
      case AND(head :: tail) =>
        prettify(head) + " AND " +
        prettify(AND(tail))
      case OR(head :: tail) =>
        prettify(head) + " OR " +
        prettify(OR(tail))
      case E(long) =>
        " = " + long
      case GT(long) =>
        " > " + long
      case LT(long) =>
        " < " + long
      case GTE (long) =>
        " >= " + long
      case LTE (long) =>
        " <= " + long
      case NOT(ct) =>
        " !(" + prettify(ct) + ")"
      case Range(start, end) =>
        prettify(AND(List(GTE(start), LTE(end))))
      case EQ_E(e) =>
        " = " + prettify(e)
      case GT_E(e) =>
        " > " + prettify(e)
      case LT_E(e) =>
        " < " + prettify(e)
      case LTE_E(e) =>
        " <= " + prettify(e)
      case GTE_E(e) =>
        " >= " + prettify(e)
      case Minus(left, right) =>
        prettify(left, oAlready) + " - " + prettify(right, Option(already + ("ignore" -> true)))
      case Plus(left, right) =>
        prettify(left, oAlready) + " + " + prettify(right, Option(already + ("ignore" -> true)))
      case Reference(value) =>
        prettify(value)
      case v : ConstantValue =>
        v.toString
      case v : SymbolicValue =>
        v.id.toString        
      case v : Map[String, _] =>
        v.map(s =>  s._1 + ": {\n" + prettify(s._2, oAlready) + "\n}").fold("")((s, acc) => {
            if (acc.equals("")) s else acc + ",\n" + s
          }).trim()
    }
    str
  }
}

import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import org.change.v2.analysis.types.IP4Type
import org.change.v2.analysis.types.LongType



object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerSubtypes(
            new NamedType(classOf[Minus], "Minus"),
            new NamedType(classOf[Plus], "Plus"),
            new NamedType(classOf[Reference], "Reference"),
            new NamedType(classOf[SymbolicValue], "SymbolicValue"),
            new NamedType(classOf[ConstantValue], "ConstantValue")
            )
  mapper.registerSubtypes(classOf[AND], classOf[OR],
      classOf[E], classOf[GT], classOf[GTE], classOf[LT],
      classOf[LTE], classOf[NOT], classOf[OR], classOf[Range],
      classOf[EQ_E], classOf[GT_E], classOf[GTE_E], classOf[LT_E],
      classOf[LTE_E], 
      LongType.getClass, IP4Type.getClass, MACType.getClass,
      PortType.getClass, ProtoType.getClass, VLANType.getClass)
  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k,v) => k.name -> v})
  }

  def toJson(value: Any): String = {
    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
  }

//  def toMap[V](json:String)(implicit m: Manifest[V]) = fromJson[Map[String,V]](json)

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}


object SomeWriter {
  def apply(state : Any) =  {
    JsonUtil.toJson(state)
  }
}

object StatePrettifier {
  def apply(state : State, level : Int) = new StatePrettifier().prettify(state, Some(Map("level" -> 0)))
  def apply(state : State) = new StatePrettifier().prettify(state, Some(Map("level" -> 0)))
}