package org.change.v2.p4.model.updated.sefl_compiler

import org.change.v2.p4.model.updated.sefl_compiler.sefl_code.SEFLCode

trait SEFLCompiler[T] {

  def compile(source: T): SEFLCode

}
