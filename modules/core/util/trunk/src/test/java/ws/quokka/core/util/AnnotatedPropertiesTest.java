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

import org.apache.tools.ant.BuildException;

import ws.quokka.core.test.AbstractTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class AnnotatedPropertiesTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map replacements;
    private AnnotatedProperties properties = new AnnotatedProperties();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        replacements = new HashMap();
        replacements.put("key1", "rep1");
        replacements.put("key2", "rep2");
        replacements.put("key3", "rep3");
        replacements.put("key4", "rep4");
    }

    public void testReplaceReferencesSimple() {
        assertEquals("${rep1}", replaceRefs("${key1}"));
        assertEquals("abc${rep1}", replaceRefs("abc${key1}"));
        assertEquals("${rep1}abc", replaceRefs("${key1}abc"));
        assertEquals("abc${rep1}xyz", replaceRefs("abc${key1}xyz"));
        assertEquals("abc${rep1}xyz${rep2}", replaceRefs("abc${key1}xyz${key2}"));
    }

    public void testReplaceReferencesExpressions() {
        assertEquals("${@rep1 }", replaceRefs("${@key1}"));
        assertEquals("${@ifdef rep1 ? ( ifdef rep2 ? rep3 : rep4 ) : rep2 }",
            replaceRefs("${@ifdef key1 ? (ifdef key2 ? key3 : key4) : key2 }"));
        assertEquals("Hello ${rep1} there ${@rep2 }", replaceRefs("Hello ${key1} there ${@key2 }"));
    }

    public void testSetIfdef() {
        properties.put("prefix.defs", "prefix1");
        properties.put("prefix.in", "${@setifdef ref prefix.defs + '.in'}");
        properties = properties.evaluateReferences(true);

        Object value = properties.get("prefix.in");
        System.out.println("value=" + value);
        assertNull(value);
    }

    public void testWildcards() {
        properties.put("prefix1.key1", "value1");
        properties.put("prefix1.key2", "value2");
        properties.put("prefix2.*", "prefix1");
        properties = properties.evaluateReferences(true);
        assertEquals(4, properties.size());
        assertEquals("value1", properties.get("prefix1.key1"));
        assertEquals("value2", properties.get("prefix1.key2"));
        assertEquals("value1", properties.get("prefix2.key1"));
        assertEquals("value2", properties.get("prefix2.key2"));
    }

    public void testWildcardsMultiple() {
        properties.put("prefix1.key1", "value1");
        properties.put("prefix1.key2", "value2");
        properties.put("prefix2.key1", "value3");
        properties.put("prefix2.key2", "value4");
        properties.put("prefix3.*", "prefix1");
        properties.put("prefix4.*", "prefix2");
        properties = properties.evaluateReferences(true);
        assertEquals(8, properties.size());
        assertEquals("value1", properties.get("prefix1.key1"));
        assertEquals("value2", properties.get("prefix1.key2"));
        assertEquals("value3", properties.get("prefix2.key1"));
        assertEquals("value4", properties.get("prefix2.key2"));
        assertEquals("value1", properties.get("prefix3.key1"));
        assertEquals("value2", properties.get("prefix3.key2"));
        assertEquals("value3", properties.get("prefix4.key1"));
        assertEquals("value4", properties.get("prefix4.key2"));
    }

    public void testWildcardsOverride() {
        properties.put("prefix1.key1", "value1");
        properties.put("prefix1.key2", "value2");
        properties.put("prefix2.*", "prefix1");
        properties.put("prefix2.key2", "overridden2");
        properties = properties.evaluateReferences(true);
        assertEquals(4, properties.size());
        assertEquals("value1", properties.get("prefix1.key1"));
        assertEquals("value2", properties.get("prefix1.key2"));
        assertEquals("value1", properties.get("prefix2.key1"));
        assertEquals("overridden2", properties.get("prefix2.key2"));
    }

    public void testWildcardsExpression() {
        properties.put("prefix1.key1", "value1");
        properties.put("prefix2.*", "${@'prefix1'}");
        properties = properties.evaluateReferences(true);
        assertEquals(2, properties.size());
        assertEquals("value1", properties.get("prefix1.key1"));
        assertEquals("value1", properties.get("prefix2.key1"));
    }

    public void testLoadWithAliases() {
        properties.load(URLs.toURL(getTestCaseResource("aliased.properties")));

        Object value = properties.get("some.long.thing.key");
        assertNotNull(value);
        assertEquals("${some.long.thing.value}", value);
        value = properties.get("some.long.thing.key2");
        assertNotNull(value);
        assertEquals("${@some.long.thing.value2 }", value);
    }

    public void testEvaluateWithEvaluator() {
        properties.put("target", "${basedir}/temp");
        properties = properties.evaluateReferences(new AnnotatedProperties.PropertyEvaluator() {
                    public boolean canEvaluate(String key) {
                        return key.equals("basedir");
                    }

                    public String evaluate(String key) {
                        return "/root";
                    }
                }, true);
        assertEquals("/root/temp", properties.get("target"));
    }

    public void testEvaluate() {
        properties.put("basedir", "/root");
        properties.put("target", "${basedir}/temp");
        properties = properties.evaluateReferences(true);
        assertEquals("/root/temp", properties.get("target"));

        properties.put("target", "${@basedir}/temp");
        properties = properties.evaluateReferences(true);
        assertEquals("/root/temp", properties.get("target"));
    }

    public void testEvaluateUnreferenced() {
        properties.put("target", "${basedir}/temp");
        properties = properties.evaluateReferences(false);

        String value = properties.getProperty("target");
        System.out.println(value);
        assertNull(value);
    }

    public void testEvaluateUnreferencedFail() {
        properties.put("target", "${basedir}/temp");

        try {
            properties.evaluateReferences(true);
            fail("Exception expected");
        } catch (BuildException e) {
            assertTrue(e.getMessage().indexOf("Undefined") != -1);
        }
    }

    public void testReplaceProperties() {
        properties.put("prefix.key1", "${prefix.key2}/temp");

        final String prefix = "some.prefix";
        properties = properties.replaceReferences(new PropertyProvider() {
                    public String getProperty(String key) {
                        String prefixMarker = "prefix.";

                        return key.startsWith(prefixMarker) ? (prefix + "." + key.substring(prefixMarker.length())) : key;
                    }
                });
        assertEquals("${some.prefix.key2}/temp", properties.get("some.prefix.key1"));
    }

    public void testMixedCase() {
        properties.put("mixedCase", "someValue");
        properties.put("someKey", "${@mixedCase}");
        properties.put("otherKey", "${mixedCase}");
        properties.put("thirdKey", "${MIXEDCASE}");
        properties = properties.evaluateReferences(true);
        assertEquals("someValue", properties.get("someKey"));
        assertEquals("someValue", properties.get("somekey"));
        assertEquals("someValue", properties.get("SOMEKEY"));
        assertEquals("someValue", properties.get("otherKey"));
        assertEquals("someValue", properties.get("OtherKEY"));
        assertEquals("someValue", properties.get("thirdKey"));
    }

    protected String replaceRefs(String value) {
        return AnnotatedProperties.replaceReferences(new PropertyProvider() {
                public String getProperty(String key) {
                    return (String)replacements.get(key);
                }
            }, value);
    }

    public void testDump() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        properties.put("hello", "there");
        properties.put("from", "me");
        properties.dump(new PrintStream(out));

        String nl = System.getProperty("line.separator");
        assertEquals("from -> me" + nl + "hello -> there" + nl, out.toString());
    }

    public void testPutAll() {
        AnnotatedProperties props = new AnnotatedProperties();
        props.put("key1", "value1");
        props.put("key2", "value2");
        properties.putAll(props);
        assertEquals(properties, props);
    }

    public void testRemove() {
        properties.put("UPPER", "value");
        properties.remove("upper");
        assertEquals(0, properties.size());
    }
}
