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
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.util.JavaEnvUtils;

import ws.quokka.core.bootstrap.constraints.BootStrapConstraints;
import ws.quokka.core.bootstrap.constraints.BootStrapContraintsParser;
import ws.quokka.core.bootstrap.constraints.JdkConstraint;
import ws.quokka.core.bootstrap.resources.*;
import ws.quokka.core.bootstrap_util.ArtifactPropertiesParser;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.version.Version;

import java.io.*;

import java.net.URL;

import java.util.*;


/**
 *
 */
public class BootStrapper {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String[] ESCAPE_CHARS = new String[] { "@", "@at@", "\"", "@quot@", "'", "@apos@" };

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Properties properties = new Properties();
    private Set profiles;
    private BootStrapConstraints constraints;
    private BootStrapResources resources;
    private File quokkaFile;
    private String jvmArgs;
    private File librariesDir;
    private File cacheDir;
    private List arguments;
    private CommandlineJava commandLine;
    private List additionalDependencies;
    private Project project;

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
        Properties properties = new ArtifactPropertiesParser().parse(coreFile, "quokka.bundle", "core", "jar");
        List dependencies = getDependencies(properties, "dist");

        // Add constraint dependencies
        additionalDependencies = constraints.findMatchingDependencies(resources);
        dependencies.addAll(additionalDependencies);

        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            DependencyResource resource = (DependencyResource)i.next();

