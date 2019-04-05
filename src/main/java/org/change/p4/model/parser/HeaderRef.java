package org.change.p4.model.parser;

import org.change.p4.model.HeaderInstance;

public class HeaderRef extends Ref {
  private HeaderInstance instance;

  public HeaderInstance getInstance() {
    return instance;
  }

  public HeaderRef setInstance(HeaderInstance instance) {
    this.instance = instance;
    this.path = instance.getName();
    return this;
  }

  public HeaderRef() {
  }

  public HeaderRef(HeaderRef h) {
    this.instance = h.instance;
    setPath(h.getPath());
  }

  @Override
  public HeaderRef setPath(String path) {
    super.setPath(path);
    return this;
  }

  public boolean isArray() {
    return false;
  }

  @Override
  public String toString() {
    return getPath();
  }
}
