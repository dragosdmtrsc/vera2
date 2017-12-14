package org.change.v2.p4.model.actions;

/**
 * Created by dragos on 30.08.2017.
 */
public enum P4ActionParameterType {
    HDR(1<<0),
    ARR(1<<1),
    FLD(1<<2),
    FLDLIST(1<<3),
    VAL(1<<4),
    C_REF(1<<5),
    M_REF(1<<6),
    R_REF(1<<7),
    FLC_REF(1<<8),
    UNKNOWN(1<<9);
    public final int x;

    P4ActionParameterType(int x) {
        this.x = x;
    }
}
