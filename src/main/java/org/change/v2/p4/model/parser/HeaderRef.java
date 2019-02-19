package org.change.v2.p4.model.parser;

public class HeaderRef extends Expression {
    private String path;
    public boolean isArray() { return false; }
    public String getPath() {
        return path;
    }
    public HeaderRef setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String toString() {
        return path;
    }
}
