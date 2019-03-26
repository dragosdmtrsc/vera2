package org.change.v2.p4.model.parser;

import java.util.Objects;

/**
 * Created by dragos on 12.09.2017.
 */
public class ReturnStatement extends Statement {
    private String where;
    private String message;
    private boolean isError;

    public ReturnStatement() {}
    public ReturnStatement(ReturnStatement rs) {
        where = rs.where;
        message = rs.message;
        isError = rs.isError;
    }

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

    @Override
    public String toString() {
        if (isError) {
            return "error " + getMessage();
        }
        return "return " + getWhere();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnStatement that = (ReturnStatement) o;
        return isError == that.isError &&
                Objects.equals(where, that.where) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(where, message, isError);
    }
}
