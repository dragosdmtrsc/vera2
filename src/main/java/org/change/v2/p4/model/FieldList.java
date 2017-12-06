package org.change.v2.p4.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 31.08.2017.
 */
public class FieldList {
    private String name;
    private List<String> fields = new ArrayList<String>();

    public FieldList(String name, List<String> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public List<String> getFields() {
        return fields;
    }
}
