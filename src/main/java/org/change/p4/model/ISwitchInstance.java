package org.change.p4.model;

import java.util.Map;

public interface ISwitchInstance {
  Map<Integer, Integer> getCloneSpec2EgressSpec();

  Map<Integer, String> getIfaceSpec();

  String getName();
}
