package org.change.v2.p4.model.table;

import scala.math.BigInt;

/**
 * Created by dragos on 06.09.2017.
 */
public class TableMatch {
    private String table;
    private String key;
    private MatchKind matchKind;
    private BigInt mask = BigInt.apply(-1);

    public TableMatch(String table, String key, MatchKind kind, BigInt mask) {
        assert table != null;
        assert key != null;
        this.table = table;
        this.key = key;
        this.matchKind = kind;
        this.mask = mask;
    }
    public TableMatch(String table, String key, MatchKind kind) {
        this(table, key, kind, BigInt.apply(-1));
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
