package org.change.v2.p4.model.table;

/**
 * Created by dragos on 06.09.2017.
 */
public class TableMatch {
    private String table;
    private String key;
    private MatchKind matchKind;

    public TableMatch(String table, String key, MatchKind kind) {
        assert table != null;
        assert key != null;
        this.table = table;
        this.key = key;
        this.matchKind = kind;
    }

    public String getKey() {
        return key;
    }

    public MatchKind getMatchKind() {
        return matchKind;
    }
    public String getTable() {
        return table;
    }

    public void instantiate(String arg) {

    }

}