            if (resource instanceof SuppliedDependencyResource) {
                classPath.add(resolveDependencyResource((SuppliedDependencyResource)resource));
            } else {
                classPath.add(toCanonical(resource));
            }
        }

        return classPath;
    }

    private File resolveDependencyResource(SuppliedDependencyResource resource) {
        try {
            if (resource.getFile() != null) {
                // Use project to resolve to current handle the base dir
                File file = project.resolveFile(project.replaceProperties(resource.getFile()));
                Assert.isTrue(file.exists() && !file.isDirectory() && file.canRead(),
                    "Boot dependency file either doesn't exist, is a directory, or isn't readable: " + file.getPath());

                return file;
            } else {
                String md5 = new IOUtils().md5String(resource.getUrl().getBytes("UTF8"));
                File cachedFile = new File(cacheDir, md5 + ".jar");

                if (!cachedFile.exists()) {
                    Log.get().info("Downloading bootstrap dependency from " + resource.getUrl());
                    Assert.isTrue(cachedFile.getParentFile().exists() || cachedFile.getParentFile().mkdirs(),
                        "Cannot create " + cachedFile.getParent());

                    IOUtils utils = new IOUtils();
                    File temp = utils.createTempFile(md5, ".jar", cacheDir);
                    URL url = utils.createURL(resource.getUrl());
                    utils.copyStream(url, temp);
                    Assert.isTrue(temp.renameTo(cachedFile),
                        "Could not rename " + temp.getPath() + " to " + cachedFile.getPath());
                }

                return cachedFile;
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
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

    protected List getCurrentClassPath() {
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
        Log.get().verbose(commandLine.toString());

        Execute execute = new Execute(new PumpStreamHandler(System.out, System.err, System.in));
        execute.setCommandline(commandLine.getCommandline());
        execute.setVMLauncher(true);

//        execute.setWorkingDirectory(new File(System.getProperty("user.dir")));
        try {
            return execute.execute();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    protected CommandlineJava createCommandLine(Jdk jdk, List bootStrapClassPath) {
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
        command.createVmArgument().setValue("-Dq.bootstrap.jvmArgs=" + jdk.getMatchedConstraint().getJvmArgs());

        if (Log.get().isDebugEnabled()) {
            Log.get().debug(classPath.toString());
        }

        command.createClasspath(project).setPath(classPath.toString());
        command.createVmArgument().setLine(jdk.getMatchedConstraint().getJvmArgs());

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

        return command;
    }

    public boolean isReleasable() {
        return (constraints != null) && (constraints.getCoreConstraints().size() != 0);
    }

    public void initialise() {
        if (constraints == null) {
            constraints = new BootStrapContraintsParser(project, quokkaFile, profiles).parse();
        }

        if ((constraints == null) || constraints.isEmpty()) {
            return; // Bootstrapping not specified
        }

        Log.get().verbose("Bootstrapping is active");

        Assert.isTrue(constraints.getCoreConstraints().size() != 0,
            "Bootstrapping requires at least one core constraint to be defined");

        String antHome = System.getProperty("ant.home");

        if (librariesDir == null) {
            librariesDir = new File(new File(antHome), "lib");
        }

        Log.get().verbose("   librariesDir -> " + librariesDir.getAbsolutePath());

        if (cacheDir == null) {
            String dir = project.getProperty("q.bootstrap.cacheDir");
            cacheDir = (dir != null) ? new File(dir) : new File(project.getProperty("q.cacheDir"), "bootstrap");
        }

        Log.get().verbose("   cacheDir -> " + cacheDir.getAbsolutePath());

        if (jvmArgs == null) {
            jvmArgs = unescape(System.getProperty("q.bootstrap.jvmArgs"));
        }

        Log.get().verbose("   jvmArgs -> " + jvmArgs);

        String dir = project.getProperty("q.bootstrap.xml");
        File bootStrapXml = (dir != null) ? new File(dir) : new File(project.getProperty("q.preferencesDir"), "bootstrap.xml");

        if (resources == null) {
            Log.get().verbose("   resourcesFile -> " + bootStrapXml.getAbsolutePath());
            resources = new BootStrapResourcesParser().parse(bootStrapXml, librariesDir, cacheDir);
        }

        //        Assert.isTrue(resources.getJdks().size() != 0, "No jdks have been defined for bootstrapping in: " + bootStrapProperties.getAbsolutePath());
        // Get the environment needed for bootstrapping
        List currentClassPath = getCurrentClassPath();
        List bootClassPath = createBootStrapClassPath();

        // Bootstrapping is required if the either the class paths, or jdks do not match
        boolean match = true;
        Set bootSet = asSet(bootClassPath);
        Set currentSet = asSet(currentClassPath);

        if (!bootSet.equals(currentSet)) {
            match = false;
            Log.get().info("Bootstrap class path does not match (use -v for details)");
            Log.get().verbose("\t current -> " + currentSet);
            Log.get().verbose("\trequired -> " + bootSet);
        }

        String currentJavaHome = System.getProperty("java.home");

        Jdk jdk;

        if (constraints.getJdkConstraints().size() != 0) {
            jdk = constraints.findMatchingJdk(resources);

            if (jdk == null) {
                throw new BuildException("No jdks have been defined that meet the bootstrap requirements in "
                    + bootStrapXml.getAbsolutePath());
            }

            String bootJavaHome = jdk.getProperties().getProperty("java.home");

            if (!bootJavaHome.equals(currentJavaHome)) {
                match = false;
                Log.get().info("Bootstrap JDK does not match (use -v for details)");
                Log.get().verbose("\t current -> " + currentJavaHome);
                Log.get().verbose("\trequired -> " + bootJavaHome);
            }

            String bootJvmArgs = jdk.getMatchedConstraint().getJvmArgs();

            if (bootJvmArgs == null) {
                jdk.getMatchedConstraint().setJvmArgs(jvmArgs); // Passes on existing args to forked jvm
            }

            if (!argsMatch(jvmArgs, bootJvmArgs)) {
                match = false;
                Log.get().info("Bootstrap JVM args do not match (use -v for details)");
                Log.get().verbose("\t current -> " + jvmArgs);
                Log.get().verbose("\trequired -> " + bootJvmArgs);
            }
        } else {
            // Set up the current jdk as the default if no jdk constraints were specified
            jdk = new Jdk();
            jdk.setLocation(new File(JavaEnvUtils.getJdkExecutable("java"))); // Get the jdk home

            JdkConstraint constraint = new JdkConstraint();
            constraint.setJvmArgs(jvmArgs);
            jdk.setMatchedConstraint(constraint);
        }

        // Compare values and if they don't match create the bootstrap command line
        if (!match) {
            commandLine = createCommandLine(jdk, bootClassPath);
        }
    }

    private String unescape(String string) {
        if (string == null) {
            return null;
        }

        for (int i = 0; i < ESCAPE_CHARS.length; i++) {
            String with = ESCAPE_CHARS[i];
            String escape = ESCAPE_CHARS[++i];
            string = replace(string, escape, with);
        }

        return string;
    }

    private String replace(String text, String repl, String with) {
        StringBuffer sb = new StringBuffer(text.length());
        int start = 0;
        int end;

        while ((end = text.indexOf(repl, start)) != -1) {
            sb.append(text.substring(start, end)).append(with);
            start = end + repl.length();
        }

        sb.append(text.substring(start));

        return sb.toString();
    }

    /**
     * Returns true if the jvm arguments include all the arguments specified in the bootJvmArgs
     */
    private boolean argsMatch(String jvmArgs, String bootJvmArgs) {
        List args = parseArgs(jvmArgs);

        for (Iterator i = parseArgs(bootJvmArgs).iterator(); i.hasNext();) {
            String arg = (String)i.next();

            if (!args.contains(arg)) {
                return false;
            }
        }

        return true;
    }

    private List parseArgs(String args) {
        Commandline command = new Commandline();
        command.createArgument().setLine(args);

        return Arrays.asList(command.getArguments());
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
