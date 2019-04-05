package org.change.p4.model;

import scala.math.BigInt;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dragos on 01.09.2017.
 */
public class HeaderInstance {
  private Header layout;
  private String name;
  private boolean metadata;

  private Map<String, BigInt> initializer = new HashMap<>();

  public HeaderInstance(Header layout, String name) {
    assert layout != null && name != null;
    this.layout = layout;
    this.name = name;
  }

  public HeaderInstance(String layout, String name) {
    assert layout != null && name != null;
    this.layout = new Header(layout);
    this.name = name;
  }

  public HeaderInstance setLayout(Header layout) {
    this.layout = layout;
    return this;
  }

  public HeaderInstance addInitializer(String field, BigInt value) {
    this.initializer.put(field, value);
    return this;
  }

  public Map<String, BigInt> getInitializer() {
    return initializer;
  }

  public Header getLayout() {
    return layout;
  }

  public String getName() {
    return name;
  }

  public boolean isMetadata() {
    return this.metadata;
  }

  public HeaderInstance setMetadata(boolean tf) {
    this.metadata = tf;
    return this;
  }

  @Override
  public String toString() {
    return getName() + " : " + getLayout().getName();
  }
}
