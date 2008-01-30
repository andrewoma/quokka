/*
 * Copyright 2007-2008 Andrew O'Malley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ws.quokka.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class PropertyExpressionParser {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final List RESERVED_WORDS = Arrays.asList(new String[] { "ifdef", "setifdef", "undef", "ref" });
    private static final String QUOTE_CHARS = "'\"";
    private static final String CHAR_TOKENS = "?+:()";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List tokens = null;
    private String expression;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public PropertyExpressionParser(String expression) {
        this.expression = expression;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getPropertyReferences() {
        List propertyReferences = new ArrayList();

        for (Iterator i = getTokens().iterator(); i.hasNext();) {
            String token = (String)i.next();

            if (isPropertyReference(token)) {
                propertyReferences.add(token);
            }
        }

        return propertyReferences;
    }

    private boolean isPropertyReference(String token) {
        return !isStringLiteral(token) && !isReservedWord(token) && !isCharToken(token);
    }

    private boolean isStringLiteral(String token) {
        return QUOTE_CHARS.indexOf(token.charAt(0)) != -1;
    }

    private boolean isReservedWord(String token) {
        return RESERVED_WORDS.contains(token);
    }

    private boolean isCharToken(String token) {
        return (token.length() == 1) && (CHAR_TOKENS.indexOf(token) != -1);
    }

    public String evaluate(PropertyProvider provider) {
        String result = evaluate(getTokens(), provider);

        return (result == null) ? null : literalValue(result);
    }

    public String replace(PropertyProvider provider) {
        StringBuffer newExpression = new StringBuffer("@");

        for (Iterator i = getTokens().iterator(); i.hasNext();) {
            String token = (String)i.next();
            String newPropertyRef = provider.getProperty(token);

            if (isPropertyReference(token) && (newPropertyRef != null)) {
                token = newPropertyRef;
            }

            newExpression.append(token).append(" ");
        }

        return newExpression.toString();
    }

    public String replace(final Map refs) {
        return replace(new PropertyProvider() {
                public String getProperty(String key) {
                    return (String)refs.get(key);
                }
            });
    }

    private String evaluate(List tokens, PropertyProvider provider) {
        if (tokens.size() == 0) {
            return null; // undefined
        }

        String token = (String)tokens.get(0);

        if (tokens.size() == 1) {
            if (isStringLiteral(token)) {
                return token;
            } else if (isPropertyReference(token)) {
                return toLiteral(provider.getProperty(token));
            } else if (token.equals("undef")) {
                return null;
            } else {
                throw error("Evaluates to '" + token + "' which is not a string literal or property reference");
            }
        }

        if (token.equals("setifdef")) {
            String message = "syntax for setifdef: 'setifdef <property-ref>': " + tokens;
            assertTrue(tokens.size() >= 2, message);

            return evaluate(tokens.subList(1, tokens.size()), provider);

            //            assertTrue(isStringLiteral(result), message);
            //            return (String) properties.get(result);
        }

        if (token.equals("ifdef")) {
            //            assertTrue(tokens.size() > 3 && isPropertyReference(nextToken) && tokens.get(2).equals("?"), "ifdef syntax is 'ifdef <property ref> ? true-expression : false-expression':" + tokens);
            int endIf = findMatching(tokens, "?");
            boolean defined = evaluate(tokens.subList(1, endIf), provider) != null;

            //            assertTrue(isPropertyReference(ref), "ifdef syntax is 'ifdef <propertyref-expression> ? true-expression : false-expression':" + tokens);
            int endTrue = findMatching(tokens, ":");

            if (defined) {
                return evaluate(tokens.subList(endIf + 1, endTrue), provider);
            } else {
                return evaluate(tokens.subList(endTrue + 1, tokens.size()), provider);
            }
        }

        if (token.equals("ref")) {
            String message = "syntax for ref: 'ref <string-expression>': " + tokens;
            assertTrue(tokens.size() >= 2, message);

            String result = evaluate(tokens.subList(1, tokens.size()), provider);
            assertTrue(isStringLiteral(result), message);

            return toLiteral(provider.getProperty(literalValue(result)));
        }

        if (token.equals("(")) {
            // Find the matching brace and evaluate it
            int matching = findMatching(tokens, ")");
            String result = evaluate(tokens.subList(1, matching), provider);

            if (matching == (tokens.size() - 1)) {
                return result;
            }

            tokens = tokens.subList(matching + 1, tokens.size());
            tokens.add(0, (result == null) ? null : result);

            return evaluate(tokens, provider);
        }

        String nextToken = (String)tokens.get(1);

        if (nextToken.equals("+")) { // infix operator
            assertTrue(tokens.size() >= 3, "syntax for + operator: <operand1> + <operand2>: " + tokens);

            List firstToken = new ArrayList();
            firstToken.add(token);

            String result = evaluate(firstToken, provider);
            assertTrue(result != null, "Cannot apply + operator to a null");

            return toLiteral(literalValue(result) + literalValue(evaluate(tokens.subList(2, tokens.size()), provider)));
        }

        throw error("Unexpected token: " + token + ", remaining tokens: " + tokens);
    }

    private String toLiteral(String value) {
        return (value == null) ? null : ("'" + value + "'");
    }

    private int findMatching(List tokens, String match) {
        int count = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String tok = (String)tokens.get(i);

            if (tok.equals("(")) {
                count++;
            }

            if (tok.equals(")")) {
                count--;
            }

            if ((count == 0) && tok.equals(match)) {
                return i;
            }
        }

        throw error("No matching token found for '" + match + "': " + tokens);
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw error(message);
        }
    }

    private RuntimeException error(String message) {
        return new RuntimeException("Invalid expression '" + expression + "'. " + message);
    }

    private String literalValue(String token) {
        return (token.length() < 2) ? "" : token.substring(1, token.length() - 1);
    }

    private List getTokens() {
        if (tokens != null) {
            return tokens;
        }

        tokens = new ArrayList();

        char quote = 0;
        StringBuffer token = new StringBuffer();

        for (int i = 1; i < expression.length(); i++) {
            char ch = expression.charAt(i);

            if (quote == 0) {
                if ((Character.isWhitespace(ch) || (QUOTE_CHARS.indexOf(ch) != -1) || (CHAR_TOKENS.indexOf(ch) != -1))
                        && (token.length() != 0)) {
                    token = addToken(token);
                }

                if (!Character.isWhitespace(ch)) {
                    token.append(ch);
                }

                if (QUOTE_CHARS.indexOf(ch) != -1) {
                    quote = ch;
                }

                if (CHAR_TOKENS.indexOf(ch) != -1) {
                    token = addToken(token);
                }
            } else {
                if (ch == quote) {
                    quote = 0;
                    token.append(ch);
                    token = addToken(token);
                } else {
                    token.append(ch);
                }
            }
        }

        if (token.length() != 0) {
            addToken(token);
        }

        //        System.out.println("expression=" + expression);
        //        System.out.println("    tokens=" + tokens);
        return tokens;
    }

    private StringBuffer addToken(StringBuffer token) {
        tokens.add(token.toString());

        return new StringBuffer();
    }
}
