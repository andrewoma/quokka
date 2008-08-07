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


package ws.quokka.core.test;

import junit.framework.TestCase;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * AbstractTest is a base class for unit and integration tests
 */
public class AbstractTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Properties properties;
    private Properties itestProperties;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getClassName() {
        return getClass().getName().substring(getClass().getName().lastIndexOf('.') + 1);
    }

    public File getTestResource(String relativePath) {
        File file = new File(getModuleHome(), normalise("/src/test/resources/" + relativePath));

        if (!file.exists()) {
            throw new RuntimeException("File does not exist: " + file.getPath());
        }

        return file;
    }

    public File getModuleHome() {
        return new File(normalise(getITestProperties().getProperty("moduleHome")));
    }

    public String normalise(String path) {
        return path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }

    public String getModuleId(String className) {
        // TODO: make this configurable for other projects
        int index = -1;

        for (int i = 0; i < 4; i++) {
            index = className.indexOf(".", index + 1);
        }

        return className.substring(className.indexOf(".") + 1, index);
    }

    public File getTestCaseResource(String relativePath) {
        return getTestResource(getClassName() + "/" + relativePath);
    }

    public Properties getITestProperties() {
        if (itestProperties == null) {
            itestProperties = loadProperties("itest.properties");
        }

        return itestProperties;
    }

    public Properties getProperties() {
        if (properties == null) {
            properties = loadProperties("test.properties");
        }

        return properties;
    }

    public Properties loadProperties(String name) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);

        if (in == null) {
            throw new RuntimeException(name + " cannot be found on the classpath");
        }

        try {
            try {
                Properties props = new Properties();
                props.load(in);

                return props;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void assertExists(File file) {
        assertTrue("File does not exist: " + file, (file != null) && file.exists());
    }

    public void assertNotExists(File file) {
        assertTrue("File exists: " + file, (file != null) && !file.exists());
    }

    public int fileCount(File directory) {
        return directory.listFiles().length;
    }

    public boolean contains(File file, String[] tokens)
            throws IOException {
        return contains(file.toURI().toURL(), tokens);
    }

    public boolean contains(URL url, String[] tokens) throws IOException {
        List notFound = new ArrayList(Arrays.asList(tokens));
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        try {
            while (true) {
                String line = reader.readLine();

                if (line == null) {
                    break;
                }

                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];

                    if (line.indexOf(token) != -1) {
                        notFound.remove(token);
                    }
                }
            }
        } finally {
            reader.close();
        }

        return notFound.size() == 0;
    }

    public void assertContentEquals(File file1, File file2)
            throws IOException {
        assertContentEquals(file1.toURI().toURL(), file2.toURI().toURL());
    }

    public void assertEqualMaps(String string, Map map1, Map map2) {
        Set additionalKeys = new HashSet(map1.keySet());

        if (!map1.equals(map2)) {
            for (Iterator i = map1.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                Object otherValue = map2.get(key);

                if (otherValue == null) {
                    System.err.println("map2 missing: " + key);
                } else {
                    additionalKeys.remove(key);

                    if (!value.equals(otherValue)) {
                        System.err.println("values don't match for: " + key + ", map1=" + value + ", map2="
                            + otherValue);
                    }
                }
            }

            for (Iterator i = additionalKeys.iterator(); i.hasNext();) {
                String key = (String)i.next();
                System.err.println("map2 has additional keys: " + key + " -> " + map2.get(key));
            }
        }

        assertEquals(string, map1, map2);
    }

    public void assertContentEquals(URL url1, URL url2)
            throws IOException {
        BufferedInputStream in1 = null;
        BufferedInputStream in2 = null;

        try {
            in1 = new BufferedInputStream(url1.openStream());
            in2 = new BufferedInputStream(url1.openStream());

            byte[] buffer1 = new byte[4096];
            byte[] buffer2 = new byte[4096];

            while (true) {
                int bytes1 = in1.read(buffer1);
                int bytes2 = in2.read(buffer2);
                String message = "URL contents do not match: url1=" + url1.toString() + ", url2=" + url2.toString();
                assertEquals(message, bytes1, bytes2);

                if (bytes1 == -1) {
                    assertTrue(bytes2 == -1);

                    break;
                }

                for (int i = 0; i < bytes1; i++) {
                    assertEquals(message, buffer1[i], buffer2[i]);
                }
            }
        } finally {
            try {
                if (in1 != null) {
                    in1.close();
                }
            } finally {
                if (in2 != null) {
                    in2.close();
                }
            }
        }
    }

    public void assertContainsEntries(File file, String[] entries) {
        try {
            JarFile jarFile = new JarFile(file);

            for (int i = 0; i < entries.length; i++) {
                String entry = entries[i];
                JarEntry jarEntry = jarFile.getJarEntry(entry);
                assertNotNull("Cannot find entry '" + entry + "' in " + file.getPath(), jarEntry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
