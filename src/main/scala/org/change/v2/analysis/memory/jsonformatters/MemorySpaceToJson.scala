package org.change.v2.analysis.memory.jsonformatters

import org.change.v2.analysis.memory.MemorySpace
import spray.json._

/**
  * A small gift from radu to symnetic.
  */
object MemorySpaceToJson extends DefaultJsonProtocol {

  implicit object MemoryStateFormat extends RootJsonFormat[MemorySpace] {
    override def read(json: JsValue): MemorySpace = deserializationError("One way protocol")

    import org.change.v2.analysis.memory.jsonformatters.MemoryObjectToJson._

    override def write(obj: MemorySpace): JsValue = JsObject(
      "tags" -> JsObject(obj.memTags.map(tv => tv._1 -> JsNumber(tv._2))),
      "meta_symbols" -> JsObject(obj.symbols.map(tv => tv._1 ->  tv._2.toJson)),
      "header_fields" -> JsObject(obj.rawObjects.map(tv => tv._1.toString -> tv._2.toJson))
    )
  }

}

object ConcreteMemorySpaceToJson extends DefaultJsonProtocol {

  implicit object MemoryStateFormat extends RootJsonFormat[MemorySpace] {
    override def read(json: JsValue): MemorySpace = deserializationError("One way protocol")

    override def write(obj: MemorySpace): JsValue = JsArray(
      obj.rawObjects.map(tv =>
        JsObject(tv._1.toString -> JsObject(
          "initial" -> JsNumber(obj.exampleFor(tv._2.initialValue.get).getOrElse(-1)),
          "final" -> JsNumber(obj.exampleFor(tv._2.initialValue.get).getOrElse(-1))
        ))).toVector
    )
  }

}
