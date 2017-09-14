package org.change.v2.p4.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dragos on 01.09.2017.
 */
public class Header {
    private String name;
    private int length = -1;
    private int maxLength;

    public int getLength() {
        if (length == -1) {
            length = 0;
            for (Field f : fields) {
                length += f.getLength();
            }
        }
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

    private Map<String, Field> namedFields = new HashMap<String, Field>();

    public String getName() {
        return name;
    }

    public Header setName(String name) {
        this.name = name;
        return this;
    }

    public Header addField(Field field) {
        fields.add(field);
        this.namedFields.put(field.getName(), field);
        return this;
    }

    public Field getField(String fieldName) {
        if (this.namedFields.containsKey(fieldName)) {
            return this.namedFields.get(fieldName);
        }
        return null;
    }
    public List<Field> getFields() {
        return fields;
    }
}
