package org.change.v2.p4.model;

/**
 * Created by dragos on 31.08.2017.
 */
public class RegisterSpecification {
    private String name;
    private int width;
    private int count;
    private boolean isDirect;
    private String directTable;
    private boolean isStatic;
    private String staticTable;

    public String getName() {
        return name;
    }

    public RegisterSpecification setName(String name) {
        this.name = name;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public RegisterSpecification setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getCount() {
        return count;
    }

    public RegisterSpecification setCount(int count) {
        this.count = count;
        return this;
    }

    public boolean isDirect() {
        return isDirect;
    }

    public RegisterSpecification setDirect(boolean direct) {
        isDirect = direct;
        return this;
    }

    public String getDirectTable() {
        return directTable;
    }

    public RegisterSpecification setDirectTable(String directTable) {
        this.directTable = directTable;
        return this;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public RegisterSpecification setStatic(boolean aStatic) {
        isStatic = aStatic;
        return this;
    }

    public String getStaticTable() {
        return staticTable;
    }

    public RegisterSpecification setStaticTable(String staticTable) {
        this.staticTable = staticTable;
        return this;
    }
}
