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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Daniel L. Rall
 * @author Stephen Colebourne
 * @author <a href="mailto:ridesmet@users.sourceforge.net">Ringo De Smet</a>
 * @author <a href="mailto:fredrik@westermarck.com>Fredrik Westermarck</a>
 * @author Holger Krauth
 * @author <a href="hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author Phil Steitz
 * @author Gary D. Gregory
 * @author Al Chou
 * @version $Id$
 */
public class StringsTest extends TestCase {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    static final String WHITESPACE;
    static final String NON_WHITESPACE;
    static final String TRIMMABLE;
    static final String NON_TRIMMABLE;

    static {
        String ws = "";
        String nws = "";
        String tr = "";
        String ntr = "";

        for (int i = 0; i < Character.MAX_VALUE; i++) {
            if (Character.isWhitespace((char)i)) {
                ws += String.valueOf((char)i);

                if (i > 32) {
                    ntr += String.valueOf((char)i);
                }
            } else if (i < 40) {
                nws += String.valueOf((char)i);
            }
        }

        for (int i = 0; i <= 32; i++) {
            tr += String.valueOf((char)i);
        }

        WHITESPACE = ws;
        NON_WHITESPACE = nws;
        TRIMMABLE = tr;
        NON_TRIMMABLE = ntr;
    }

    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {  };
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", new Long(2) };
    private static final String SEPARATOR = ",";
    private static final String TEXT_LIST = "foo,bar,baz";
    private static final String TEXT_LIST_NOSEP = "foobarbaz";

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testJoin_ArrayString() {
        assertEquals(null, Strings.join((Object[])null, null));
        assertEquals(TEXT_LIST_NOSEP, Strings.join(ARRAY_LIST, null));
        assertEquals(TEXT_LIST_NOSEP, Strings.join(ARRAY_LIST, ""));

        assertEquals("", Strings.join(NULL_ARRAY_LIST, null));

        assertEquals("", Strings.join(EMPTY_ARRAY_LIST, null));
        assertEquals("", Strings.join(EMPTY_ARRAY_LIST, ""));
        assertEquals("", Strings.join(EMPTY_ARRAY_LIST, SEPARATOR));

        assertEquals(TEXT_LIST, Strings.join(ARRAY_LIST, SEPARATOR));
        assertEquals(",,foo", Strings.join(MIXED_ARRAY_LIST, SEPARATOR));
        assertEquals("foo,2", Strings.join(MIXED_TYPE_LIST, SEPARATOR));
    }

    public void testJoin_IteratorString() {
        assertEquals(null, Strings.join((Iterator)null, null));
        assertEquals(TEXT_LIST_NOSEP, Strings.join(Arrays.asList(ARRAY_LIST).iterator(), null));
        assertEquals(TEXT_LIST_NOSEP, Strings.join(Arrays.asList(ARRAY_LIST).iterator(), ""));
        assertEquals("foo", Strings.join(Collections.singleton("foo").iterator(), "x"));
        assertEquals("foo", Strings.join(Collections.singleton("foo").iterator(), null));

        assertEquals("", Strings.join(Arrays.asList(NULL_ARRAY_LIST).iterator(), null));

        assertEquals("", Strings.join(Arrays.asList(EMPTY_ARRAY_LIST).iterator(), null));
        assertEquals("", Strings.join(Arrays.asList(EMPTY_ARRAY_LIST).iterator(), ""));
        assertEquals("", Strings.join(Arrays.asList(EMPTY_ARRAY_LIST).iterator(), SEPARATOR));

        assertEquals(TEXT_LIST, Strings.join(Arrays.asList(ARRAY_LIST).iterator(), SEPARATOR));
    }

    public void testSplit_String() {
        assertEquals(null, Strings.split(null));
        assertEquals(0, Strings.split("").length);

        String str = "a b  .c";
        String[] res = Strings.split(str);
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(".c", res[2]);

        str = " a ";
        res = Strings.split(str);
        assertEquals(1, res.length);
        assertEquals("a", res[0]);

        str = "a" + WHITESPACE + "b" + NON_WHITESPACE + "c";
        res = Strings.split(str);
        assertEquals(2, res.length);
        assertEquals("a", res[0]);
        assertEquals("b" + NON_WHITESPACE + "c", res[1]);
    }

