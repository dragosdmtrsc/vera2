package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class DataRef extends Expression {
    private long start, end;

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public DataRef(long start, long end) {
        this.start = start;
        this.end = end;
    }
}
