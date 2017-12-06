package org.change.v2.p4.model;

/**
 * Created by dragos on 04.10.2017.
 */
public enum InstanceType {
    PKT_INSTANCE_TYPE_NORMAL(0),
    PKT_INSTANCE_TYPE_INGRESS_CLONE(1),
    PKT_INSTANCE_TYPE_EGRESS_CLONE(2),
    PKT_INSTANCE_TYPE_COALESCED(3),
    PKT_INSTANCE_TYPE_RECIRC(4),
    PKT_INSTANCE_TYPE_REPLICATION(5),
    PKT_INSTANCE_TYPE_RESUBMIT(6);

    public int value;
    InstanceType(int value) {
        this.value = value;
    }

    public static InstanceType byValue(int val) {
        switch (val) {
            case 0: return PKT_INSTANCE_TYPE_NORMAL;
            case 1: return PKT_INSTANCE_TYPE_INGRESS_CLONE;
            case 2: return PKT_INSTANCE_TYPE_EGRESS_CLONE;
            case 3: return PKT_INSTANCE_TYPE_COALESCED;
            case 4: return PKT_INSTANCE_TYPE_RECIRC;
            case 5: return PKT_INSTANCE_TYPE_REPLICATION;
            case 6: return PKT_INSTANCE_TYPE_RESUBMIT;
            default: throw new IllegalArgumentException("No such instance type");
        }
    }
}