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

import org.apache.tools.ant.BuildException;

import ws.quokka.core.test.AbstractTest;

import java.io.File;

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
 *
 */
public abstract class IntegrationTest extends AbstractTest {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String QUOKKA_REPOSITORY = "ws.quokka.core.repo_spi.Repository";
    private static final String QUOKKA_ITEST_REPOSITORY = "quokka.itest.repository";

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
        //        if (!properties.containsKey("quokka.project.targetDir")) {
        properties.put("quokka.project.targetDir", getTargetDir(testProject));

        //        }
        ant(getBuildFile(testProject), targets);
    }

    public void setLogLevel(int level) {
        properties.setProperty("quokka.debugger.logLevel", Integer.toString(level));
    }

    protected void deleteDir(File dir) {
        File[] files = dir.listFiles();

        for (int i = 0; (files != null) && (i < files.length); i++) {
            File file = files[i];

            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                //                System.out.println("Deleting: " + file.getPath());
                assertTrue("Cannot delete: " + file.getPath(), file.delete());
            }
        }

        //        System.out.println("Deleting dir: " + dir.getPath());
        //        assertTrue("Cannot delete: " + dir.getPath(), dir.delete());
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
        String originalRepositoryClass = null;
        String moduleClassPathId = "quokka.classpath." + getModuleId(getClass().getName());

        try {
            properties.putAll(getITestProperties());
            addProperties(properties);

            originalRepositoryClass = properties.getProperty(QUOKKA_REPOSITORY);
            properties.setProperty(QUOKKA_REPOSITORY, "ws.quokka.core.itest.IntegrationTestRepository");
            properties.put("quokka.core.itest.moduleHome", getModuleHome().getAbsolutePath());

            if (originalRepositoryClass != null) {
                properties.setProperty(QUOKKA_ITEST_REPOSITORY, originalRepositoryClass);
            }

            // Set up a system property to add instrumented classes and their associated .jar if specified
            String testPath = System.getProperty("quokka.junit.instrumentCompiledOutput");

            if (testPath != null) {
                testPath += (File.pathSeparator + System.getProperty("quokka.junit.instrumentTestPath"));
                System.setProperty(moduleClassPathId, testPath);

                //                System.out.println("Setting module class path: moduleClasspathId=" + moduleClassPathId + ", value=" + testPath);
            }

            // Set up URLs
            FilteredClassLoader loader = new FilteredClassLoader(getCoreClassPath(getITestProperties().getProperty("itest.classPath")),
                    getClass().getClassLoader(), getClassLoaderFilter());
            ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(loader);

                Class clazz = loader.loadClass("ws.quokka.core.itest.AntRunner");
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

                throw new BuildException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        } finally {
            System.setProperty(moduleClassPathId, "");

            if (originalRepositoryClass == null) {
                properties.remove(QUOKKA_REPOSITORY);
            } else {
                properties.put(QUOKKA_REPOSITORY, originalRepositoryClass);
            }
        }
    }

    public void addProperties(Properties properties) {
    }

    public Filter getClassLoaderFilter() {
        return new Filter() {
                public boolean loadFromParent(String name) {
                    return !name.startsWith("org.apache.tools.ant") && !name.startsWith("ws.quokka")
                    && !name.startsWith("net.sourceforge.cobertura") && !name.startsWith("de.hunsicker");
                }
            };
    }

    private URL[] getCoreClassPath(String path) {
        List urls = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(path, ";" + File.pathSeparator);

        //        System.out.println("\n\nIntegration test class path:");
        try {
            while (tokenizer.hasMoreTokens()) {
                String pathElement = tokenizer.nextToken();

//                                System.out.println(pathElement);
                assertTrue(new File(normalise(pathElement)).exists());
                urls.add(new File(pathElement).toURL());
            }

            File toolsJar = getToolsJar();

            if (toolsJar != null) {
                urls.add(toolsJar.toURL());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        return (URL[])urls.toArray(new URL[urls.size()]);
    }

    public File getBuildFile(String buildFile) {
        return getTestResource(buildFile + "/" + "ibuild-quokka.xml");
    }

    public Map getAntProperties() {
        return (Map)results.get("antProperties");
    }

    public File getTarget() {
        return getFile("quokka.lifecycle.target");
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
