package org.change.p4.model.parser;

public class IndexedHeaderRef extends HeaderRef {
  private int index;

  @Override
  public boolean isArray() {
    return true;
  }

  public boolean isLast() {
    return index == -1;
  }

  public boolean isNext() {
    return index == -2;
  }

  public IndexedHeaderRef setLast() {
    index = -1;
    return this;
  }

  public int getIndex() {
    return index;
  }

  public IndexedHeaderRef setIndex(int index) {
    this.index = index;
    return this;
  }

  public IndexedHeaderRef() {
  }

  public IndexedHeaderRef(IndexedHeaderRef ihr) {
    super(ihr);
    this.index = ihr.index;
  }

  @Override
  public String toString() {
    if (isLast()) {
      return super.toString() + ".last";
    } else if (isNext()) {
      return super.toString() + ".next";
    } else {
      return super.toString() + "[" + index + "]";
    }
  }
}
