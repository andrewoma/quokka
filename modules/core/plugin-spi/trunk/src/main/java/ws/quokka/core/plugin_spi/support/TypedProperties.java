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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
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
 *
 */
public class TypedProperties {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map properties;
    private Project project;
    private Converter converter = new Converter();
    private String prefix;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public TypedProperties(String prefix) {
        this.prefix = prefix;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public String getString(String key) {
        return getString(key, null, true);
    }

    public String getString(String key, String defaultValue) {
        return getString(key, defaultValue, false);
    }

    private String getString(String key, String defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : value;
    }

    private String getProperty(String key, boolean mandatory) {
        String value = (String)properties.get(prefix + key);
        Assert.isTrue(!mandatory || (value != null), "Mandatory property '" + key + "' has not been set.");

        return value;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false, true);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, defaultValue, false);
    }

    private boolean getBoolean(String key, boolean defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toBoolean(value);
    }

    public Path getPath(String key) {
        return getPath(key, null, true);
    }

    public Path getPath(String key, Path defaultValue) {
        return getPath(key, defaultValue, false);
    }

    private Path getPath(String key, Path defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toPath(value);
    }

    public File getFile(String key) {
        return getFile(key, null, true);
    }

    public File getFile(String key, File defaultValue) {
        return getFile(key, defaultValue, false);
    }

    private File getFile(String key, File defaultValue, boolean mandatory) {
        String value = getProperty(key, mandatory);

        return (value == null) ? defaultValue : converter.toFile(value);
    }

    public ResourceCollection getResourceCollection(String key, boolean mandatory) {
        ResourceCollection rc;

        // Look for a reference first
        String refId = getString(key + ".refid", null, false);

        if (refId != null) {
            return (ResourceCollection)project.getReference(refId);
        }

        // Try getting a fileset
        rc = getFileSet(key);

        if (rc != null) {
            return rc;
        }

        Assert.isTrue(!mandatory, "Mandatory property '" + key + "' has not been set.");

        return null;
    }

    public TypedProperties sub(String suffix) {
        TypedProperties props = new TypedProperties(prefix + suffix);
        props.setProperties(properties);
        props.setProject(project);

        return props;
    }

    public FileSet getExistingFileSet(String key) {
        FileSet fileSet = getFileSet(key);

        if ((fileSet != null) && fileSet.getDir(project).exists() && fileSet.getDir(project).isDirectory()) {
            return fileSet;
        }

        return null;
    }

    public FileSet getFileSet(String key) {
        return getFileSet(key, false);
    }

    public FileSet getFileSet(String key, boolean mandatory) {
        FileSet fileSet = (FileSet)project.createDataType("fileset");
        TypedProperties setProperties = sub(key + ".set.");
        Setter setter = new Setter(setProperties);
        setter.set(fileSet,
            new String[] {
                "dir", "file", "defaultExcludes", "includes", "includesFile", "excludes", "excludesFile",
                "caseSensitive", "followSymLinks"
            });

        if (fileSet.getDir(project) == null) {
            if (mandatory) {
                throw new BuildException(prefix + key + " does not refer to a valid fileset");
            } else {
                return null; // Neither dir or file attributes have been supplied, so not valid
            }
        }

        return fileSet;
    }

    public Map getMap(String root, boolean mandatory, Class valueType) {
        Map map = new HashMap();

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            String prefix = this.prefix + root + "[";

            if (key.startsWith(prefix)) {
                int keyEnd = key.indexOf(']');
                String mapKey = key.substring(prefix.length(), keyEnd);
                TypedProperties mapValue = (TypedProperties)map.get(mapKey);

                if (mapValue == null) {
                    mapValue = new TypedProperties("");
                    mapValue.setProject(project);
                    mapValue.setProperties(new HashMap());
                    map.put(mapKey, mapValue);
                }

                String newKey = (key.length() > (keyEnd + 1))
                    ? (((valueType == null) ? "" : "value.") + key.substring(keyEnd + 1, key.length())) : "value";
                mapValue.properties.put(newKey, entry.getValue());
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
                } else if (valueType.equals(File.class)) {
                    value = properties.getFile("value");
                } else if (valueType.equals(Boolean.class)) {
                    value = (properties.getBoolean("value")) ? Boolean.TRUE : Boolean.FALSE;
                } else if (valueType.equals(Path.class)) {
                    value = properties.getPath("value");
                } else if (valueType.equals(ResourceCollection.class)) {
                    value = properties.getResourceCollection("value", mandatory);
                }

                entry.setValue(value);
            }
        }

        return map;
    }

    public void dump() {
        SortedMap sorted = new TreeMap(properties);

        for (Iterator i = sorted.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
    }

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

    public static List commaSepList(String string) {
        List tokens = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(string, ",");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            tokens.add(token.trim());
        }

        return tokens;
    }

    public String toString() {
        return properties.toString();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public class Converter {
        IntrospectionHelper helper = IntrospectionHelper.getHelper(Converter.class);
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
