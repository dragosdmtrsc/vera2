package org.change.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class ConstantExpression extends Expression {
  private long value;

  public ConstantExpression(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value + "";
  }
}
