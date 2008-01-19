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


package ws.quokka.core.itest;

import ws.quokka.core.test.AbstractTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Properties;


/**
 *
 */
public class FilteredClassLoaderTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testExcludes() throws MalformedURLException {
        URL url = new File(System.getProperty("user.home")).toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        return !name.equals("java.awt.TextField");
                    }
                });

        try {
            loader.loadClass("java.awt.TextField");
        } catch (ClassNotFoundException e) {
            return;
        }

        fail("ClassNotFoundException expected");
    }

    public void testIncludes() throws MalformedURLException, ClassNotFoundException {
        URL url = new File(System.getProperty("user.home")).toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        return name.equals("java.awt.TextField");
                    }
                });
        loader.loadClass("java.awt.TextField");
    }

    public void testAvailOnBoth() throws MalformedURLException, ClassNotFoundException {
        File file = new File(getModuleHome(), "target/test-compile");
        assertExists(file);

        URL url = file.toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        return !name.equals("ws.quokka.core.itest.FilteredClassLoaderTest$SomeClass");
                    }
                });
        Class clazz = loader.loadClass("ws.quokka.core.itest.FilteredClassLoaderTest$SomeClass");
        assertEquals(loader, clazz.getClassLoader());
    }

    public void testLoadedPrior() throws MalformedURLException, ClassNotFoundException {
        Class.forName("ws.quokka.core.itest.FilteredClassLoaderTest$SomeClass");

        URL url = new File(getModuleHome(), "target/test-compile").toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        return !name.equals("ws.quokka.core.itest.FilteredClassLoaderTest$SomeClass");
                    }
                });
        Class clazz = loader.loadClass("ws.quokka.core.itest.FilteredClassLoaderTest$SomeClass");
        assertEquals(loader, clazz.getClassLoader());
    }

    public void testResources() throws IOException {
        URL url = new File(getModuleHome(), "src/test/resources").toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        //                System.out.println(name);
                        //                return !name.equals("some.properties");
                        return true;
                    }
                });
        InputStream in = loader.getResourceAsStream("some.properties");
        assertNotNull(in);

        Properties properties = new Properties();
        properties.load(in);
        assertEquals("value", properties.getProperty("key"));
    }

    public void testHierarchicalResources() throws IOException {
        URL url = new File(getModuleHome(), "src/test/resources").toURL();
        FilteredClassLoader loader = new FilteredClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader(),
                new Filter() {
                    public boolean loadFromParent(String name) {
                        //                System.out.println(name);
                        //                return !name.equals("org/quokka/other.properties");
                        return true;
                    }
                });
        InputStream in = loader.getResourceAsStream("org/quokka/other.properties");
        assertNotNull(in);

        Properties properties = new Properties();
        properties.load(in);
        assertEquals("value", properties.getProperty("key"));
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class SomeClass {
    }
}
