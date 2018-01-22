package org.change.v2.p4.model.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class P4Node {
    private String name, p4, dataplane;
    private boolean isStar = false;
    private Map<Integer, String> ifaces = new HashMap<>();

    public boolean isStar() {
        return isStar;
    }

    public P4Node setStar(boolean star) {
        isStar = star;
        return this;
    }

    public Map<Integer, String> getIfaces() {
        return ifaces;
    }

    public String getName() {
        return name;
    }

    public P4Node setName(String name) {
        this.name = name;
        return this;
    }

    public String getP4() {
        return p4;
    }

    public P4Node setP4(String p4) {
        this.p4 = p4;
        return this;
    }

    public String getDataplane() {
        return dataplane;
    }

    public P4Node setDataplane(String dataplane) {
        this.dataplane = dataplane;
        return this;
    }

    @Override
    public String toString() {
        return this.name + "[" + p4 + "," + dataplane + "]" + "["+ Objects.toString(this.ifaces) + "]";
    }
}
