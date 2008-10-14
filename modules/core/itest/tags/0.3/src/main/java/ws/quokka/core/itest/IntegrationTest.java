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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * IntegrationTest is a base class for running Quokka integration tests.
 * It runs the
 */
public abstract class IntegrationTest extends AbstractTest {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static FilteredClassLoader classLoader;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    protected Map results;
    protected Properties properties = new Properties();
    protected Properties pluginState = new Properties();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Runs the default target for the given test project
     */
    protected void ant(String target) {
        ant(getClassName(), new String[] { target });
    }

    /**
     * Runs the default target for the given test project
     */
    protected void ant(String testProject, String target) {
        ant(testProject, new String[] { target });
    }

    /**
     * Runs the given targets for the test project specified
     */
    protected void ant(String[] targets) {
        ant(getClassName(), targets);
    }

    /**
     * Runs the given targets for the test project specified
     */
    protected void ant(String testProject, String[] targets) {
        // Override the output to go to this module's output
        properties.put("q.project.targetDir", getTargetDir(testProject));
        properties.put("q.project.targetDir", getTargetDir(testProject));
        properties.put("q.repositoryOverride", "itest");
        properties.put("q.repo.itest.url", "ws.quokka.core.itest.IntegrationTestRepository");

        ant(getBuildFile(testProject), targets);
    }

    public void setLogLevel(int level) {
        properties.setProperty("q.debugger.logLevel", Integer.toString(level));
    }

    protected void deleteDir(File dir) {
        File[] files = dir.listFiles();

        for (int i = 0; (files != null) && (i < files.length); i++) {
            File file = files[i];

            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                assertTrue("Cannot delete: " + file.getPath(), file.delete());
            }
        }
    }

    protected void clean() {
        deleteDir(new File(getTargetDir()));
    }

    protected String getTargetDir() {
        return normalise(new File(getModuleHome(), "target/integration-test/" + getClassName()).getAbsolutePath());
    }

    protected String getTargetDir(String testProject) {
        return normalise(new File(getModuleHome(), "target/integration-test/" + testProject).getAbsolutePath());
    }

    /**
     * Runs the given targets for the test project specified
     */
    protected void ant(File buildFile, String[] targets) {
        String moduleClassPathId = "q.classpath." + getModuleId(getClass().getName());

        try {
            properties.putAll(getITestProperties());
            addProperties(properties);

            // Set up a system property to add instrumented classes and their associated .jar if specified
            String testPath = System.getProperty("q.junit.instrumentCompiledOutput");

            if (testPath != null) {
                // The instrumentTestPath has already been added to the JVM forked to run these tests
                // Do not add again or Cobertura fails to obtain locks properly
                // testPath += (File.pathSeparator + System.getProperty("q.junit.instrumentTestPath"));
                System.setProperty(moduleClassPathId, testPath);
//                System.out.println("Skipping adding cobertura");

//                System.out.println("Setting module class path: moduleClasspathId=" + moduleClassPathId + ", value=" + testPath);
            }

            // Set up URLs
            if (classLoader == null) {
                // Cache class loader as Cobertura can't exist on multiple loaders within one JVM
                classLoader = new FilteredClassLoader(getCoreClassPath(getITestProperties().getProperty("itest.classPath")),
                        getClass().getClassLoader(), getClassLoaderFilter());
            }

            ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

            try {
                buildFile = filterBuildFile(buildFile);
                Thread.currentThread().setContextClassLoader(classLoader);

                Class clazz = classLoader.loadClass("ws.quokka.core.itest.AntRunner");
                Object antRunner = clazz.newInstance();
                Method method = clazz.getMethod("run",
                        new Class[] { File.class, List.class, Properties.class, Properties.class });
                results = (Map)method.invoke(antRunner,
                        new Object[] { buildFile, Arrays.asList(targets), properties, pluginState });
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = ((InvocationTargetException)e).getTargetException();
                }

                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }

                throw new RuntimeException(e);
            } finally {
                buildFile.delete();
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        } finally {
            System.setProperty(moduleClassPathId, "");
        }
    }

    /**
     * Replace @moduleVersion@ tokens with the real thing. Otherwise it's very easy to forget to
     * increment the versions between releases
     */
    protected File filterBuildFile(File buildFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(buildFile));

        try {
            File temp = new File(buildFile.getParentFile(), buildFile.getName().substring(1)); // Strips the first char from the name
            temp.deleteOnExit();

            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

            try {
                while (true) {
                    String line = reader.readLine();

                    if (line == null) {
                        break;
                    }

                    line = line.replaceAll("\\@moduleVersion\\@", getITestProperties().getProperty("moduleVersion"));
                    writer.write(line + "\n");
                }

                return temp;
            } finally {
                writer.close();
            }
        } finally {
            reader.close();
        }
    }

    public void addProperties(Properties properties) {
    }

    protected Filter getClassLoaderFilter() {
        return new Filter() {
                public boolean loadFromParent(String name) {
                    return IntegrationTest.this.loadFromParent(name);
                }
            };
    }

    protected boolean loadFromParent(String name) {
        return !name.startsWith("org.apache.tools.ant") && !name.startsWith("ws.quokka")
        && !name.startsWith("de.hunsicker");
    }

    protected URL[] getCoreClassPath(String path) {
        List urls = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(path, ";" + File.pathSeparator);

        //        System.out.println("\n\nIntegration test class path:");
        while (tokenizer.hasMoreTokens()) {
            String pathElement = tokenizer.nextToken();

//                                System.out.println(pathElement);
            assertTrue("Core classpath element does not exist: " + normalise(pathElement),
                new File(normalise(pathElement)).exists());
            urls.add(toURL(pathElement));
        }

        File toolsJar = getToolsJar();

        if (toolsJar != null) {
            urls.add(toURL(toolsJar.getPath()));
        }

        return (URL[])urls.toArray(new URL[urls.size()]);
    }

    protected URL toURL(String pathElement) {
        try {
            assertTrue(new File(normalise(pathElement)).exists());

            return new File(pathElement).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public File getBuildFile(String buildFile) {
        return getTestResource(buildFile + "/" + "ibuild-quokka.xml");
    }

    public Map getAntProperties() {
        return (Map)results.get("antProperties");
    }

    public File getTarget() {
        return getFile("q.lifecycle.target");
    }

    public File getFile(String antPropertyKey) {
        return new File(normalise((String)getAntProperties().get(antPropertyKey)));
    }

    public void assertThrowsException(String testProject, String target, String expectedError) {
        try {
            ant(testProject, target);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf(expectedError) != -1);
        }
    }

    public File getToolsJar() {
        // Fragment taken from org.apache.tools.ant.launch.Locator
        // based on java.home setting
        String javaHome = System.getProperty("java.home");
        File toolsJar = new File(javaHome + "/lib/tools.jar");

        if (toolsJar.exists()) {
            // Found in java.home as given
            return toolsJar;
        }

        if (javaHome.toLowerCase(Locale.US).endsWith(File.separator + "jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
            toolsJar = new File(javaHome + "/lib/tools.jar");
        }

        if (!toolsJar.exists()) {
//            System.out.println("Unable to locate tools.jar. " + "Expected to find it in " + toolsJar.getPath());
            return null;
        }

        return toolsJar;
    }
}
