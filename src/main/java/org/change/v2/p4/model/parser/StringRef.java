package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class StringRef extends Expression {
    private String ref;
    private int arrayIndex = -2;

    public int getArrayIndex() {
        return arrayIndex;
    }

    public StringRef setArrayIndex(int arrayIndex) {
        this.arrayIndex = arrayIndex;
        return this;
    }

    public boolean isArray() {
        return arrayIndex >= -1 ;
    }

    public boolean isNext() {
        return arrayIndex == -1;
    }

    public StringRef setNext() {
        this.arrayIndex = -1;
        return this;
    }


    public String getRef() {
        return ref;
    }

    public StringRef(String ref) {
        this.ref = ref;
    }
}
