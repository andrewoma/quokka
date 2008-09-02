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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class PropertyExpressionParserTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List refs;
    private Map props = new HashMap();
    private PropertyProvider provider = new PropertyProvider() {
            public String getProperty(String key) {
                return (String)props.get(key);
            }
        };

    private String result;
    private String newExpression;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public PropertyExpressionParserTest() {
        props.put("prop1", "val1");
        props.put("prop2", "val2");
        props.put("prop3", "val3");
        props.put("prop4", "val4");
        props.put("prop5", "val5");
        props.put("instancePrefix", "javac.");
        props.put("javac.defaults", "javac.defs.");
        props.put("javac.defs.target", "sometarget");
        props.put("javac.src", "somedir");
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testRefsWhiteSpace() {
        refs("@   ifdef   (   quokka.global.javac.source   ?   '   \"hello   '   :   '   goodbye?   '   )   ");
        assertEquals(asList(new String[] { "quokka.global.javac.source" }), refs);
    }

    public void testProperty() {
        eval("@prop1");
        assertEquals("val1", result);
    }

    public void testPropertyNotFound() {
        eval("@prop33");
        assertNull(result);
    }

    public void testRefs() {
        refs("@ifdef(quokka.global.javac.source?'hello':'goodbye')");
        assertEquals(asList(new String[] { "quokka.global.javac.source" }), refs);
    }

    public void testRefMultiple() {
        refs("@ifdef(source?target1:target2)");
        assertEquals(asList(new String[] { "source", "target1", "target2" }), refs);
    }

    public void testEvalSetIfDefTrue() {
        eval("@setifdef prop1");
        assertEquals("val1", result);
    }

    public void testEvalIfDefTrue() {
        eval("@ifdef prop1 ? prop2 : prop3");
        assertEquals("val2", result);
    }

    public void testEvalIfDefFalse() {
        eval("@ifdef someprop ? prop2 : prop3");
        assertEquals("val3", result);
    }

    public void testUndef() {
        eval("@undef");
        assertNull(result);
    }

    public void testLiteral() {
        eval("@'hello'");
        assertEquals("hello", result);
    }

    public void testParens() {
        eval("@('hello')");
        assertEquals("hello", result);
    }

    public void testNestedIfDef() {
        eval("@ifdef prop1 ? (ifdef someprop ? prop3 : prop4) : prop2");
        assertEquals("val4", result);
    }

    public void testLiteral2() {
        eval("@\"hello\"");
        assertEquals("hello", result);
    }

    public void testConcat() {
        eval("@'hello' + 'there'");
        assertEquals("hellothere", result);
    }

    public void testConcat2() {
        eval("@'hello' + ' there' + ' my' + ' friend'");
        assertEquals("hello there my friend", result);
    }

    public void testComplex1() {
        eval("@'head:' + prop1 + (ifdef prop2 ? prop3 : prop4) + ':tail'");
        assertEquals("head:val1val3:tail", result);
    }

    public void testComplex2() {
        eval("@'head:' + prop1 + ':' + (ifdef prop2 ? prop3 : prop4) + ':tail'");
        assertEquals("head:val1:val3:tail", result);
    }

    public void testComplex3() {
        eval("@'head:' + prop1 + ':' + (ifdef prop2 ? 'pre3-' + prop3 + '-post3' : prop4) + ':tail'");
        assertEquals("head:val1:pre3-val3-post3:tail", result);
    }

    public void testComplex3Refs() {
        refs("@'head:' + prop1 + ':' + (ifdef prop2 ? 'pre3-' + prop3 + '-post3' : prop4) + ':tail'");
        assertEquals(asList(new String[] { "prop1", "prop2", "prop3", "prop4" }), refs);
    }

    public void testNestedUndef() {
        eval("@ifdef prop1 ? (ifdef someprop ? 'valuex' : undef) : 'valuey'");
        assertNull(result);
    }

    public void testEvalSetIfDefFalse() {
        eval("@setifdef someprop");
        assertNull(result);
    }

    public void testRef() {
        eval("@ref 'prop' + '1'");
        assertEquals("val1", result);
    }

    public void testDefaultRef() {
        eval("@setifdef ref instancePrefix + 'src'");
        assertEquals("somedir", result);
    }

    public void testDefaultRef2() {
        eval("@setifdef ref instancePrefix + 'dummy'");
        assertNull(result);
    }

    public void testReplace1() {
        Map refs = new HashMap();
        refs.put("prop1", "prop8");
        refs.put("prop3", "prop9");
        replace("@'head:' + prop1 + ':' + (ifdef prop2 ? 'pre3-' + prop3 + '-post3' : prop4) + ':tail'", refs);
        assertEquals("@'head:' + prop8 + ':' + ( ifdef prop2 ? 'pre3-' + prop9 + '-post3' : prop4 ) + ':tail' ",
            newExpression);
    }

    public void testDefaultRef2Levels() {
        // real use:
        //     # Global defaults
        //     property name="javacDefaults.target" value="targetDir"
        //     property name="javacDefaults.src" value="srcDir"
        //
        //     # Optional instance defaults
        //     property name="myJavac.instanceDefaults" value="myJavacDefaults."
        //     property name="myJavac.sourceDir" value="someSource"
        //     
        //     property name="prefix.defaults" value="${ifdef (ref prefix + 'defaults') ? prefix + 'defaults' : 'javac.defaults.' }
        //     property name="prefix.javac.target" value="${@setifdef ref prefix.defaults + 'target'}"
        eval("@setifdef ref (ref instancePrefix + 'defaults') + 'target'");
        assertEquals("sometarget", result);
    }

    public List asList(String[] strings) {
        return Arrays.asList(strings);
    }

    public String eval(String expression) {
        PropertyExpressionParser parser = new PropertyExpressionParser(expression);
        result = parser.evaluate(provider);

        return result;
    }

    public List refs(String expression) {
        PropertyExpressionParser parser = new PropertyExpressionParser(expression);
        refs = parser.getPropertyReferences();

        return refs;
    }

    public String replace(String expression, Map refs) {
        PropertyExpressionParser parser = new PropertyExpressionParser(expression);
        newExpression = parser.replace(refs);

        return newExpression;
    }
}
