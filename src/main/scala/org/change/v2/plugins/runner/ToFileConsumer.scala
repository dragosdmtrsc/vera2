package org.change.v2.plugins.runner

import java.io.{BufferedOutputStream, FileOutputStream}

import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.memory.SimpleMemory
import org.change.v2.plugins.eq.{ExecutorConsumer, PluginBuilder}

import scala.collection.mutable.ListBuffer

class ToFileConsumer(success : String, fail : String) extends ExecutorConsumer {

  private val mFail: ListBuffer[SimpleMemory] = scala.collection.mutable.ListBuffer.empty[SimpleMemory]
  private val mSuccess: ListBuffer[SimpleMemory] = scala.collection.mutable.ListBuffer.empty[SimpleMemory]

  override def apply(x: SimpleMemory): Unit = {
    if (x.errorCause.isEmpty)
      mSuccess.append(x)
    else
      mFail.append(x)
  }

  override def flush(): Unit = {
    val fout = new BufferedOutputStream(new FileOutputStream(success))
    JsonUtil.toJson(mSuccess, fout)
    fout.close()

    val fout2 = new BufferedOutputStream(new FileOutputStream(fail))
    JsonUtil.toJson(mFail, fout2)
    fout2.close()
  }
}

object ToFileConsumer {
  class Builder extends PluginBuilder[ToFileConsumer] {
    var success = "success.json"
    var fail = "fail.json"
    override def set(parm: String, value: String): PluginBuilder[ToFileConsumer] = parm match {
      case "success" => success = value; this
      case "fail" => fail = value; this
    }

    override def build(): ToFileConsumer = {
      new ToFileConsumer(success, fail)
    }
  }
}
