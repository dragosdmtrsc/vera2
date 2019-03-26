package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.RegisterSpecification;

public class RegisterRef extends Ref {
    private RegisterSpecification register;
    public RegisterRef() {}

    public RegisterSpecification getRegister() {
        return register;
    }

    public RegisterRef setRegister(RegisterSpecification register) {
        this.register = register;
        return this;
    }
}
