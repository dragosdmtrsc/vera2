package org.change.v2.analysis.memory.jsonformatters

import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.memory.jsonformatters.ValueToJson._
import spray.json.{DefaultJsonProtocol, _}

/**
  * A small gift from radu to symnetic.
  */
object MemoryObjectToJson extends DefaultJsonProtocol {

  implicit object MemoryObjectFormat extends RootJsonFormat[MemoryObject] {
    override def read(json: JsValue): MemoryObject = deserializationError("One way protocol")

    override def write(obj: MemoryObject): JsValue = obj.value match {

      case Some(v) => v.toJson
      case _ => JsString("NoAllocation")
    }
  }

}
