package org.change.v2.p4.model.parser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by dragos on 12.09.2017.
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "type")
public class Expression {
}
