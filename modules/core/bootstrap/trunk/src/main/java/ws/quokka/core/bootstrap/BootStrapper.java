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


package ws.quokka.core.bootstrap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.launch.Launcher;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.util.JavaEnvUtils;

import ws.quokka.core.bootstrap.constraints.BootStrapConstraints;
import ws.quokka.core.bootstrap.constraints.BootStrapContraintsParser;
import ws.quokka.core.bootstrap.constraints.JdkConstraint;
import ws.quokka.core.bootstrap.resources.BootStrapResources;
import ws.quokka.core.bootstrap.resources.BootStrapResourcesParser;
import ws.quokka.core.bootstrap.resources.DependencyResource;
import ws.quokka.core.bootstrap.resources.Jdk;
import ws.quokka.core.bootstrap_util.ArtifactPropertiesParser;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Exec;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.version.Version;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;


/**
 *
 */
public class BootStrapper {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Properties properties = new Properties();
    private Set profiles;
    private BootStrapConstraints constraints;
    private BootStrapResources resources;
    private File quokkaFile;
    private String maxMemory;
    private File librariesDir;
    private File cacheDir;
    private List arguments;
    private String commandLine;
    private List additionalDependencies;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setLibrariesDir(File librariesDir) {
        this.librariesDir = librariesDir;
    }

