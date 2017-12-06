package org.change.v2.smt;

import org.smtlib.IExpr;
import org.smtlib.IExpr.*;
import org.smtlib.IVisitor.NullVisitor;

import java.util.Map;

public class RenameVisitor extends NullVisitor<IExpr> {

    private Map<String, String> map;
    private IFactory factory;

    public RenameVisitor(Map<String, String> theMap,
                         IExpr.IFactory factory) {
        super();
        this.map = theMap;
        this.factory = factory;
    }


    @Override
    public IExpr visit(IAttributedExpr arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return factory.attributedExpr(arg0.expr().accept(this), arg0.attributes());
    }

    @Override
    public IExpr visit(IBinaryLiteral arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }

    @Override
    public IExpr visit(IDecimal arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }


    @Override
    public IExpr visit(IFcnExpr arg0)
            throws org.smtlib.IVisitor.VisitorException {
        IExpr[] exprs = new IExpr[arg0.args().size()];
        int i = 0;
        for (IExpr ex : arg0.args()) {
            exprs[i++] = ex.accept(this);
        }
        return factory.fcn(arg0.head(), exprs);
    }

    @Override
    public IExpr visit(IHexLiteral arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }

    @Override
    public IExpr visit(INumeral arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }


    @Override
    public IExpr visit(IParameterizedIdentifier arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }

    @Override
    public IExpr visit(IAsIdentifier arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }

    @Override
    public IExpr visit(IStringLiteral arg0)
            throws org.smtlib.IVisitor.VisitorException {
        return arg0;
    }

    @Override
    public IExpr visit(ISymbol arg0)
            throws org.smtlib.IVisitor.VisitorException {
        if (this.map.containsKey(arg0.value())) {
            return factory.symbol(this.map.get(arg0.value()));
        }
        return arg0;
    }


}
