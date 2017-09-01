package org.change.v2.p4.model;

import org.change.v2.p4.model.Header;
import org.change.v2.p4.model.HeaderInstance;

/**
 * Created by dragos on 01.09.2017.
 */
public class ArrayInstance extends HeaderInstance {
    private int length;
    public ArrayInstance(Header layout, String name, int length) {
        super(layout, name);
        this.length = length;
    }

    public int getLength() {
        return length;
    }
}
