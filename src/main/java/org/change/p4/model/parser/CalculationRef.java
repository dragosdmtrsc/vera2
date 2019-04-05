package org.change.p4.model.parser;

import org.change.p4.model.Calculation;

public class CalculationRef extends Ref {
    private Calculation calc;

    public CalculationRef(Calculation calc) {
        this.calc = calc;
        this.setPath(calc.getName());
    }

    public Calculation getCalc() {
        return calc;
    }

    public CalculationRef setCalc(Calculation calc) {
        this.calc = calc;
        return this;
    }
}
