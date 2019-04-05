package org.change.v2.p4.model.parser;

public class Ref extends Expression {
    protected String path;
    public String getPath() {
        return path;
    }

    public Ref setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String toString() {
        return getPath();
    }
}
