package org.change.v2.p4.model;

import generated.parse.p4.P4GrammarBaseListener;
import generated.parse.p4.P4GrammarParser;

/**
 * Created by dragos on 04.09.2017.
 */
public class P4ParserListener extends P4GrammarBaseListener {

    @Override
    public void exitParser_function_body(P4GrammarParser.Parser_function_bodyContext ctx) {
        super.exitParser_function_body(ctx);
    }

    @Override
    public void exitParser_function_declaration(P4GrammarParser.Parser_function_declarationContext ctx) {
        super.exitParser_function_declaration(ctx);
    }

    @Override
    public void exitParser_exception_name(P4GrammarParser.Parser_exception_nameContext ctx) {
        super.exitParser_exception_name(ctx);
    }

    @Override
    public void exitParser_exception_declaration(P4GrammarParser.Parser_exception_declarationContext ctx) {
        super.exitParser_exception_declaration(ctx);
    }

    @Override
    public void exitMetadata_expr(P4GrammarParser.Metadata_exprContext ctx) {
        super.exitMetadata_expr(ctx);
    }
}
