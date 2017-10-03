package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class Value {
    private long value, mask;

    public Value() {
    }

    public Value(long value, long mask) {
        this.value = value;
        this.mask = mask;
    }

    public long getValue() {
        return value;
    }

    public Value setValue(long value) {
        this.value = value;
        return this;
    }

    public long getMask() {
        return mask;
    }

    public Value setMask(long mask) {
        this.mask = mask;
        return this;
    }
}
