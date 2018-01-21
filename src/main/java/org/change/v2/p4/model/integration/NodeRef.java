package org.change.v2.p4.model.integration;

public class NodeRef {
    private String node;
    private int port;

    public String getNode() {
        return node;
    }

    public NodeRef setNode(String node) {
        this.node = node;
        return this;
    }

    public int getPort() {
        return port;
    }

    public NodeRef setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public String toString() {
        return node + "@" + port;
    }
}