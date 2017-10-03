package org.change.v2.p4.model.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dragos on 13.09.2017.
 */
public class ParserInterpreter {
    public static Expression parseExpression(String ref) {
        Pattern currentRegex = Pattern.compile("current\\(([0-9]+),.*?([0-9]+)\\)");
        Pattern arrayThing = Pattern.compile("(.*?)\\[(.*?)\\]");
        Pattern latestRegex = Pattern.compile("latest\\.(.*)");
        Matcher matcher = currentRegex.matcher(ref);
        if (matcher.matches()) {
            int start = Integer.decode(matcher.group(1)).intValue();
            int end = Integer.decode(matcher.group(2)).intValue();
            return new DataRef(start, end);
        } else {
            matcher = arrayThing.matcher(ref);
            if (matcher.matches()) {
                String index = matcher.group(2);
                String instance = matcher.group(1);
                StringRef stringRef = new StringRef(instance);
                if (index.equals("next")) {
                    stringRef.setNext();
                } else {
                    stringRef.setArrayIndex(Integer.decode(index));
                }
                return stringRef;
            } else {
                matcher = latestRegex.matcher(ref);
                if (matcher.matches()) {
                    return new LatestRef(matcher.group(1));
                } else {
                    try {
                        return new ConstantExpression(Integer.decode(ref));
                    } catch (NumberFormatException nfe) {
                        return new StringRef(ref);
                    }
                }
            }
        }
    }
}
