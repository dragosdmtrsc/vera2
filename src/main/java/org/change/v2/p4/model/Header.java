package org.change.v2.p4.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 01.09.2017.
 */
public class Header {
    private String name;
    private int length;
    private int maxLength;

    public int getLength() {
        return length;
    }

    public Header setLength(int length) {
        this.length = length;
        return this;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public Header setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    private List<Field> fields = new ArrayList<Field>();

    public String getName() {
        return name;
    }

    public Header setName(String name) {
        this.name = name;
        return this;
    }

    public Header addField(Field field) {
        fields.add(field);
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }
}
