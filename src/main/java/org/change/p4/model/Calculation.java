package org.change.p4.model;

public class Calculation {
  private String name;

  public Calculation(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Calculation setName(String name) {
    this.name = name;
    return this;
  }
}
