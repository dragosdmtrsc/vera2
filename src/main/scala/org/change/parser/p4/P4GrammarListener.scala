package org.change.parser.p4

import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}

/**
  * A small gift from radu to symnetic.
  */
class P4GrammarListener extends P4GrammarBaseListener {


  override def exitField_value(ctx: P4GrammarParser.Field_valueContext): Unit = {
    ctx.fieldValue = ctx.const_value().constValue
  }

  override def exitConst_value(ctx: P4GrammarParser.Const_valueContext): Unit = {
    //TODO: Support the width field
    ctx.constValue = (if (ctx.getText.startsWith("-")) -1 else 1) * ctx.unsigned_value().unsignedValue
  }

  override def exitUnsigned_value(ctx: P4GrammarParser.Unsigned_valueContext): Unit = {
    ctx.unsignedValue = {
        Option(ctx.decimal_value()).map(_.parsedValue) orElse
        Option(ctx.binary_value()).map(_.parsedValue) orElse
        Option(ctx.hexadecimal_value()).map(_.parsedValue)
    }.get
  }

  override def exitBinary_value(ctx: P4GrammarParser.Binary_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.binaryToInt(ctx.getText)
  }

  override def exitHexadecimal_value(ctx: P4GrammarParser.Hexadecimal_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.hexToInt(ctx.getText)
  }

  override def exitDecimal_value(ctx: P4GrammarParser.Decimal_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.decimalToInt(ctx.getText)
  }
}
