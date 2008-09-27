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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.taskdefs.ImportTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import ws.quokka.core.bootstrap.BootStrapper;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.bootstrap_util.PropertiesUtil;
import ws.quokka.core.bootstrap_util.Reflect;
import ws.quokka.core.main.ant.task.ArchetypeTask;
import ws.quokka.core.main.ant.task.BuildPathTask;
import ws.quokka.core.main.ant.task.ConsoleTask;
import ws.quokka.core.main.ant.task.CopyPathTask;
import ws.quokka.core.main.ant.task.DependencyOfTask;
import ws.quokka.core.main.ant.task.ForTask;
import ws.quokka.core.main.ant.task.IfTask;
import ws.quokka.core.main.ant.task.InputUnlessSetTask;
import ws.quokka.core.main.ant.task.ListPluginsTask;
import ws.quokka.core.main.ant.task.PluginTargetTask;
import ws.quokka.core.main.ant.task.RunTargetTask;
import ws.quokka.core.main.ant.task.SwitchTask;
import ws.quokka.core.main.ant.task.VariableTask;
import ws.quokka.core.main.parser.ProjectParser;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryFactory;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.repo_standard.RepositoryFactoryImpl;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 */
public class ProjectHelper extends ProjectHelper2 {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String BUILD_RESOURCES_PREFIX = "q.project.resources[";
    private static final Map SPECIAL_TARGETS = new HashMap();
    private static final String BUILD_RESOURCES_LISTENER = "q.project.buildResourcesListener";
    public static final String REPOSITORY_FACTORY = "q.project.repositoryFactory";
    public static final String REPOSITORY = "q.project.repository";

