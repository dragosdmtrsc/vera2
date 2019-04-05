package org.change.p4.model;

/**
 * Created by dragos on 01.09.2017.
 */
public class ArrayInstance extends HeaderInstance {
  private int length;

  public ArrayInstance(Header layout, String name, int length) {
    super(layout, name);
    this.length = length;
  }

  public ArrayInstance(String layout, String name, int length) {
    super(layout, name);
    this.length = length;
  }

  public int getLength() {
    return length;
  }
}
