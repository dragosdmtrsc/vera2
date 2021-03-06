package org.change.p4.model.control.exp;

import org.change.p4.model.parser.Expression;
import scala.math.BigInt;

public class LiteralExpr extends Expression implements P4Expr {
  private BigInt value;
  private int width;

  public LiteralExpr(BigInt value, int width) {
    this.value = value;
    this.width = width;
  }

  public BigInt getValue() {
    return value;
  }

  public int getWidth() {
    return width;
  }

  public LiteralExpr setWidth(int width) {
    this.width = width;
    return this;
  }

  @Override
  public String toString() {
    return getWidth() + "w" + getValue();
  }
}
