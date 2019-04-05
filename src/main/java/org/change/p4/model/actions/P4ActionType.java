package org.change.v2.p4.model.actions;

/**
 * Created by dragos on 30.08.2017.
 */
public enum P4ActionType {
    Complex,
    AddHeader,
    CopyHeader,
    RemoveHeader,
    ModifyField,
    AddToField,
    Add,
    SubtractFromField,
    Subtract,
    ModifyFieldWithHashBasedOffset,
    ModifyFieldRngUniform,
    BitAnd,
    BitOr,
    BitXor,
    ShiftLeft,
    ShiftRight,
    Truncate,
    Drop,
    NoOp,
    Push,
    Pop,
    Count,
    ExecuteMeter,
    RegisterRead,
    RegisterWrite,
    GenerateDigest,
    Resubmit,
    Recirculate,
    CloneIngressPktToIngress,
    CloneEgressPktToIngress,
    CloneIngressPktToEgress,
    CloneEgressPktToEgress,
    UNKNOWN
}
