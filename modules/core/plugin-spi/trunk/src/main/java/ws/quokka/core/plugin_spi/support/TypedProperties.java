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


package ws.quokka.core.plugin_spi.support;

import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ResourceCollection;

import ws.quokka.core.bootstrap_util.Assert;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;


/**
 * TypedProperties provides convenience methods for acessing properties as typed values.
 * It supports simple types such as File and String, as well as more complicated structures
 * such as Lists, Maps and ResourceCollections. It can also verify that invalid keys
 * have not been set.
 */
public class TypedProperties {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String REFID = "refid";
    private static final String[] FILE_SET_ATTRIBUTES = new String[] {
            "dir", "file", "defaultExcludes", "includes", "includesFile", "excludes", "excludesFile", "caseSensitive",
            "followSymLinks"
        };
    private static final String[] FILE_LIST_ATTRIBUTES = new String[] { "dir", "files" };

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map properties;
    private Project project;
    private Converter converter = new Converter();
    private String prefix;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param prefix the prefix to automatically add to properties when accessed via get methods. The prefix
     * should have a trailing '.'
     * @param properties The underlying properties to retrieve values from
     * @param project The current Ant project
     */
    public TypedProperties(String prefix, Map properties, Project project) {
        this.prefix = prefix;
        this.properties = properties;
        this.project = project;

        // TODO: evaluate whether eagerly filtering the properties is a good idea
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Project getProject() {
        return project;
    }

    /**
     * Returns the value as a String that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public String getString(String key) {
        return getString(key, null, true);
    }

    /**
     * Returns the value as a String that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public String getString(String key, String defaultValue) {
        return getString(key, defaultValue, false);
    }

    /**
     * Returns the value as an int that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public int getInt(String key) {
        return Integer.parseInt(getString(key, null, true));
    }

    /**
     * Returns the value as an int that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null, false);

        return (value == null) ? defaultValue : Integer.parseInt(value);
    }

    /**
     * Returns the value as a long that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public long getLong(String key) {
        return Long.parseLong(getString(key, null, true));
    }

    /**
     * Returns the value as a long that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public long getLong(String key, long defaultValue) {
        String value = getString(key, null, false);

        return (value == null) ? defaultValue : Long.parseLong(value);
    }

    private String getString(String key, String defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : value;
    }

    private String getProperty(String key, boolean mandatory) {
        String value = (String)properties.get(prefix + key);
        Assert.isTrue(!mandatory || (value != null), "Mandatory property '" + prefix + key + "' has not been set.");

        return value;
    }

    /**
     * Returns the value as a boolean that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false, true);
    }

    /**
     * Returns the value as a boolean that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, defaultValue, false);
    }

    private boolean getBoolean(String key, boolean defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toBoolean(value);
    }

    /**
     * Returns the value as a Path that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public Path getPath(String key) {
        return getPath(key, null, true);
    }

    /**
     * Returns the value as a Path that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public Path getPath(String key, Path defaultValue) {
        return getPath(key, defaultValue, false);
    }

    private Path getPath(String key, Path defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toPath(value);
    }

    /**
     * Returns the value as a File that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public File getFile(String key) {
        return getFile(key, null, true);
    }

    /**
     * Returns the value as a File that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public File getFile(String key, File defaultValue) {
        return getFile(key, defaultValue, false);
    }

    private File getFile(String key, File defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toFile(value);
    }

    /**
     * Returns the value as a ResourceCollection. See {@link #getResourceCollection(String, org.apache.tools.ant.types.ResourceCollection)} )}
     * for more information.
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public ResourceCollection getResourceCollection(String key) {
        return getResourceCollection(key, null, true);
    }

    /**
     * Returns the value as a ResourceCollection that is associated with the given key, or
     * the default value if no value exists for the given key
     * <br>
     * Currently, it supports FileSets, FileLists, Paths and References using the
     * prefixes of "set", "list", "path" and "refid" respectively.
     * <br>
     * e.g. for a key of "rc", a property of "rc.set.dir=/tmp" would resolve to a FileSet
     */
    public ResourceCollection getResourceCollection(String key, ResourceCollection defaultValue) {
        return getResourceCollection(key, defaultValue, false);
    }

    private ResourceCollection getResourceCollection(String key, ResourceCollection defaultValue, boolean mandatory) {
        ResourceCollection rc;

        TypedProperties sub = sub(key + ".");

        // Look for a reference first
        String refId = sub.getString(REFID, null, false);

        if (refId != null) {
            Object object = project.getReference(refId);
            Assert.isTrue(object instanceof ResourceCollection,
                "The object referenced by '" + refId + "' from '" + prefix + key
                + ".refid' is not a resource collection");
            sub.verify(new Keys(), new Keys(REFID)); // Should be no other values if refid specified

            return (ResourceCollection)object;
        }

        // Try getting a fileset
        rc = getFileSet(key, null, false);

        if (rc != null) {
            sub.verify(new Keys(), new Keys("set.")); // Should be no other values than set

            return rc;
        }

        // Try getting a filelist
        rc = getFileList(key, null, false);

        if (rc != null) {
            sub.verify(new Keys(), new Keys("list.")); // Should be no other values than list

            return rc;
        }

        // Try getting a path
        rc = getPath(key + ".path", null, false);

        if (rc != null) {
            sub.verify(new Keys(), new Keys("path")); // Should be no other values than path

            return rc;
        }

        Assert.isTrue(!mandatory, "Mandatory property '" + key + "' has not been set.");

        return defaultValue;
    }

    /**
     * Returns the value as a FileList that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public FileList getFileList(String key) {
        return getFileList(key, null, true);
    }

    /**
     * Returns the value as a FileList that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public FileList getFileList(String key, FileList defaultValue) {
        return getFileList(key, defaultValue, false);
    }

    private FileList getFileList(String key, FileList defaultValue, boolean mandatory) {
        FileList fileList = (FileList)project.createDataType("filelist");
        TypedProperties setProperties = sub(key + ".list.");
        Setter setter = new Setter(setProperties);
        setter.set(fileList, FILE_LIST_ATTRIBUTES);

        if (fileList.getDir(project) == null) {
            Assert.isTrue(!mandatory, prefix + key + " does not refer to a valid filelist");

            return defaultValue; // Neither dir or file attributes have been supplied, so not valid
        }

        setProperties.verify(new Keys(FILE_LIST_ATTRIBUTES));

        return fileList;
    }

    /**
     * Returns a new TypedProperties instance that represents a subset of the properties
     * by adding the supplied suffix to the existing prefix
     */
    public TypedProperties sub(String suffix) {
        return new TypedProperties(prefix + suffix, properties, project);
    }

    /**
     * Returns the value as a FileSet that is associated with the given key
     * @throws org.apache.tools.ant.BuildException if no value exists for the given key
     */
    public FileSet getFileSet(String key) {
        return getFileSet(key, null, true);
    }

    /**
     * Returns the value as a FileSet that is associated with the given key, or the default value if
     * no value exists for the given key
     */
    public FileSet getFileSet(String key, FileSet defaultValue) {
        return getFileSet(key, defaultValue, false);
    }

    private FileSet getFileSet(String key, FileSet defaultValue, boolean mandatory) {
        FileSet fileSet = (FileSet)project.createDataType("fileset");
        TypedProperties setProperties = sub(key + ".set.");
        Setter setter = new Setter(setProperties);
        setter.set(fileSet, FILE_SET_ATTRIBUTES);

        if (fileSet.getDir(project) == null) {
            Assert.isTrue(!mandatory, prefix + key + " does not refer to a valid fileset");

            return defaultValue; // Neither dir or file attributes have been supplied, so not valid
        }

        setProperties.verify(new Keys(FILE_SET_ATTRIBUTES));

        return fileSet;
    }

    /**
     * Returns a Map of keys and value for the given root. Properties should be in
     * the format of prefix.root[key1]=value1.
     * @param root the key of the map.
     * @param mandatory if true, an exception will be thrown if the map is undefined, otherwise an empty map
     * will be returned
     * @param valueType if specified, the values will automatically be converted to the type given. If null,
     * it is assumed the value is a more complex type and TypedProperties will be returned containing the subset
     * of values that match each key under the root
     */
    public Map getMap(String root, boolean mandatory, Class valueType) {
        Map map = new HashMap();

        String prefix = this.prefix + root + "[";
        String invalid = this.prefix + root + ".";

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            if (key.startsWith(prefix)) {
                int keyEnd = key.indexOf(']');
                String mapKey = key.substring(prefix.length(), keyEnd);
                TypedProperties mapValue = (TypedProperties)map.get(mapKey);

                if (mapValue == null) {
                    mapValue = new TypedProperties("", new HashMap(), project);
                    map.put(mapKey, mapValue);
                }

                String newKey = (key.length() > (keyEnd + 1))
                    ? (((valueType == null) ? "" : "value.") + key.substring(keyEnd + 1, key.length())) : "value";
                mapValue.properties.put(newKey, entry.getValue());
            } else {
                Assert.isTrue(!key.startsWith(invalid), "Invalid property with same root as map or list: '" + key + "'");
            }
        }

        Assert.isTrue(!mandatory || (map.size() != 0), "Mandatory property '" + root + "' has not been set.");

        // Automatically convert to known types
        if (valueType != null) {
            for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                TypedProperties properties = (TypedProperties)entry.getValue();
                Object value = null;

                if (valueType.equals(String.class)) {
                    value = properties.getString("value");
                } else if (valueType.equals(FileSet.class)) {
                    value = properties.getFileSet("value");
                } else if (valueType.equals(FileList.class)) {
                    value = properties.getFileList("value");
                } else if (valueType.equals(File.class)) {
                    value = properties.getFile("value");
                } else if (valueType.equals(Boolean.class)) {
                    value = (properties.getBoolean("value")) ? Boolean.TRUE : Boolean.FALSE;
                } else if (valueType.equals(Path.class)) {
                    value = properties.getPath("value");
                } else if (valueType.equals(ResourceCollection.class)) {
                    value = properties.getResourceCollection("value");
                }

                entry.setValue(value);
            }
        }

