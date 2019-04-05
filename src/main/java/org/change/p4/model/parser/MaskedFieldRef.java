package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.exp.LiteralExpr;
import scala.math.BigInt;

public class MaskedFieldRef extends Expression {
    private FieldRef reference;
    private LiteralExpr mask;

    public MaskedFieldRef(FieldRef reference, BigInt mask) {
        this.reference = reference;
        this.mask = new LiteralExpr(mask, -1);
    }
    public FieldRef getReference() {
        return reference;
    }
    public MaskedFieldRef setReference(FieldRef reference) {
        this.reference = reference;
        return this;
    }

    public LiteralExpr getMask() {
        return mask;
    }
}
