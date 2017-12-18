package org.change.v2.p4.model;

import java.util.List;
import java.util.Map;

public interface ISwitchInstance {
    Map<Integer, Integer> getCloneSpec2EgressSpec();

    Map<Integer, String> getIfaceSpec();

    String getName();
}
