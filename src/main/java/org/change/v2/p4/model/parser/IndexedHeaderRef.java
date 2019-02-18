package org.change.v2.p4.model.parser;

public class IndexedHeaderRef extends HeaderRef {
    private int index;
    @Override
    public boolean isArray() { return true; }
    public boolean isLast() { return index == -1; }
    public IndexedHeaderRef setLast() { index = -1; return this; }
    public int getIndex() { return index; }
    public IndexedHeaderRef setIndex(int index) {
        this.index = index;
        return this;
    }
}