    static {
        SPECIAL_TARGETS.put("archetype", ArchetypeTask.class);

        // ANT's default behaviour is to import targets multiple times under different names
        // I believe this is supposed to allow overriding. However, if the duplicated target includes
        // a dependency-of declaration, this means it will get executed twice. This horrible hack
        // replaces the TargetHandler so that imported targets are only imported once.
        // It also has the effect of dropping any targets in the import that are already defined
        new Reflect().set(new Reflect().getField(ProjectHelper2.class, "targetHandler"), null,
            new TargetHandler() {
                public void onStartElement(String uri, String tag, String qname, Attributes attrs, AntXMLContext context)
                        throws SAXParseException {
                    boolean ignoring = context.isIgnoringProjectTag();

                    try {
                        context.setIgnoreProjectTag(false);

                        super.onStartElement(uri, tag, qname, attrs, context);
                    } finally {
                        context.setIgnoreProjectTag(ignoring);
                    }
                }
            });
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void parse(final Project antProject, Object source) {
        try {
            _parse(antProject, source);
        } finally {
            // Ant 1.7.1 uses a nasty hack to remove duplicates from -projecthelp using the location toString value
            // This effectively removes all but one quokka target as they all have a toString of "".
            // This hack circumvents that hack by giving each target a unique location
            // TODO: time to ditch Ant's Main in favour of a completely custom Main
            int number = 1;

            for (Iterator i = antProject.getTargets().values().iterator(); i.hasNext();) {
                Target target = (Target)i.next();

                if (target.getLocation().equals(Location.UNKNOWN_LOCATION)) {
                    target.setLocation(new Location("" + number++, 1, 1));
                }
            }
        }
    }

    private void _parse(Project antProject, Object source) {
        File antFile = new File(antProject.getUserProperty("ant.file"));

        try {
            // Handle special cases of "archetype" & "help" targets
            String specialTarget = antProject.getProperty("q.project.specialTarget");

            if (specialTarget != null) {
                handleSpecialTarget(antProject, specialTarget);

                return;
            }

            File quokkaFile = getQuokkaFile(antFile);

            if (quokkaFile != null) {
                boolean topLevel = addParentProjectBuildListener(antProject);
                getImportStack().addElement(quokkaFile);
                initialise(antProject, quokkaFile, topLevel);
            } else {
                super.parse(antProject, source);
            }
        } catch (RuntimeException e) {
            // When using no banner logging it is sometimes unclear which sub project is causing the error
            // as the sub build listener is only called after the project is successfully compiled.
            // Therefore, display the current file and rethrow
            antProject.log("Error parsing " + antFile.getPath(), Project.MSG_ERR);
            throw e;
        }
    }

    private void handleSpecialTarget(Project antProject, String specialTarget) {
        antProject.setBasedir(System.getProperty("user.dir"));

        AnnotatedProperties projectProperties = ProjectParser.getProjectProperties(null,
                PropertiesUtil.getProperties(antProject));
        Repository repository = getRepository(getRepositoryProperties(projectProperties, antProject), antProject);

        Target target = new Target();
        target.setProject(antProject);
        target.setName(specialTarget);
        target.setLocation(Location.UNKNOWN_LOCATION);
        antProject.addTarget(target);

        Task task = (Task)new Reflect().construct((Class)SPECIAL_TARGETS.get(specialTarget));
        task.setProject(antProject);
        task.setLocation(Location.UNKNOWN_LOCATION);
        task.setOwningTarget(target);
        task.setTaskName("archetype");

        if (task instanceof ArchetypeTask) {
            ((ArchetypeTask)task).setRepository(repository);
        }

        task.init();
        target.addTask(task);
    }

    private boolean addParentProjectBuildListener(Project antProject) {
        for (Iterator i = antProject.getBuildListeners().iterator(); i.hasNext();) {
            BuildListener buildListener = (BuildListener)i.next();

            if (buildListener instanceof ParentProjectBuildListener) {
                return false;
            }
        }

        ParentProjectBuildListener parentListener = new ParentProjectBuildListener();
        antProject.addBuildListener(parentListener);

        return true;
    }

    /**
     * Determines if the file given is a quokka file. It may be a quokka file if:
     * 1. The source is an ant build file and a matching quokka file exists (by naming convention)
     * 2. The file content is detected as a quokka file
     */
    private File getQuokkaFile(File buildFile) {
        if (getImportStack().size() != 0) { // Don't detect when importing

            return null;
        }

        // TODO: add content-based detection (check for quokka header)
        String name = buildFile.getName();

        if (name.indexOf("quokka") != -1) {
            return buildFile;
        }

        // Assume this is an ant build file and look for a matching quokka file
        File file = new File(buildFile.getParentFile(), name.substring(0, name.lastIndexOf(".")) + "-quokka.xml");

        if (file.exists()) {
            return file;
        }

        return null;
    }

    private DefaultProjectModel initialise(Project antProject, File quokkaFile, boolean topLevel) {
        try {
            return initialise_(antProject, quokkaFile, topLevel);
        } catch (UnresolvedArtifactException e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    private DefaultProjectModel initialise_(Project antProject, File quokkaFile, boolean topLevel) {
        clearInheritedProperties(antProject);
        antProject.setProperty("q.project.file", quokkaFile.getAbsolutePath());
        antProject.setBaseDir(quokkaFile.getParentFile());

        Profiles profiles = new Profiles(antProject.getProperty("profiles"));
        Map antProperties = PropertiesUtil.getProperties(antProject);
        BootStrapper bootStrapper = bootStrap(antProject, antProperties, quokkaFile, profiles, topLevel);

        registerBuiltIns(antProject);
        antProject.log("Quokka project detected: parsing '" + quokkaFile.getPath() + "'", Project.MSG_VERBOSE);

        AnnotatedProperties projectProperties = ProjectParser.getProjectProperties(quokkaFile, antProperties);
        projectProperties.put("basedir", quokkaFile.getParent());

        Repository repository = getRepository(getRepositoryProperties(projectProperties, antProject), antProject);
        DefaultModelFactory factory = new DefaultModelFactory();
        factory.setRepository(repository);

        DefaultProjectModel projectModel = (DefaultProjectModel)factory.getProjectModel(quokkaFile,
                new ArrayList(profiles.getElements()), topLevel, projectProperties, new ProjectLogger(antProject),
                antProject);
        projectModel.setBootStrapper(bootStrapper);
        antProject.addReference("q.projectModel", projectModel);
        antProject.addReference("q.project.scriptHelper", new ScriptHelper(projectModel));
        antProject.setDefault(projectModel.getProject().getDefaultTarget());

        Map antTargets = addTargets(projectModel, antProject, projectModel.getTargets());

        String tempDir = getTargetDir(antProject);
        tempDir = (tempDir == null) ? (antProject.getBaseDir() + "/target") : tempDir;

        // Initialise build resources, including listener to extract resources if they are cleaned
        File dir = normalise(new File(tempDir + "/temp/buildresources"));
        DefaultBuildResources buildResources = (DefaultBuildResources)projectModel.getBuildResources();
        buildResources.setTempDir(dir);

        BuildResourcesListener buildResourcesListener = new BuildResourcesListener(buildResources, antProject);
        antProject.addReference(BUILD_RESOURCES_LISTENER, buildResourcesListener);
        antProject.addBuildListener(buildResourcesListener);

        loadProperties(projectModel, antProject); // Must load after basedir set

        processImports(antProject, projectModel, antTargets);

        // Add any type def paths
        setTypeDefinitionClassLoaders(projectModel);

        return projectModel;
    }

    private String getTargetDir(Project antProject) {
        return antProject.getProperty("q.project.targetDir");
    }

    /**
     * Converts a URL to a file, copying it to a temporary file if necessary
     */
    private File extractFile(final URL url) {
        return (File)new ExceptionHandler() {
                public Object run() throws IOException {
                    if (url.toString().startsWith("file:/")) {
                        return new File(Locator.fromURI(url.toString()));
                    }

                    File file = File.createTempFile("import", ".xml");
                    file.deleteOnExit();
                    new IOUtils().copyStream(url, file);

                    return file;
                }
            }.soften();
    }

    private void processImports(Project antProject, DefaultProjectModel projectModel, Map antTargets) {
        for (Iterator i = projectModel.getResolvedImports().iterator(); i.hasNext();) {
            URL importURL = (URL)i.next();

            antProject.log("Importing " + importURL.toString(), Project.MSG_VERBOSE);

            ImportTask importTask = new ImportTask();
            importTask.setProject(antProject);

            Target dummy = new Target();
            dummy.setName("");
            dummy.setProject(antProject);
            importTask.setOwningTarget(dummy); // Requires a target with name of ""

            String file = extractFile(importURL).getAbsolutePath();
            importTask.setFile(file);
            importTask.setLocation(new Location(file));
            importTask.execute();
        }

        // Search for any "dependency-of" declarations
        for (Iterator i = antProject.getTargets().entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Target target = (Target)entry.getValue();
            Task[] tasks = target.getTasks();

            if ((tasks != null) && (tasks.length != 0) && !(tasks[0] instanceof PluginTargetTask)) {
                for (int j = 0; j < tasks.length; j++) {
                    Task task = tasks[j];

                    if (task.getTaskType().equals("dependency-of")) {
                        Assert.isTrue(task instanceof UnknownElement,
                            "Expected dependency-of element to be of type UnknownElement");
                        task.maybeConfigure();

                        DependencyOfTask dependencyOf = (DependencyOfTask)((UnknownElement)task).getRealThing();
                        List targets = Strings.commaSepList(dependencyOf.getTargets());

                        for (Iterator k = targets.iterator(); k.hasNext();) {
                            String dependencyOfTarget = (String)k.next();
                            org.apache.tools.ant.Target antTarget = (Target)antProject.getTargets().get(dependencyOfTarget);
                            Assert.isTrue(antTarget != null, task.getLocation(),
                                "Dependency-of task refers to an unknown target: " + dependencyOfTarget);
                            antTarget.addDependency(target.getName());
                        }
                    }
                }
            }
        }

        // Handle any dependency-of's declared by plugin dependency targets.
        // Note: this can only be done here are any imports have been included
        for (Iterator i = projectModel.getTargets().values().iterator(); i.hasNext();) {
            ws.quokka.core.model.Target target = (ws.quokka.core.model.Target)i.next();

            if (target.getPluginDependencyTarget() != null) {
                for (Iterator j = target.getPluginDependencyTarget().getDependencyOf().iterator(); j.hasNext();) {
                    String dependencyOfTarget = (String)j.next();
                    org.apache.tools.ant.Target antTarget = (Target)antProject.getTargets().get(dependencyOfTarget);
                    Assert.isTrue(antTarget != null, target.getPluginDependencyTarget().getLocator(),
                        "Dependency-of task refers to an unknown target: " + dependencyOfTarget);
                    antTarget.addDependency(target.getName());
                }
            }
        }
    }

    /**
     * Reassigns the class loader for ant type definitions to include the ant-type project path
     */
    private void setTypeDefinitionClassLoaders(DefaultProjectModel projectModel) {
        List pathList = projectModel.getProjectPath("ant-types", false, true);

        if (pathList.size() != 0) {
            Path path = projectModel.toAntPath(pathList);
            ComponentHelper componentHelper = ComponentHelper.getComponentHelper(projectModel.getAntProject());
            setTypeClassLoader(projectModel.getAntProject(), componentHelper.getAntTypeTable().values(), path);
        }
    }

    private void setTypeClassLoader(Project antProject, Collection typeDefinitions, Path path) {
        Map classLoaders = new HashMap();
        antProject.log("Adding " + path.toString() + " to type definition class loaders", Project.MSG_DEBUG);

        ClassLoader sharedLoader = antProject.createClassLoader(path);

        for (Iterator i = typeDefinitions.iterator(); i.hasNext();) {
            AntTypeDefinition definition = (AntTypeDefinition)i.next();
            ClassLoader currentLoader = definition.getClassLoader();
            antProject.log("Setting " + definition.getName() + ", className=" + definition.getClassName()
                + ", classLoader=" + currentLoader, Project.MSG_DEBUG);

            if (currentLoader == null) {
                definition.setClassLoader(sharedLoader);

                continue;
            }

            ClassLoader newLoader = (ClassLoader)classLoaders.get(currentLoader);

            if (newLoader == null) {
                newLoader = antProject.createClassLoader(currentLoader, path);
                classLoaders.put(currentLoader, newLoader);
            }

            definition.setClassLoader(newLoader);
        }
    }

    private BootStrapper bootStrap(Project antProject, Map properties, File quokkaFile, Profiles profiles,
        boolean topLevel) {
        // Disable bootstrapping:
        // 1. If q.bootstrap.enabled=false
        // 2. Otherwise, it defaults to enabled if launched from a script, or disabled otherwise (e.g. from an IDE)
        // This gets around having to continually specify q.bootstrap.enabled=false in IDEs
        boolean script = "true".equals(properties.get("q.bootstrap.script"));
        String enabled = (String)properties.get("q.bootstrap.enabled");

        if ("false".equals(enabled) || (!script && (enabled == null))) {
            return null; // Bootstrapping disabled
        }

        // Get the arguments that
        List arguments = new ArrayList();

        for (int i = 0; true; i++) {
            String key = "q.bootstrap.args[" + i + "]";
            String argument = (String)properties.get(key);

            if (argument == null) {
                break;
            }

            arguments.add(argument);
        }

        try {
            Log.set(new ProjectLogger(antProject));

            BootStrapper bootStrapper = new BootStrapper();
            bootStrapper.setArguments(arguments);
            bootStrapper.setProfiles(new HashSet(profiles.getElements()));
            bootStrapper.setQuokkaFile(quokkaFile);
            bootStrapper.setProject(antProject);

            bootStrapper.initialise();

            if (bootStrapper.isBootStrapRequired()) {
                Assert.isTrue(topLevel,
                    "Cannot bootstrap '" + quokkaFile.getAbsolutePath() + "' as this is not a top level project. "
                    + "Either standardise the bootstrap options between parents and children, or launch the "
                    + "descendents using <bootstrap> tasks from within parent projects build.xml file.");

                int code;

                try {
                    code = bootStrapper.bootStrap();
                    System.exit(code);
                } catch (Exception e) {
                    antProject.fireBuildFinished(e);
                    System.exit(1);
                }
            }

            return bootStrapper;
        } finally {
            Log.clear();
        }
    }

    /**
     * Returns properties for initialising the repository.
     */
    private AnnotatedProperties getRepositoryProperties(AnnotatedProperties properties, Project antProject) {
        // TODO: work out how to check if debug is enabled before building this
        AnnotatedProperties repositoryProperties = evaluateProperties(properties, antProject, false, null);
        StringBuffer sb = new StringBuffer("\nRepository properties:\n");

        for (Iterator i = new TreeMap(repositoryProperties).entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            if (System.getProperty(key) == null) {
                sb.append("    ").append(key).append(" -> ").append(entry.getValue()).append("\n");
            }
        }

        antProject.log(sb.toString(), Project.MSG_DEBUG);

        return repositoryProperties;
    }

    private Object getField(Class clazz, Object object, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);

            return field.get(object);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private Object getField(Object object, String name) {
        return getField(object.getClass(), object, name);
    }

    private void loadProperties(DefaultProjectModel projectModel, Project antProject) {
        AnnotatedProperties properties = projectModel.getProperties();

        // User properties specified on the CLI (or via subant property elements) over-ride all others
        // Other properties defined in the parent will be inherited
        properties.putAll(antProject.getUserProperties());
        properties.put("basedir", antProject.getBaseDir().getAbsolutePath());
        properties = evaluateProperties(properties, antProject, true, projectModel);

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            setProperty(antProject, (String)entry.getKey(), (String)entry.getValue(), false);
        }
    }

    private void clearInheritedProperties(Project antProject) {
        // Clear all inherited properties
        PropertyHelper ph = PropertyHelper.getPropertyHelper(antProject);
        Hashtable antProperties = (Hashtable)getField(ph, "properties");
        Hashtable user = (Hashtable)getField(ph, "userProperties");
        Hashtable inheritied = (Hashtable)getField(ph, "inheritedProperties");

        for (Iterator i = inheritied.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();

            //            System.out.println("Clearing inherited: " + key);
            if (!key.equals("ant.java.version") && (System.getProperty(key) == null)) {
                antProperties.remove(key);
                user.remove(key);
                i.remove();
            }
        }
    }

    private AnnotatedProperties evaluateProperties(AnnotatedProperties properties, final Project antProject,
        boolean failIfUnreferenced, final DefaultProjectModel projectModel) {
        return properties.evaluateReferences(new AnnotatedProperties.PropertyEvaluator() {
                public boolean canEvaluate(String key) {
                    return key.startsWith(BUILD_RESOURCES_PREFIX);
                }

                public String evaluate(String key) {
                    if (projectModel == null) {
                        return null; // Can only evaluate build resources when the project is available
                    }

                    String resourceKey = key.substring(BUILD_RESOURCES_PREFIX.length(), key.length() - 1).trim();
                    File resource = normalise(projectModel.getBuildResources().getFileOrDir(resourceKey));

                    // If the resource required to be extracted, add it to the listener to restore on demand
                    BuildResourcesListener listener = (BuildResourcesListener)antProject.getReference(BUILD_RESOURCES_LISTENER);

                    if (resource.getPath().startsWith(listener.getBuildResources().getTempDir().getPath())) {
                        antProject.log("Adding resource to BuildResourcesListener: " + resourceKey, Project.MSG_DEBUG);
                        listener.addResource(resourceKey);
                    }

                    return resource.getAbsolutePath();
                }
            }, failIfUnreferenced);
    }

    private File normalise(File file) {
        return FileUtils.getFileUtils().normalize(file.getAbsolutePath());
    }

    private void setProperty(Project antProject, String key, String value, boolean debug) {
        PropertyHelper helper = PropertyHelper.getPropertyHelper(antProject);
        value = helper.replaceProperties(null, value, null);

        if (antProject.getUserProperty(key) != null) {
            if (debug) {
                System.out.println("user property: " + key + "=" + value);
            }

            helper.setUserProperty(null, key, value);
        } else {
            if (debug) {
                System.out.println("inherited property: " + key + "=" + value);
            }

            helper.setInheritedProperty(null, key, value);
        }
    }

    private Map addTargets(DefaultProjectModel project, Project antProject, Map targets) {
        Map antTargets = new HashMap(); // All ant targets keyed by both name and alias
        Map aliases = new HashMap();

        for (Iterator i = targets.values().iterator(); i.hasNext();) {
            ws.quokka.core.model.Target target = (ws.quokka.core.model.Target)i.next();

            if (target.getAlias() != null) {
                aliases.put(target.getName(), target.getAlias());
            }

            org.apache.tools.ant.Target antTarget = new org.apache.tools.ant.Target();
            antTarget.setProject(antProject);
            antTarget.setName(target.getName());

            if (target.getDependencies().size() != 0) {
                antTarget.setDepends(Strings.commaSepList(target.getDependencies()));
            }

            antTarget.setLocation(Location.UNKNOWN_LOCATION);

            // Set the description only if there is an actual implementation. As such only targets that
            // do something turn up in project help
            if (target.isMain() && (!target.isAbstract() || (target.isAbstract() && target.isImplemented()))) {
                antTarget.setDescription(target.getDescription());
            }

            antProject.addTarget(antTarget);
            antProject.log("\t  depends: " + target.getDependencies() + "\n", Project.MSG_DEBUG);

            PluginTargetTask task = new PluginTargetTask();
            task.setProject(antProject);
            task.setOwningTarget(antTarget);
            task.setLocation(Location.UNKNOWN_LOCATION);
            task.setDescription("plugin target task for " + target.getName());

//            String pluginClass = target.getPlugin().getClassName();
//            pluginClass = pluginClass == null ? "plugin" : pluginClass;
//            task.setTaskName(pluginClass.substring(pluginClass.lastIndexOf(".") + 1).toLowerCase());
            task.setTaskName("plugin");
            task.setPluginTarget(target);
            task.setProjectModel(project);
            antTarget.addTask(task);

            antTargets.put(target.getName(), antTarget);

            if (target.getAlias() != null) {
                antTargets.put(target.getAlias(), antTarget);
            }
        }

        // Apply aliases
        for (Iterator i = aliases.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String from = (String)entry.getKey();
            String to = (String)entry.getValue();
            antProject.log("Adding alias from '" + from + "' to '" + to + "'", Project.MSG_DEBUG);

            org.apache.tools.ant.Target antTarget = (Target)antProject.getTargets().get(from);
            Assert.isTrue(antTarget != null, "From target '" + from + "' is not defined in the project.");

            org.apache.tools.ant.Target aliasTarget = new org.apache.tools.ant.Target();
            aliasTarget.setProject(antProject);
            aliasTarget.setName(to);
            aliasTarget.setDepends(antTarget.getName());
            aliasTarget.setLocation(Location.UNKNOWN_LOCATION);
            aliasTarget.setDescription(antTarget.getDescription());
            antTarget.setDescription(null); // Prevent it from showing up as a "main" ant target

            // Rewrite any target dependencies that did refer to the target being aliased, to the aliased target itself
            List dependencies = new ArrayList();

            for (Iterator j = antProject.getTargets().values().iterator(); j.hasNext();) {
                antTarget = (Target)j.next();
                dependencies.clear();

                boolean rewrite = false;

                for (Enumeration e = antTarget.getDependencies(); e.hasMoreElements();) {
                    String dependency = (String)e.nextElement();

                    if (dependency.equals(from)) {
                        dependencies.add(to);
                        rewrite = true;
                    } else {
                        dependencies.add(dependency);
                    }
                }

                if (rewrite) {
                    antProject.log("   Rewrote " + antTarget.getName() + " with " + dependencies, Project.MSG_DEBUG);
                    antTarget.setDepends(Strings.commaSepList(dependencies));
                }
            }

            antProject.addTarget(aliasTarget);
        }

        addBuiltinTargets(antProject);

        return antTargets;
    }

    private void addBuiltinTargets(Project antProject) {
        addBuiltinTarget(antProject, "console", new ConsoleTask(),
            "Allows targets to be entered interactively, eliminating startup overheads");
        addBuiltinTarget(antProject, "list-plugins", new ListPluginsTask(),
            "Lists available plugins including analysis of compatibility with the core and other plugins");
    }

    private void addBuiltinTarget(Project antProject, String name, Task task, String description) {
        Target target = new Target();
        target.setProject(antProject);
        target.setName(name);
        target.setDepends("");
        target.setLocation(Location.UNKNOWN_LOCATION);
        target.setDescription(description);
        antProject.addTarget(target);

        task.setProject(antProject);
        task.setOwningTarget(target);
        task.setLocation(Location.UNKNOWN_LOCATION);
        task.setTaskName(name);
        task.setTaskType(name);
        target.addTask(task);
    }

    private Repository getRepository(AnnotatedProperties properties, Project antProject) {
        RepositoryFactory factory = createFactory(properties, antProject);
        Repository repository;

        // Check for an override (usually for integration testing of quokka itself)
        String override = properties.getProperty("q.repositoryOverride");

        if (override != null) {
            repository = factory.getOrCreate(override, true);
        } else {
            // Try 'project' repository, then 'shared'
            repository = factory.getOrCreate("project", false);

            if (repository == null) {
                repository = factory.getOrCreate("shared", false);
            }

            Assert.isTrue(repository != null, "Either a 'project' or 'shared' repository must be defined");
        }

        repository = new CachingRepository(antProject, repository);
        antProject.addReference(REPOSITORY, repository);

        return repository;
    }

    private RepositoryFactory createFactory(AnnotatedProperties properties, Project antProject) {
        // Create the factory
        RepositoryFactoryImpl factory = new RepositoryFactoryImpl();
        factory.setRepositoryVersion("0.2");
        factory.setProject(antProject);
        factory.setProperties(properties);
        factory.registerType(new RepoType("jar", "Java Archive (.jar) file", "jar"));
        factory.registerType(new RepoType("license", "License file", "txt"));
        factory.registerType(new RepoType("paths", "Repository file", "xml"));
        factory.registerType(new RepoType("plugin", "Quokka plugin", "jar"));
        factory.registerType(new RepoType("archetype", "Quokka archetype", "jar"));
        factory.registerType(new RepoType("depset", "Quokka dependency set", "jar"));
        antProject.addReference(REPOSITORY_FACTORY, factory);

        return factory;
    }

    private void registerBuiltIns(Project antProject) {
        registerBuiltIn(antProject, "copy-path", CopyPathTask.class);
        registerBuiltIn(antProject, "if", IfTask.class);
        registerBuiltIn(antProject, "switch", SwitchTask.class);
        registerBuiltIn(antProject, "run-target", RunTargetTask.class);
        registerBuiltIn(antProject, "for", ForTask.class);
        registerBuiltIn(antProject, "var", VariableTask.class);
        registerBuiltIn(antProject, "dependency-of", DependencyOfTask.class);
        registerBuiltIn(antProject, "buildpath", BuildPathTask.class);
        registerBuiltIn(antProject, "input-unless-set", InputUnlessSetTask.class);
    }

    private void registerBuiltIn(Project antProject, String name, Class clazz) {
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(name);
        def.setClassName(clazz.getName());
        def.setClass(clazz);
        def.setAdapterClass(TaskAdapter.class);
        def.setAdaptToClass(Task.class);
        def.setClassLoader(getClass().getClassLoader());
        def.checkClass(antProject);
        ComponentHelper.getComponentHelper(antProject).addDataTypeDefinition(def);
    }
}