    public void setQuokkaFile(File quokkaFile) {
        this.quokkaFile = quokkaFile;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setProfiles(Set profiles) {
        this.profiles = profiles;
    }

    public boolean isBootStrapRequired() {
        return commandLine != null;
    }

    public void setArguments(List arguments) {
        this.arguments = arguments;
    }

    protected void setConstraints(BootStrapConstraints constraints) {
        this.constraints = constraints;
    }

    protected void setBootStrapProperties(BootStrapResources resources) {
        this.resources = resources;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    protected List createBootStrapClassPath() {
        List classPath = new ArrayList();

        DependencyResource core = constraints.findMatchingCore(resources);
        File coreFile = toCanonical(core);
        classPath.add(coreFile);

        // Add core dependencies
        Properties properties = new ArtifactPropertiesParser().parse(coreFile, "quokka.core.main", "main", "jar");
        List dependencies = getDependencies(properties, "dist");

        // Add constraint dependencies
        additionalDependencies = constraints.findMatchingDependencies(resources);
        dependencies.addAll(additionalDependencies);

        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            DependencyResource resource = (DependencyResource)i.next();
            classPath.add(toCanonical(resource));
        }

        return classPath;
    }

    public List getAdditionalDependencies() {
        return additionalDependencies;
    }

    public static List getDependencies(Properties properties, String path) {
        List dependencies = new ArrayList();

        for (int i = 0; true; i++) {
            String prefix = "path." + path + "." + i + ".id.";
            String group = properties.getProperty(prefix + "group");

            if (group == null) {
                break;
            }

            String name = properties.getProperty(prefix + "name");
            String version = properties.getProperty(prefix + "version");
            dependencies.add(new DependencyResource(group, name, new Version(version)));
        }

        return dependencies;
    }

    private File toCanonical(DependencyResource dependency) {
        try {
            return new File(librariesDir, dependency.toFileName()).getCanonicalFile();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    protected List getJavaClassPath() {
        List javaClassPath = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);

        while (tokenizer.hasMoreTokens()) {
            try {
                File file = new File(tokenizer.nextToken()).getCanonicalFile();

                if (!file.getAbsolutePath().endsWith(File.separator + "tools.jar")) { // Strip tools.jar
                    javaClassPath.add(file);
                }
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }

        return javaClassPath;
    }

    private Set asSet(Collection collection) {
        return new TreeSet(collection);
    }

    public int bootStrap() {
        Log.get().info("Bootstrapping build ...");
        Log.get().verbose(commandLine);

        return new Exec().exec(commandLine);
    }

    protected String createCommandLine(Jdk jdk, List bootStrapClassPath) {
        if (Log.get().isDebugEnabled()) {
            Log.get().debug(bootStrapClassPath.toString());
        }

        // Convert the classpath to a string
        StringBuffer classPath = new StringBuffer();

        for (Iterator i = bootStrapClassPath.iterator(); i.hasNext();) {
            File file = (File)i.next();
            classPath.append(file.getAbsolutePath());

            if (i.hasNext()) {
                classPath.append(File.pathSeparator);
            }
        }

        CommandlineJava command = new CommandlineJava();
        command.setVm(jdk.getLocation().getAbsolutePath());

        Project project = new Project();
        project.init();

        // VM args
        command.createVmArgument().setValue("-Dorg.apache.tools.ant.ProjectHelper=ws.quokka.core.main.ant.ProjectHelper");
        command.createVmArgument().setValue("-Dant.home=" + System.getProperty("ant.home"));
        command.createVmArgument().setValue("-Dant.library.dir=" + new File(System.getProperty("ant.home"), "antlib"));
        command.createVmArgument().setValue("-Dquokka.bootstrap.maxMemory=" + jdk.getMatchedConstraint().getMaxMemory());

        if (Log.get().isDebugEnabled()) {
            Log.get().debug(classPath.toString());
        }

        command.createClasspath(project).setPath(classPath.toString());
        command.setMaxmemory(jdk.getMatchedConstraint().getMaxMemory());

        command.setClassname(Launcher.class.getName());

        // Launcher args
        //        command.createArgument().setLine("-D" + IN_PROGRESS + "=true"); // Indicate bootstrapping has taken place
        command.createArgument().setValue("-main");
        command.createArgument().setValue("ws.quokka.core.main.ant.QuokkaMain");

        // Properties
        // TODO: work out if there is a max command line length ... might be better passing via a file
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            command.createArgument().setValue("-D" + entry.getKey() + "=" + entry.getValue());
        }

        for (Iterator i = arguments.iterator(); i.hasNext();) {
            String target = (String)i.next();
            command.createArgument().setValue(target);
        }

        return command.toString();
    }

    public boolean isReleasable() {
        return (constraints != null) && (constraints.getCoreConstraints().size() != 0);
    }

    public void initialise() {
        if (constraints == null) {
            constraints = new BootStrapContraintsParser(quokkaFile, profiles).parse();
        }

        if ((constraints == null) || constraints.isEmpty()) {
            return; // Bootstrapping not specified
        }

        Log.get().verbose("Bootstrapping is active");

        Assert.isTrue(constraints.getCoreConstraints().size() != 0,
            "Bootstrapping requires at least one core constraint to be defined");

        String antHome = System.getProperty("ant.home");
        File bootStrapDefaultDir = new File(antHome, "bootstrap");

        if (librariesDir == null) {
            librariesDir = new File(new File(antHome), "libs");
        }

        Log.get().verbose("   librariesDir -> " + librariesDir.getAbsolutePath());

        if (cacheDir == null) {
            cacheDir = new File(System.getProperty("quokka.bootstrap.cachedir"),
                    new File(bootStrapDefaultDir, "cache").getAbsolutePath());
        }

        Log.get().verbose("   cacheDir -> " + cacheDir.getAbsolutePath());

        if (maxMemory == null) {
            maxMemory = System.getProperty("quokka.bootstrap.maxMemory");
        }

        Log.get().verbose("   maxMemory -> " + maxMemory);

        File bootStrapPropertes = new File(System.getProperty("quokka.bootstrap.properties"),
                new File(bootStrapDefaultDir, "bootstrap.xml").getAbsolutePath());

        if (resources == null) {
            Log.get().verbose("   resourcesFile -> " + bootStrapPropertes.getAbsolutePath());
            resources = new BootStrapResourcesParser().parse(bootStrapPropertes, librariesDir, cacheDir);
        }

        //        Assert.isTrue(resources.getJdks().size() != 0, "No jdks have been defined for bootstrapping in: " + bootStrapPropertes.getAbsolutePath());
        // Get the environment needed for bootstrapping
        List javaClassPath = getJavaClassPath();
        List bootStrapClassPath = createBootStrapClassPath();

        // Bootstrapping is required if the either the class paths, or jdks do not match
        boolean match = true;
        Set bootSet = asSet(bootStrapClassPath);

        if (Log.get().isDebugEnabled()) {
            Log.get().debug("bootClassPath -> " + bootSet);
        }

        Set javaSet = asSet(javaClassPath);

        if (Log.get().isDebugEnabled()) {
            Log.get().debug("javaClassPath -> " + javaSet);
        }

        if (!bootSet.equals(javaSet)) {
            match = false;
            Log.get().debug("Class paths do not match");
        }

        String javaHome = System.getProperty("java.home");

        if (Log.get().isDebugEnabled()) {
            Log.get().debug("javaHome -> " + javaHome);
        }

        Jdk jdk;

        if (constraints.getJdkConstraints().size() != 0) {
            jdk = constraints.findMatchingJdk(resources);

            String bootHome = jdk.getProperties().getProperty("java.home");
            Log.get().debug("bootHome -> " + bootHome);

            if (!bootHome.equals(javaHome)) {
                match = false;
                Log.get().debug("Jdks do not match");
            }

            String bootMemory = jdk.getMatchedConstraint().getMaxMemory();
            Log.get().debug("bootMemory -> " + bootMemory);
            Log.get().debug("javaMemory -> " + maxMemory);

            if (bootMemory == null) {
                jdk.getMatchedConstraint().setMaxMemory(maxMemory); // Passes on existing memory to forked jvm
            }

            if (maxMemory.equals(bootMemory)) {
                match = false;
                Log.get().debug("Memory does not match");
            }
        } else {
            // Set up the current jdk as the default if no jdk constraints were specified
            jdk = new Jdk();
            jdk.setLocation(new File(JavaEnvUtils.getJdkExecutable("java"))); // Get the jdk home

            JdkConstraint constraint = new JdkConstraint();
            constraint.setMaxMemory(maxMemory);
            jdk.setMatchedConstraint(constraint);
        }

        // Compare values and if they don't match create the bootstrap command line
        if (!match) {
            commandLine = createCommandLine(jdk, bootStrapClassPath);
        }
    }
}