        return map;
    }

    /**
     * Returns a list of properties in sequence. Properties should be in the form of prefix.root[index]=value
     * @param root the key of the list
     * @param mandatory if true, an exception will be thrown if the list is undefined, otherwise an empty list
     * will be returned
     * @param valueType if specified, the values will automatically be converted to the type given. If null,
     * it is assumed the value is a more complex type and TypedProperties will be returned containing the subset
     * of values that match each key under the root
     * @param allowSparse if true, the indexed values do not have to be a continual sequence starting
     * from zero. Missing values will be padded with null
     */
    public List getList(String root, boolean mandatory, Class valueType, boolean allowSparse) {
        Map map = getMap(root, mandatory, valueType);
        SortedMap intMap = new TreeMap();
        int maxIndex = -1;

        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Integer index = Integer.valueOf((String)entry.getKey());
            intMap.put(index, entry.getValue());
            maxIndex = Math.max(maxIndex, index.intValue());
        }

        if (!allowSparse && (intMap.size() != (maxIndex + 1))) {
            throw new RuntimeException("List does not contain a complete sequence of indexes starting from 0");
        }

        List list = new ArrayList(maxIndex + 1);

        for (int i = 0; i <= maxIndex; i++) {
            list.add(null);
        }

        for (Iterator i = intMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            list.set(((Integer)entry.getKey()).intValue(), entry.getValue());
        }

        return list;
    }

    /**
     * A convenience methods for splitting a string of comma separated values.
     * @param string the string to spit
     * @param trim if true, tokens will be trimmed of white space
     * @param preserveTokens if true, empty tokens are returned as empty strings. i.e. consecutive delimiters are
     * not merged
     * @return a list of tokens
     */
    public static List commaSepList(String string, boolean trim, boolean preserveTokens) {
        if (string == null) {
            return new ArrayList();
        }

        List tokens = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(string, ",", true);

        boolean previousWasDelimiter = true;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            token = trim ? token.trim() : token;

            boolean delimiter = token.equals(",");

            if (delimiter && preserveTokens && (previousWasDelimiter || !tokenizer.hasMoreTokens())) {
                tokens.add("");
            }

            previousWasDelimiter = delimiter;

            if (!delimiter) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * Returns a sorted list of all properties
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        SortedMap sorted = new TreeMap(properties);

        for (Iterator i = sorted.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            if (key.startsWith(prefix)) {
                sb.append(key + " -> " + entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Verifies that the only keys under this prefix are those specified in keys
     */
    public void verify(Keys keys) {
        verify(keys, new Keys());
    }

    /**
     * Verifies that the only keys under this prefix are those specified in keys with the
     * exclusion of those specified in exclusions
     */
    public void verify(Keys keys, Keys exclusions) {
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();

            if (key.startsWith(prefix)) {
                String keyVal = key.substring(prefix.length());
                Assert.isTrue(keys.toSet().contains(keyVal) || exclusion(keyVal, exclusions),
                    "Property '" + key + "' is not a valid property");
            }
        }
    }

    private boolean exclusion(String keyVal, Keys exclusions) {
        for (Iterator i = exclusions.toSet().iterator(); i.hasNext();) {
            String exclusion = (String)i.next();

            if (exclusion.endsWith(".") || exclusion.endsWith("[")) {
                if (keyVal.startsWith(exclusion)) {
                    return true;
                }
            } else {
                if (keyVal.equals(exclusion)) {
                    return true;
                }
            }
        }

        return false;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * Converter is a helper class to allow the use of Ant's built in type conversion mechanism.
     * It is necessary as the conversion only works when setting a value
     */
    protected class Converter {
        private IntrospectionHelper helper = IntrospectionHelper.getHelper(Converter.class);
        private boolean booleanValue;
        private Path path;
        private File file;

        public boolean toBoolean(String value) {
            setAttribute("booleanValue", value);

            return booleanValue;
        }

        public void setBooleanValue(boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public Path toPath(String value) {
            setAttribute("path", value);

            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public File toFile(String value) {
            setAttribute("file", value);

            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        private void setAttribute(String attribute, String value) {
            helper.setAttribute(project, this, attribute, value);
        }
    }
}
