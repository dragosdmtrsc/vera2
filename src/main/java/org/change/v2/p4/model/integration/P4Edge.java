package org.change.v2.p4.model.integration;

public class P4Edge {

    private NodeRef to, from;

    public NodeRef getTo() {
        return to;
    }

    public P4Edge setTo(NodeRef to) {
        this.to = to;
        return this;
    }

    public NodeRef getFrom() {
        return from;
    }

    public P4Edge setFrom(NodeRef from) {
        this.from = from;
        return this;
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
