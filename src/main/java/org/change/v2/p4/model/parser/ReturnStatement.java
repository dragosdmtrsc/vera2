package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class ReturnStatement extends Statement {
    private String where;
    private String message;
    private boolean isError;

    public ReturnStatement(String where) {
        this.where = where;
    }

    public String getMessage() {
        return message;
    }

    public ReturnStatement setMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isError() {
        return isError;
    }

    public ReturnStatement setError(boolean error) {
        isError = error;
        return this;
    }

    public String getWhere() {
        return where;
    }
}
