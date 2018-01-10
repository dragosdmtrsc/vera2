Issues:
1. egress_port - as per spec, this field is supposed to be
read-only. Why is it written to?

2. tables directtoprio & setprio will always experience meta.register_tmp == 0

3.  Spec says: "If the counter is declared with the static attribute, the counter resource is dedicated to
the indicated table. The compiler must raise an error if the counter name is referenced
by actions used in another table." => We must check that this is enforced - apparently,
ndp_router is not compliant

4. An error should be raised whenever trying to access a register with an out-of-bounds
index - no validation performed at this time