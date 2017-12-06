package org.change.v2.p4.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dragos on 01.09.2017.
 */
public class HeaderInstance {
    private Header layout;
    private String name;
    private boolean metadata;

    private Map<String, Long> initializer = new HashMap<String, Long>();

    public HeaderInstance(Header layout, String name) {
        this.layout = layout;
        this.name = name;
    }

    public HeaderInstance addInitializer(String field, Long value) {
        this.initializer.put(field, value);
        return this;
    }

    public Map<String, Long> getInitializer() {
        return initializer;
    }

    public Header getLayout() {
        return layout;
    }

    public String getName() {
        return name;
    }

    public boolean isMetadata() {
        return this.metadata;
    }
    public HeaderInstance setMetadata(boolean tf) {
        this.metadata = tf;
        return this;
    }
}