    public void testSplitByWholeString_StringStringBoolean() {
        assertEquals(null, Strings.splitByWholeSeparator(null, "."));

        assertEquals(0, Strings.splitByWholeSeparator("", ".").length);

        String stringToSplitOnNulls = "ab   de fg";
        String[] splitOnNullExpectedResults = { "ab", "de", "fg" };

        String[] splitOnNullResults = Strings.splitByWholeSeparator("ab   de fg", null);
        assertEquals(splitOnNullExpectedResults.length, splitOnNullResults.length);

        for (int i = 0; i < splitOnNullExpectedResults.length; i += 1) {
            assertEquals(splitOnNullExpectedResults[i], splitOnNullResults[i]);
        }

        String stringToSplitOnCharactersAndString = "abstemiouslyaeiouyabstemiously";

        String[] splitOnStringExpectedResults = { "abstemiously", "abstemiously" };
        String[] splitOnStringResults = Strings.splitByWholeSeparator(stringToSplitOnCharactersAndString, "aeiouy");
        assertEquals(splitOnStringExpectedResults.length, splitOnStringResults.length);

        for (int i = 0; i < splitOnStringExpectedResults.length; i += 1) {
            assertEquals(splitOnStringExpectedResults[i], splitOnStringResults[i]);
        }

        String[] splitWithMultipleSeparatorExpectedResults = { "ab", "cd", "ef" };
        String[] splitWithMultipleSeparator = Strings.splitByWholeSeparator("ab:cd::ef", ":");
        assertEquals(splitWithMultipleSeparatorExpectedResults.length, splitWithMultipleSeparator.length);

        for (int i = 0; i < splitWithMultipleSeparatorExpectedResults.length; i++) {
            assertEquals(splitWithMultipleSeparatorExpectedResults[i], splitWithMultipleSeparator[i]);
        }
    }

    public void testSplitPreserveAllTokens_String() {
        assertEquals(null, Strings.splitPreserveAllTokens(null));
        assertEquals(0, Strings.splitPreserveAllTokens("").length);

        String str = "abc def";
        String[] res = Strings.splitPreserveAllTokens(str);
        assertEquals(2, res.length);
        assertEquals("abc", res[0]);
        assertEquals("def", res[1]);

        str = "abc  def";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(3, res.length);
        assertEquals("abc", res[0]);
        assertEquals("", res[1]);
        assertEquals("def", res[2]);

        str = " abc ";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("abc", res[1]);
        assertEquals("", res[2]);

        str = "a b .c";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(".c", res[2]);

        str = " a b .c";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("b", res[2]);
        assertEquals(".c", res[3]);

        str = "a  b  .c";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("", res[1]);
        assertEquals("b", res[2]);
        assertEquals("", res[3]);
        assertEquals(".c", res[4]);

        str = " a  ";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("", res[3]);

        str = " a  b";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("b", res[3]);

        str = "a" + WHITESPACE + "b" + NON_WHITESPACE + "c";
        res = Strings.splitPreserveAllTokens(str);
        assertEquals(WHITESPACE.length() + 1, res.length);
        assertEquals("a", res[0]);

        for (int i = 1; i < (WHITESPACE.length() - 1); i++) {
            assertEquals("", res[i]);
        }

        assertEquals("b" + NON_WHITESPACE + "c", res[WHITESPACE.length()]);
    }

    public void testReplace_StringStringString() {
        assertEquals(null, Strings.replace(null, null, null));
        assertEquals(null, Strings.replace(null, null, "any"));
        assertEquals(null, Strings.replace(null, "any", null));
        assertEquals(null, Strings.replace(null, "any", "any"));

        assertEquals("", Strings.replace("", null, null));
        assertEquals("", Strings.replace("", null, "any"));
        assertEquals("", Strings.replace("", "any", null));
        assertEquals("", Strings.replace("", "any", "any"));

        assertEquals("FOO", Strings.replace("FOO", "", "any"));
        assertEquals("FOO", Strings.replace("FOO", null, "any"));
        assertEquals("FOO", Strings.replace("FOO", "F", null));
        assertEquals("FOO", Strings.replace("FOO", null, null));

        assertEquals("", Strings.replace("foofoofoo", "foo", ""));
        assertEquals("barbarbar", Strings.replace("foofoofoo", "foo", "bar"));
        assertEquals("farfarfar", Strings.replace("foofoofoo", "oo", "ar"));
    }

