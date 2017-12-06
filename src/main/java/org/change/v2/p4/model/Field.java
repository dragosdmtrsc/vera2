package org.change.v2.p4.model;

/**
 * Created by dragos on 01.09.2017.
 */
public class Field {
    private String name;
    private int length;
    private int modifier = 0;

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public int getLength() {
        return length;
    }

    public Field setLength(int length) {
        this.length = length;
        return this;
    }

    public Field setSaturating() {
        this.modifier |= 1 << 1;
        return this;
    }
    public Field setSigned() {
        this.modifier |= 1 << 0;
        return this;
    }

    public boolean isSigned() {
        return (this.modifier & 1 << 0) != 0;
    }
    public boolean isSaturating() {
        return (this.modifier & 1 << 1) != 0;
    }

}
