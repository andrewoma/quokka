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


package ws.quokka.core.bootstrap.resources;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

import org.w3c.dom.Element;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.bootstrap_util.XmlParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class BootStrapResourcesParser extends XmlParser {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public BootStrapResources parse(File file, File librariesDir, File cacheDir) {
        try {
            return parse_(file, librariesDir, cacheDir);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    protected List getAvailableLibraries(File libDir) {
        List availableLibraries = new ArrayList();

        for (int i = 0; i < libDir.listFiles().length; i++) {
            File file = libDir.listFiles()[i];

            if (file.getName().toLowerCase().endsWith(".jar")) {
                DependencyResource dependency = DependencyResource.parse(file.getName());
                availableLibraries.add(dependency);
            }
        }

        Collections.sort(availableLibraries,
            new Comparator() {
                public int compare(Object o1, Object o2) {
                    DependencyResource d1 = (DependencyResource)o1;
                    DependencyResource d2 = (DependencyResource)o2;

                    return d1.getVersion().compareTo(d2.getVersion()) * -1; // Latest first
                }
            });

        return availableLibraries;
    }

    public BootStrapResources parse_(File file, File librariesDir, File cacheDir)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        BootStrapResources resources = new BootStrapResources();

        // Add available libraries
        resources.getAvailableLibraries().addAll(getAvailableLibraries(librariesDir));

        // Add the current jdk
        Jdk currentJdk = new Jdk();
        currentJdk.setLocation(new File(JavaEnvUtils.getJdkExecutable("java"))); // Get the jdk home
        currentJdk.setProperties(System.getProperties());
        resources.getJdks().add(currentJdk);

        Element rootEl;

        try {
            rootEl = parseXml(file);
        } catch (BuildException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                Log.get().warn("This project uses bootstrapping, but no resources (such as available jdks) have been defined in: "
                    + e.getCause().getMessage());

                return resources;
            }

            throw e;
        }

        Element jdksEl = getChild(rootEl, "jdks", false);

        if (jdksEl == null) {
            return resources;
        }

        for (Iterator i = getChildren(jdksEl, "jdk", false).iterator(); i.hasNext();) {
            Element jdkEl = (Element)i.next();
            Jdk jdk = new Jdk();
            resources.getJdks().add(jdk);

            jdk.setLocation(new File(getAttribute(jdkEl, "location")));
            Assert.isTrue(jdk.getLocation().exists(),
                "JDK location '" + jdk.getLocation().getAbsolutePath() + "' specified in '" + file.getAbsolutePath()
                + "' does not exist");

            // Create a hash of the jdk location and use it a key for a cache of jvm system properties
            messageDigest.reset();
            messageDigest.update(jdk.getLocation().getAbsolutePath().getBytes("UTF8"));

            String locationHash = toHex(messageDigest.digest());
            File propertiesFile = new File(cacheDir, locationHash + ".properties");

            if (!propertiesFile.exists()
                    || (propertiesFile.exists() && (propertiesFile.lastModified() < jdk.getLocation().lastModified()))) {
                createProperties(jdk.getLocation(), propertiesFile);
            }

            InputStream in = new BufferedInputStream(new FileInputStream(propertiesFile));

            try {
                jdk.getProperties().load(in);
            } finally {
                in.close();
            }
        }

        return resources;
    }

    private void createProperties(File location, File propertiesFile) {
        Project project = new Project();
        project.init();

        Java java = new Java();
        java.setProject(project);
        java.setClasspath(new Path(project, System.getProperty("java.class.path")));
        java.setJvm(location.getAbsolutePath());
        java.setFork(true);
        java.setFailonerror(true);
        java.setClassname(JvmProperties.class.getName());
        java.createArg().setFile(propertiesFile);
        java.setErrorProperty("error");

        try {
            java.perform();
        } finally {
            String error = project.getProperty("error");

            if (error != null) {
                System.err.println(error);
            }
        }
    }

    private String toHex(byte[] fileDigest) {
        StringBuffer hex = new StringBuffer();

        for (int i = 0; i < fileDigest.length; i++) {
            String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);

            if (hexStr.length() < 2) {
                hex.append("0");
            }

            hex.append(hexStr);
        }

        return hex.toString();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class JvmProperties {
        public static void main(String[] args) {
            try {
                File propertiesFile = new File(args[0]);
                File dir = propertiesFile.getParentFile();

                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("Unable to create directory: " + dir.getAbsolutePath());
                }

                OutputStream out = new BufferedOutputStream(new FileOutputStream(propertiesFile));

                try {
                    System.getProperties().store(out, "Generated at " + new Date());
                } finally {
                    out.close();
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }
    }
}
