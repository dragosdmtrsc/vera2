package org.change.v2.p4.model.actions;

import java.util.ArrayList;
import java.util.List;

public class P4ActionProfile {
    private String name;
    private List<String> actions = new ArrayList<String>();
    private int size = 0;
    private String dynamicActionSelector;

    public P4ActionProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getActions() {
        return actions;
    }

    public int getSize() {
        return size;
    }

    public P4ActionProfile setSize(int size) {
        this.size = size;
        return this;
    }

    public String getDynamicActionSelector() {
        return dynamicActionSelector;
    }

    public P4ActionProfile setDynamicActionSelector(String dynamicActionSelector) {
        this.dynamicActionSelector = dynamicActionSelector;
        return this;
    }
}
