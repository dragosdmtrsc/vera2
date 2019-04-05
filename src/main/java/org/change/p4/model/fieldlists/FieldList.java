package org.change.v2.p4.model.fieldlists;

import org.change.v2.p4.model.parser.FieldRef;
import org.change.v2.p4.model.parser.HeaderRef;
import scala.math.BigInt;

import java.util.ArrayList;
import java.util.List;

public class FieldList {
    private String name;
    private List<Entry> entries = new ArrayList<>();

    public FieldList(String name) {
        this.name = name;
    }
    public FieldList add(Entry newEntry) {
        entries.add(newEntry);
        return this;
    }

    public String getName() {
        return name;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public interface Entry {}
    public static class FieldRefEntry implements Entry {
        private FieldRef fieldRef;
        public FieldRefEntry(FieldRef fieldRef) {
            this.fieldRef = fieldRef;
        }
        public FieldRef getFieldRef() {
            return fieldRef;
        }
    }
    public static class StringRefEntry implements Entry {
        private String ref;

        public StringRefEntry(String ref) {
            this.ref = ref;
        }

        public String getRef() {
            return ref;
        }
    }
    public static class HeaderRefEntry implements Entry {
        private HeaderRef headerRef;

        public HeaderRefEntry(HeaderRef headerRef) {
            this.headerRef = headerRef;
        }

        public HeaderRef getHeaderRef() {
            return headerRef;
        }
    }
    public static class PayloadEntry implements Entry {}
    public static class FieldValueEntry implements Entry {
        private BigInt number;
        private int width;

        public FieldValueEntry(BigInt number, int width) {
            this.number = number;
            this.width = width;
        }
        public BigInt getNumber() {
            return number;
        }
        public int getWidth() {
            return width;
        }
    }
    public static class FieldListEntry implements Entry {
        FieldList fieldList;
        public FieldListEntry(String fieldList) {
            this.fieldList = new FieldList(fieldList);
        }
        public FieldListEntry(FieldList fieldList) {
            this.fieldList = fieldList;
        }

        public FieldList getFieldList() {
            return fieldList;
        }

        public FieldListEntry setFieldList(FieldList fieldList) {
            this.fieldList = fieldList;
            return this;
        }
    }
}
