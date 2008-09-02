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
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.util.FileUtils;

import org.xml.sax.Locator;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.util.xml.LocatorImpl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;


/**
 * AnnotatedProperties extends Properties with the following functionality:
 * <ul>
 * <li>A custom properties file parser is used to remember both the line number
 * and preceeding comments for each property entry.</li>
 * <li>Aliases can be defined with a special comment of #{&lt;aliasId&gt;=&lt;value&gt;}
 * and subsequently used in property definitions using a key of &lt;aliasId&gt;!subkey=value.</li>
 * <li>Properties can reference one another using ${reference} syntax</li>
 * <li>Supports wildcard properties for copying a set of properties from
 * one prefix to another. e.g. "copy.*=source" will copy any properties that start with "source." to
 * "copy.".</li>
 * <li>Supports expressions such as ${@ifdef(quokka.global.javac.source?'hello':'goodbye')}.
 * See {@link ws.quokka.core.util.PropertyExpressionParser} for more information on expressions</li>
 * <p/>
 * Custom properties parser is based on the xwork 2 implementation of LocationProperties
 * and subject to its copyright
 */
public class AnnotatedProperties extends Properties {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map annotations = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Loads the properties from the given URL, keeping track of line numbers
     */
    public AnnotatedProperties load(final URL url) {
        return (AnnotatedProperties)new ExceptionHandler() {
                public Object run() throws Exception {
                    InputStream in;

                    // JDK 1.2 can't hanlde URLs with escaped spaces
                    if (url.toExternalForm().startsWith("file:")) {
                        in = new BufferedInputStream(new FileInputStream(FileUtils.getFileUtils().fromURI(url
                                        .toExternalForm())));
                    } else {
                        in = url.openStream();
                    }

                    try {
                        return load(url, new BufferedReader(new InputStreamReader(in)));
                    } finally {
                        in.close();
                    }
                }
            }.soften();
    }

    /**
     * Loads the properties from the given reader
     * @param url is only used to mark the location
     */
    public AnnotatedProperties load(URL url, Reader reader)
            throws IOException {
        Map aliases = new HashMap();
        PropertiesReader pr = new PropertiesReader(reader);

        while (pr.nextProperty()) {
            Locator locator = getLocator(pr, url);

            for (Iterator i = pr.getCommentLines().iterator(); i.hasNext();) {
                String comment = (String)i.next();

                if (comment.startsWith("#{") && comment.endsWith("}")) {
                    String directive = comment.substring(2, comment.length() - 1);
                    String[] tokens = Strings.trim(Strings.split(directive, "="));
                    Assert.isTrue(tokens.length == 2, locator, "Directive should be in the format '#{<aliasId>=<value>}");
                    aliases.put(tokens[0], tokens[1]);
                }
            }

            AliasExpander aliasExpander = new AliasExpander(aliases);
            String name = aliasExpander.getProperty(pr.getPropertyName());
            String val = replaceReferences(aliasExpander, pr.getPropertyValue());

            Annotations annotations = new Annotations();
            annotations.put(AnnotatedObject.LOCATOR, locator);
            annotations.put("initial", val);
            setProperty(name, val, annotations);
        }

        return this;
    }

    private Locator getLocator(PropertiesReader pr, final URL url) {
        int line = pr.getLineNumber();
        String systemId = (String)new ExceptionHandler() {
                    public Object run() throws UnsupportedEncodingException {
                        return org.apache.tools.ant.launch.Locator.encodeURI(url.toString());
                    }
                }.soften();

        return new LocatorImpl(null, systemId, line, 0);
    }

    /**
     * Sets the property along with annotations
     */
    public Object setProperty(String key, String value, Annotations annotations) {
        Object object = super.setProperty(key, value);

        if (annotations != null) {
            this.annotations.put(key, annotations);
        }

        return object;
    }

    /**
     * Returns the annotations for a given key
     */
    public Annotations getAnnotation(String key) {
        return (Annotations)annotations.get(key.toLowerCase());
    }

