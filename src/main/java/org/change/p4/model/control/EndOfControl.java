package org.change.p4.model.control;

import org.change.p4.model.parser.Statement;

import java.util.Objects;

public class EndOfControl extends Statement implements ControlStatement {
  private String control;

  public EndOfControl(String control) {
    this.control = control;
  }

  public String getControl() {
    return control;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EndOfControl that = (EndOfControl) o;
    return Objects.equals(control, that.control);
  }

  @Override
  public int hashCode() {
    return Objects.hash(control);
  }

  @Override
  public String toString() {
    return "end(" + control + ")";
  }
}
