package org.change.p4.model.control.exp;

import org.change.p4.model.parser.HeaderRef;

public class ValidRef implements P4BExpr {
  private HeaderRef href;

  public ValidRef(HeaderRef href) {
    this.href = href;
  }

  public HeaderRef getHref() {
    return href;
  }

  @Override
  public String toString() {
    return "valid(" + href.getPath() + ")";
  }
}