    /**
     * The equivalent of the standard putAll, but copies across annotations as well
     */
    public void putAll(AnnotatedProperties properties) {
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            super.put(entry.getKey(), entry.getValue());
            annotations.put(((String)entry.getKey()).toLowerCase(), properties.getAnnotation((String)entry.getKey()));
        }
    }

    public synchronized Object put(Object key, Object value) {
        Assert.isTrue(value != null, "Value is null for key=" + key);

        return super.put(key, value);
    }

    /**
     * Supports renaming of references
     * @param provider the provider will be called with the value of a reference and should return
     * the value to rename the reference to
     */
    public AnnotatedProperties replaceReferences(PropertyProvider provider) {
        AnnotatedProperties properties = new AnnotatedProperties();

        for (Iterator i = entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            properties.setProperty(provider.getProperty((String)entry.getKey()),
                replaceReferences(provider, (String)entry.getValue()), getAnnotation((String)entry.getKey()));
        }

        return properties;
    }

    protected static String replaceReferences(PropertyProvider provider, String value) {
        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        ProjectHelper.parsePropertyString(value, fragments, propertyRefs);

        StringBuffer replaced = new StringBuffer();
        Iterator refs = propertyRefs.iterator();

        for (Iterator i = fragments.iterator(); i.hasNext();) {
            String fragment = (String)i.next();

            if (fragment == null) {
                // Insert reference
                String ref = (String)refs.next();
                String replacedRef;

                if (ref.startsWith("@")) {
                    PropertyExpressionParser parser = new PropertyExpressionParser(ref);
                    replacedRef = parser.replace(provider);
                } else {
                    replacedRef = provider.getProperty(ref);
                }

                replaced.append("${").append(replacedRef).append("}");
            } else {
                replaced.append(fragment);
            }
        }

        return replaced.toString();
    }

    /**
     * Returns a new properties object will all references expanded into their full form
     * @param failIfUnreferenced if true, a reference to an undefined property will result in an exception,
     * otherwise the value is expanded to a warning message
     */
    public AnnotatedProperties evaluateReferences(boolean failIfUnreferenced) {
        return evaluateReferences(new PropertyEvaluator() {
                public boolean canEvaluate(String key) {
                    return false;
                }

                public String evaluate(String key) {
                    return null;
                }
            }, failIfUnreferenced);
    }

    /**
     * Returns a new properties object will all references expanded into their full form
     * @param evaluator all the values of references to be supplied from an external source, not just
     * from other property values
     * @param failIfUnreferenced if true, a reference to an undefined property will result in an exception,
     * otherwise the value is expanded to a warning message
     */
    public AnnotatedProperties evaluateReferences(PropertyEvaluator evaluator, boolean failIfUnreferenced) {
        expandWildcards(evaluator, failIfUnreferenced);

        AnnotatedProperties replaced = new AnnotatedProperties();
        List stack = new ArrayList();

        for (Iterator i = entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            stack.clear();
            evaluateReference(evaluator, (String)entry.getKey(), replaced, failIfUnreferenced, stack);
        }

        return replaced;
    }

    private void expandWildcards(PropertyEvaluator evaluator, boolean failIfUnreferenced) {
        // TODO: Handle any interdependencies between wildcard properties. For now it assumes that
        // the wildcard prefixes can be evaluated independently
        // Pre-process map to prevent concurrent modifications. Evaluate the RHS of wildcards to get prefix
        Map wildcards = new HashMap();

        for (Iterator i = entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            if (key.endsWith(".*")) {
                wildcards.put(key.substring(0, key.length() - 2), evaluate(evaluator, failIfUnreferenced, key) + ".");
                i.remove();
            }
        }

        // Copy across wild cards entries
        Map newEntries = new HashMap();

        for (Iterator i = entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            for (Iterator j = wildcards.entrySet().iterator(); j.hasNext();) {
                Map.Entry wcEntry = (Map.Entry)j.next();
                String wcKey = (String)wcEntry.getKey();
                String wcPrefix = (String)wcEntry.getValue();

                if (key.startsWith(wcPrefix)) {
                    String newKey = wcKey + key.substring(wcPrefix.length() - 1);

                    if (!containsKey(newKey)) {
                        newEntries.put(newKey, value);
                    }
                }
            }
        }

        putAll(newEntries);
    }

    private String evaluate(PropertyEvaluator evaluator, boolean failIfUnreferenced, String key) {
        AnnotatedProperties temp = new AnnotatedProperties();
        evaluateReference(evaluator, key, temp, failIfUnreferenced, new ArrayList());

        return (String)temp.get(key);
    }

    private String evaluateReference(PropertyEvaluator evaluator, String key, AnnotatedProperties evaluated,
        boolean failIfUnreferenced, List stack) {
        stack.add(key);

        // Look for an already evaluated value
        String value = (String)evaluated.get(key);

        if (value != null) {
            return value;
        }

        // Get the raw value before evaluation
        value = (String)get(key);

        // Process expressions
        boolean undefined = false;

        if ((value != null) && (value.indexOf("${") != -1)) { // Test first to reduce overhead

            Vector fragments = new Vector();
            Vector propertyRefs = new Vector();
            ProjectHelper.parsePropertyString(value, fragments, propertyRefs);

            StringBuffer expanded = new StringBuffer();
            Iterator refs = propertyRefs.iterator();

            for (Iterator i = fragments.iterator(); i.hasNext();) {
                String fragment = (String)i.next();

                if (fragment == null) {
                    // Insert reference
                    String ref = (String)refs.next();
                    String evaluatedRef;

                    if (ref.startsWith("@")) {
                        PropertyExpressionParser parser = new PropertyExpressionParser(ref);
                        evaluatedRef = parser.evaluate(new Provider(evaluator, evaluated, false, stack));

                        if ((fragments.size() == 1) && (propertyRefs.size() == 1)) {
                            failIfUnreferenced = false; // Allow property to be dropped as null must have been returned from setifdef
                        }
                    } else if (evaluator.canEvaluate(ref)) {
                        evaluatedRef = evaluator.evaluate(ref);
                    } else {
                        evaluatedRef = evaluateReference(evaluator, ref, evaluated, failIfUnreferenced, stack);
                    }

                    if (evaluatedRef == null) {
                        undefined = true;

                        break;
                    }

                    expanded.append(evaluatedRef);
                } else {
                    expanded.append(fragment);
                }
            }

            value = expanded.toString();
        }

        if ((value == null) || undefined) {
            if (failIfUnreferenced) {
                StringBuffer sb = new StringBuffer();

                for (int i = 0; i < (stack.size() - 1); i++) {
                    String stackKey = (String)stack.get(i);
                    Annotations annotations = getAnnotation(stackKey);
                    Locator locator = null;

                    if (annotations != null) {
                        locator = (Locator)annotations.get(AnnotatedObject.LOCATOR);
                    }

                    sb.append("\n   ").append(stackKey).append(" -> ").append(get(stackKey)).append((locator == null)
                        ? "" : (": " + locator));
                }

                throw new BuildException("Undefined property: '" + key + "': Stack of references follows:" + sb);
            } else {
                //                System.out.println("Dropping property as it references undefined properties: " + key);
                return null;

                //                return "Undefined value (references undefined properties in the project)";
            }
        }

        evaluated.put(key, value);

        return value;
    }

    /**
     * Writes out the sorted list of properties contained in this object to the print stream provided
     */
    public void dump(Writer writer) {
        Map properties = new TreeMap(this);
        String ls = System.getProperty("line.separator");

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();

            try {
                writer.write(entry.getKey() + " -> " + entry.getValue() + ls);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }

    //~ Inner Interfaces -----------------------------------------------------------------------------------------------

    /**
     * PropertyEvaluator allows references in properties to be expanded using a source external to the
     * properties file itself.
     */
    public static interface PropertyEvaluator {
        boolean canEvaluate(String key);

        String evaluate(String key);
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * AliasExpander is a property provider that automatically expands aliases
     */
    static class AliasExpander implements PropertyProvider {
        private Map aliases;

        public AliasExpander(Map aliases) {
            this.aliases = aliases;
        }

        public String getProperty(String propertyName) {
            String result = propertyName;
            String[] tokens = Strings.trim(Strings.split(propertyName, "!"));

            if (tokens.length == 2) {
                String prefix = (String)aliases.get(tokens[0]);
                Assert.isTrue(prefix != null, "Property references a alias that is not defined: " + propertyName);
                result = prefix + tokens[1];
            }

            return result;
        }
    }

    /**
     * Provider is for internal use and aids the evaluation of properties
     */
    class Provider implements PropertyProvider {
        private AnnotatedProperties evaluated;
        private boolean failIfUnreferenced;
        private List stack;
        private PropertyEvaluator evaluator;

        public Provider(PropertyEvaluator evaluator, AnnotatedProperties evaluated, boolean failIfUnreferenced,
            List stack) {
            this.evaluator = evaluator;
            this.evaluated = evaluated;
            this.failIfUnreferenced = failIfUnreferenced;
            this.stack = stack;
        }

        public String getProperty(String key) {
            try {
                return evaluateReference(evaluator, key, evaluated, failIfUnreferenced, stack);
            } catch (BuildException e) {
                if (e.getMessage().startsWith("Undefined property")) {
                    return null; // Null means undefined
                }

                throw e;
            }
        }
    }
}