    public void testReplaceOnce_StringStringString() {
        assertEquals(null, Strings.replaceOnce(null, null, null));
        assertEquals(null, Strings.replaceOnce(null, null, "any"));
        assertEquals(null, Strings.replaceOnce(null, "any", null));
        assertEquals(null, Strings.replaceOnce(null, "any", "any"));

        assertEquals("", Strings.replaceOnce("", null, null));
        assertEquals("", Strings.replaceOnce("", null, "any"));
        assertEquals("", Strings.replaceOnce("", "any", null));
        assertEquals("", Strings.replaceOnce("", "any", "any"));

        assertEquals("FOO", Strings.replaceOnce("FOO", "", "any"));
        assertEquals("FOO", Strings.replaceOnce("FOO", null, "any"));
        assertEquals("FOO", Strings.replaceOnce("FOO", "F", null));
        assertEquals("FOO", Strings.replaceOnce("FOO", null, null));

        assertEquals("foofoo", Strings.replaceOnce("foofoofoo", "foo", ""));
    }

    public void testIsEmpty() {
        assertEquals(true, Strings.isEmpty(null));
        assertEquals(true, Strings.isEmpty(""));
        assertEquals(false, Strings.isEmpty(" "));
        assertEquals(false, Strings.isEmpty("foo"));
        assertEquals(false, Strings.isEmpty("  foo  "));
    }

    public void testIsBlank() {
        assertEquals(true, Strings.isBlank(null));
        assertEquals(true, Strings.isBlank(""));
        assertEquals(true, Strings.isBlank(WHITESPACE));
        assertEquals(false, Strings.isBlank("foo"));
        assertEquals(false, Strings.isBlank("  foo  "));
    }

    public void testSplitPreserveAllTokens_StringString() {
        assertEquals(null, Strings.splitPreserveAllTokens(null, "."));
        assertEquals(0, Strings.splitPreserveAllTokens("", ".").length);

        String str = "a.b. c";
        String[] res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(" c", res[2]);

        str = "a.b.. c";
        res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(4, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("", res[2]);
        assertEquals(" c", res[3]);

        str = ".a.";
        res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);

        str = ".a..";
        res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("", res[3]);

        str = "..a.";
        res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);
        assertEquals("", res[3]);

        str = "..a";
        res = Strings.splitPreserveAllTokens(str, ".");
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);

        str = "a b c";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);

        str = "a  b  c";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("", res[1]);
        assertEquals("b", res[2]);
        assertEquals("", res[3]);
        assertEquals("c", res[4]);

        str = " a b c";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("b", res[2]);
        assertEquals("c", res[3]);

        str = "  a b c";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(5, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);
        assertEquals("b", res[3]);
        assertEquals("c", res[4]);

        str = "a b c ";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(4, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
        assertEquals("", res[3]);

        str = "a b c  ";
        res = Strings.splitPreserveAllTokens(str, " ");
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
        assertEquals("", res[3]);
        assertEquals("", res[4]);

        str = "a b.c. ";
        res = Strings.splitPreserveAllTokens(str, " .");
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
        assertEquals("", res[3]);
        assertEquals("", res[4]);
        // Match example in javadoc
        {
            String[] results = null;
            String[] expectedResults = { "a", "", "b", "c" };
            results = Strings.splitPreserveAllTokens("a..b.c", ".");
            assertEquals(expectedResults.length, results.length);

            for (int i = 0; i < expectedResults.length; i++) {
                assertEquals(expectedResults[i], results[i]);
            }
        }
    }

    public void testSplitTopLevel() {
        String[] result = Strings.splitTopLevel("hello(),goodbye()", '(', ')', ',');
        assertEquals(new String[] { "hello()", "goodbye()" }, result);

        result = Strings.splitTopLevel(" hello() , goodbye() ", '(', ')', ',');
        assertEquals(new String[] { " hello() ", " goodbye() " }, result);

        result = Strings.splitTopLevel("hello(nested, again(again())), goodbye", '(', ')', ',');
        assertEquals(new String[] { "hello(nested, again(again()))", " goodbye" }, result);
    }

    private void assertEquals(String[] expected, String[] actual) {
        if (!Arrays.equals(expected, actual)) {
            System.err.println("expected: " + Strings.asList(expected));
            System.err.println("  actual: " + Strings.asList(actual));
        }

        assertTrue(Arrays.equals(expected, actual));
    }

    public void testCommaSepList() {
        String strings = "hello, there, how, are, you, today?";
        List commaSepList = Strings.commaSepList(strings);
        String back = Strings.commaSepList(commaSepList);
        assertEquals(strings, back);
    }

    public void testSplitIncludeDelimiters() {
        String[] result = Strings.splitIncludeDelimiters("8+2=10", "+=");
        assertEquals(new String[] { "8", "+", "2", "=", "10" }, result);
    }

    public void testTrimsNull() {
        assertNull(Strings.trim(null));
    }
}
