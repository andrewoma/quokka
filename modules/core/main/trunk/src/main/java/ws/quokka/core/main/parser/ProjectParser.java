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


package ws.quokka.core.main.parser;

import org.apache.tools.ant.taskdefs.condition.Os;

import org.xml.sax.Locator;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.License;
import ws.quokka.core.model.Override;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathSpec;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.PluginDependencyTarget;
import ws.quokka.core.model.Profile;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.model.Project;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.URLs;
import ws.quokka.core.util.xml.Converter;
import ws.quokka.core.util.xml.Document;
import ws.quokka.core.util.xml.Element;
import ws.quokka.core.util.xml.LocatorDomParser;
import ws.quokka.core.util.xml.ReflectionConverter;
import ws.quokka.core.util.xml.XmlConverter;
import ws.quokka.core.version.Version;

import java.io.File;

import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class ProjectParser {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Map OSes = new HashMap();

    static {
        OSes.put(Os.FAMILY_WINDOWS, "osWindows");
        OSes.put(Os.FAMILY_9X, "osWin9x");
        OSes.put(Os.FAMILY_NT, "osWinnt");
        OSes.put(Os.FAMILY_OS2, "osOs2");
        OSes.put(Os.FAMILY_NETWARE, "osNetware");
        OSes.put(Os.FAMILY_DOS, "osDos");
        OSes.put(Os.FAMILY_MAC, "osMac");
        OSes.put(Os.FAMILY_TANDEM, "osTandem");
        OSes.put(Os.FAMILY_UNIX, "osUnix");
        OSes.put(Os.FAMILY_VMS, "osOpenvms");
        OSes.put(Os.FAMILY_ZOS, "osZos");
        OSes.put(Os.FAMILY_OS400, "osOs400");
    }

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File projectFile;
    private Profiles activeProfiles;
    private Repository repository;
    private XmlConverter converter = new XmlConverter();
    private Project project;
    private boolean topLevel;
    private AnnotatedProperties projectProperties;
    private AnnotatedProperties eagerProperties;
    private Logger log;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ProjectParser(File projectFile, Profiles activeProfiles, Repository repository, boolean topLevel,
        AnnotatedProperties projectProperties, Logger log) {
        this.projectFile = projectFile;
        this.activeProfiles = activeProfiles;
        this.repository = repository;
        this.topLevel = topLevel;
        this.projectProperties = projectProperties;
        eagerProperties = new AnnotatedProperties();
        eagerProperties.putAll(projectProperties);
        this.log = log;

        // Set up converter for parsing
        converter.add(new RepoXmlConverter.RepoArtifactIdConverter(RepoArtifactId.class));
        converter.add(new ReflectionConverter(Profile.class));
        converter.add(new ProjectConverter(Project.class));
        converter.add(new ArtifactConverter(Artifact.class));
        converter.add(new DependencySetConverter(DependencySet.class));
        converter.add(new DependencyConverter(Dependency.class));
        converter.add(new PluginDependencyConverter(PluginDependency.class));
        converter.add(new PathConverter(Path.class));
        converter.add(new OverrideConverter(Override.class));
        converter.add(new PluginDependendencyTargetConverter(PluginDependencyTarget.class));
        converter.add(new ReflectionConverter(PathSpec.class));
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    private void activateAutomaticProfiles() {
        for (int i = 1; i <= 6; i++) {
            String version = "1." + i;
            activatePropertyProfile("source" + version, "q.project.java.source", version);
            activatePropertyProfile("target" + version, "q.project.java.target", version);
        }
    }

    private void activatePropertyProfile(String id, String key, String value) {
        String actual = eagerProperties.getProperty(key);

        if (value.equals(actual)) {
            activateProfile(id);
        }
    }

    private void activateProfile(String id) {
        project.getActiveProfiles().add(id);
        log.verbose("Automatically activating profile '" + id + "'");
    }

    private Element applyProfilesChild(Element element, String child) {
        List elements = applyProfiles(element.getChildren(child));
        Assert.isTrue(elements.size() <= 1,
            "There can be only 1 active '" + child + "' element at a time for a given '" + element.getName() + "'");

        return (Element)((elements.size() == 0) ? null : elements.get(0));
    }

    private List applyProfiles(List elements) {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element element = (Element)i.next();

            if (applyProfiles(element) == null) {
                i.remove();
            }
        }

        return elements;
    }

    private Element applyProfiles(Element element) {
        if (element == null) {
            return null;
        }

        return activeProfiles.matches(element.getAttribute("profiles")) ? element : null;
    }

    private void addProperties(AnnotatedProperties properties, String prefix, Element el) {
        for (Iterator i = applyProfiles(el.getChildren("property")).iterator(); i.hasNext();) {
            Element propertyEl = (Element)i.next();
            addProperty(propertyEl, prefix, Collections.singleton(properties));
        }
    }

    public static void addProperty(Element propertyEl, String prefix, Collection properties) {
        String name = propertyEl.getAttribute("name");
        String value = propertyEl.getAttribute("value");
        String text = propertyEl.getText();
        Locator locator = getLocator(propertyEl);
        Assert.isTrue(((value == null) && (text.length() != 0)) || ((value != null) && (text.length() == 0)), locator,
            "Property must either have text content or a value attribute, but not both");
        value = (value == null) ? text : value;

        Annotations annotations = new Annotations();
        annotations.put(AnnotatedObject.LOCATOR, locator);
        annotations.put("initial", value);

        for (Iterator i = properties.iterator(); i.hasNext();) {
            AnnotatedProperties annotatedProperties = (AnnotatedProperties)i.next();
            annotatedProperties.setProperty(prefix + name, value, annotations);
        }
    }

    /*
     * TODO: Define DTDs and add validation
     */
    public Project parse() {
        log.verbose("Parsing " + projectFile.getAbsolutePath());

        QuokkaEntityResolver resolver = new QuokkaEntityResolver();

//        resolver.addVersion("project", "0.1");
        resolver.addVersion("project", new String[] { "0.1", "0.2" });

        Document document = Document.parse(projectFile, resolver);
        Project project = (Project)converter.fromXml(Project.class, document.getRoot());
        project.setProjectFile(projectFile);

        return project;
    }

    public static AnnotatedProperties getProjectProperties(File projectFile, Map antProperties) {
        AnnotatedProperties properties = new AnnotatedProperties();

        // Project properties
        if (projectFile != null) {
            String name = projectFile.getAbsolutePath();
            File file = new File(name.substring(0, name.lastIndexOf(".")) + ".properties");

            if (file.exists()) {
                properties.putAll(getProperties(URLs.toURL(file)));
            }
        }

        // Project properies from project file itself
        addTopLevelProjectProperties(projectFile, properties);

        // User properties
        final File file = new File(new File(new File(System.getProperty("user.home")), ".quokka"), "quokka.properties");

        if (!file.exists()) {
            File parentFile = file.getParentFile();
            System.out.println("Creating default properties: " + file.getAbsolutePath());
            Assert.isTrue(parentFile.exists() || parentFile.mkdirs(),
                "Cannot getOrCreate quokka defaults dir: " + parentFile.getAbsolutePath());

            new IOUtils().copyStream(ProjectParser.class.getClassLoader().getResourceAsStream("quokkadefaults.properties"),
                file);
        }

        properties.putAll(getProperties(URLs.toURL(file)));

        // Ant properties ... those set by command line
        properties.putAll(antProperties);

        return properties;
    }

    private static void addTopLevelProjectProperties(File projectFile, AnnotatedProperties properties) {
        try {
            Document document = Document.parse(projectFile, new Document.NullEntityResolver());
            Element projectEl = document.getRoot();

            if (projectEl != null) {
                Element dependencySetEl = projectEl.getChild("dependency-set");
                List propertyEls = dependencySetEl.getChildren("property");

                for (Iterator i = propertyEls.iterator(); i.hasNext();) {
                    Element propertyEl = (Element)i.next();
                    addProperty(propertyEl, "", Collections.singleton(properties));
                }
            }
        } catch (Exception e) {
            // Ignore as the document will be parsed properly and validated later
        }
    }

    private static AnnotatedProperties getProperties(URL url) {
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.load(url);

        return properties;
    }

    private static Locator getLocator(Element el) {
        return LocatorDomParser.getLocator(el.getElement());
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public class PluginDependendencyTargetConverter extends AbstractProjectConverter {
        public PluginDependendencyTargetConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element element) {
            PluginDependencyTarget target = (PluginDependencyTarget)super.fromXml(element);

            if ((target.getTemplate() != null) && (target.getPrefix() == null)) {
                target.setPrefix(target.getName()); // Default the prefix to the target name if not supplied
            }

            List dependencies = Strings.commaSepList(element.getAttribute("depends"));

            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                String dependency = (String)i.next();
                target.addDependency(dependency);
            }

            List dependencyOf = Strings.commaSepList(element.getAttribute("dependency-of"));

            for (Iterator i = dependencyOf.iterator(); i.hasNext();) {
                String dependency = (String)i.next();
                target.addDependencyOf(dependency);
            }

            DependencySet dependencySet = (DependencySet)getContext("dependencySet");
            addProperties(dependencySet.getProperties(),
                (target.getPrefix() == null) ? "" : (target.getPrefix() + "."), element);

            return target;
        }
    }

    public static class AbstractProjectConverter extends ReflectionConverter {
        public AbstractProjectConverter(Class clazz) {
            super(clazz);
        }
    }

    public class OverrideConverter extends RepoXmlConverter.RepoOverrideConverter {
        public OverrideConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element overrideEl) {
            Override override = (Override)super.fromXml(overrideEl);
            String paths = overrideEl.getAttribute("plugin-paths");

            if (paths != null) {
                for (Iterator i = Strings.commaSepList(paths).iterator(); i.hasNext();) {
                    String path = (String)i.next();
                    override.addPluginPath(path);
                }
            }

            return override;
        }
    }

    public class ProjectConverter extends AbstractProjectConverter {
        public ProjectConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element projectEl) {
            project = (Project)super.fromXml(projectEl);
            project.setActiveProfiles(activeProfiles);
            project.setProperties(projectProperties);

            addAutomaticProfiles();
            activateAutomaticProfiles();

            Converter converter;

            // Description
            Element descriptionEl = projectEl.getChild("description");

            if (descriptionEl != null) {
                project.setDescription(descriptionEl.getText());
            }

            // Dependency set
            // Important: parse first as dependency sets must be recursed first to discover profiles, overrides and paths
            Element dependencySetEl = applyProfilesChild(projectEl, "dependency-set");

            if (dependencySetEl != null) {
                Assert.isTrue(dependencySetEl.getElement().getAttributes().getLength() == 0,
                    getLocator(dependencySetEl), "The root dependency set should not have any attributes");

                converter = getConverter(DependencySet.class);
                project.setDependencySet((DependencySet)converter.fromXml(dependencySetEl));
            }

            // Artifacts
            Element artifactsEl = projectEl.getChild("artifacts");

            if (artifactsEl != null) {
                converter = getConverter(RepoArtifactId.class);

                RepoArtifactId defaultId = (RepoArtifactId)converter.fromXml(artifactsEl);

                List artifactEls = applyProfiles(artifactsEl.getChildren("artifact"));

                for (Iterator i = artifactEls.iterator(); i.hasNext();) {
                    Element artifactEl = (Element)i.next();
                    converter = getConverter(Artifact.class);
                    addContext("defaultId", defaultId);

                    Artifact artifact = (Artifact)converter.fromXml(artifactEl);
                    project.addArtifact(artifact);
                }
            }

            // Default the artifact description to the project description if there is a single artifact
            if (project.getArtifacts().size() == 1) {
                Artifact artifact = (Artifact)project.getArtifacts().iterator().next();

                if (artifact.getDescription() == null) {
                    artifact.setDescription(project.getDescription());
                }
            }

            // Default the project name to the group, if none is supplied
            if ((project.getName() == null) && (project.getArtifacts().size() > 0)) {
                project.setName(((Artifact)project.getArtifacts().iterator().next()).getId().getGroup());
            }

            Assert.isTrue(project.getName() != null, getLocator(projectEl),
                "The name attribute must be supplied if no artifacts are defined");

            return project;
        }

        private void addAutomaticProfiles() {
            addProfile("hasParent", "Automatic profile: activated if the project has been invoked by a parent",
                !topLevel);

            for (int i = 1; i <= 6; i++) {
                String version = "1." + i;
                addPropertyProfile("source" + version, "q.project.java.source", version);
                addPropertyProfile("target" + version, "q.project.java.target", version);
                addProfile("java" + version, "Automatic profile: activated if 'ant.java.version='" + version,
                    version.equals(projectProperties.get("ant.java.version")));
            }

            // Adds profiles for OS families and activates the current one
            for (Iterator i = OSes.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String profileId = (String)entry.getValue();
                String antId = (String)entry.getKey();
                addProfile(profileId, "Automatic profile: activated if the OS is " + antId, Os.isFamily(antId));
            }
        }

        private void addPropertyProfile(String id, String key, String value) {
            String actual = eagerProperties.getProperty(key);
            addProfile(id, "Automatic profile: activated if '" + key + "=" + value + "'", value.equals(actual));
        }

        private void addProfile(String id, String description, boolean active) {
            project.addProfile(new Profile(id, description));

            if (active) {
                activateProfile(id);
            }
        }
    }

    public class DependencySetConverter extends AbstractProjectConverter {
        public DependencySetConverter(Class clazz) {
            super(clazz);
        }

        /**
         * Important: Process the overrides, profiles and paths first then recurse. The accumulated lists
         * are required for proper processing of subsequent elements.
         */
        public Object fromXml(Element dependencySetEl) {
            DependencySet dependencySet = (DependencySet)super.fromXml(dependencySetEl);
            dependencySet.setParent((DependencySet)getXmlConverter().getContext("dependencySetParent"));

            Converter converter;

            // Profiles ... must come first
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("profile")).iterator(); i.hasNext();) {
                Element profileEl = (Element)i.next();
                converter = getConverter(Profile.class);

                Profile profile = (Profile)converter.fromXml(profileEl);
                dependencySet.addProfile(profile);
                project.addProfile(profile);
            }

            // Properties
            addProperties(dependencySet.getProperties(), "", dependencySetEl);

            AnnotatedProperties temp = eagerProperties;
            eagerProperties = new AnnotatedProperties();
            eagerProperties.putAll(dependencySet.getProperties());
            eagerProperties.putAll(temp);
            activateAutomaticProfiles();

            // Overrides
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("override")).iterator(); i.hasNext();) {
                Element overrideEl = (Element)i.next();
                converter = getConverter(Override.class);

                Override override = (Override)converter.fromXml(overrideEl);
                dependencySet.addOverride(override);

//                project.addOverride(override);
            }

            // Paths
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("path")).iterator(); i.hasNext();) {
                Element pathEl = (Element)i.next();
                converter = getConverter(Path.class);

                Path path = (Path)converter.fromXml(pathEl);
                dependencySet.addPath(path);
            }

            // Nested dependency sets ... must do before adding dependencies
            List dependencySetEls = applyProfiles(dependencySetEl.getChildren("dependency-set"));

            for (Iterator i = dependencySetEls.iterator(); i.hasNext();) {
                Element nestedSetEl = (Element)i.next();
                Assert.isTrue(nestedSetEl.getElement().getChildNodes().getLength() == 0, getLocator(nestedSetEl),
                    "Nested dependency sets do not currently support child nodes");

                // Get the dependency set from the repository
                converter = getConverter(RepoArtifactId.class);

                RepoArtifactId id = ((RepoArtifactId)converter.fromXml(nestedSetEl)).mergeDefaults();
                id = new RepoArtifactId(id.getGroup(), id.getName(), "depset", id.getVersion());

                RepoArtifact artifact;

                try {
                    artifact = ProjectParser.this.repository.resolve(id);
                } catch (UnresolvedArtifactException e) {
                    // Support types of jar during the migration window
                    id = new RepoArtifactId(id.getGroup(), id.getName(), "jar", id.getVersion());
                    artifact = ProjectParser.this.repository.resolve(id);
                }

                // Create a new dependency set
                getXmlConverter().addContext("dependencySetParent", dependencySet);

                DependencySet nestedSet = (DependencySet)fromXml(parseNestedDependencySet(artifact));
                nestedSet.setParent(dependencySet);
                dependencySet.addSubset(nestedSet);
                nestedSet.setArtifact(artifact);

                // Set the properties
                URL url = URLs.toURL(artifact.getLocalCopy(),
                        "META-INF/quokka/" + nestedSet.getArtifact().getId().toPathString() + "/quokka.properties");

                if (url != null) {
                    nestedSet.getProperties().putAll(getProperties(url));

                    temp = eagerProperties;
                    eagerProperties = new AnnotatedProperties();
                    eagerProperties.putAll(nestedSet.getProperties());
                    eagerProperties.putAll(temp);
                    activateAutomaticProfiles();
                }

                // Set the build resources
                nestedSet.addBuildResources(URLs.toURLEntries(artifact.getLocalCopy(),
                        "META-INF/quokka/" + nestedSet.getArtifact().getId().toPathString() + "/resources/"));

                // Add imported ANT project
                nestedSet.setImportURL(URLs.toURL(artifact.getLocalCopy(),
                        "META-INF/quokka/" + nestedSet.getArtifact().getId().toPathString() + "/import.xml"));
            }

            // Dependencies
            List dependencyEls = applyProfiles(dependencySetEl.getChildren("dependency"));

            for (Iterator i = dependencyEls.iterator(); i.hasNext();) {
                Element dependencyEl = (Element)i.next();
                converter = getConverter(Dependency.class);

                Dependency dependency = (Dependency)converter.fromXml(dependencyEl);
                dependencySet.addDependency(dependency);
            }

            // Plugin dependencies
            dependencyEls = applyProfiles(dependencySetEl.getChildren("plugin"));
            addContext("dependencySet", dependencySet);

            for (Iterator i = dependencyEls.iterator(); i.hasNext();) {
                Element dependencyEl = (Element)i.next();
                converter = getConverter(PluginDependency.class);
                dependencySet.addDependency((PluginDependency)converter.fromXml(dependencyEl));
            }

            // Location of top level project properties, imports and resources are different to nested depsets
            if (dependencySet.getParent() == null) {
                // Set the import file
                String name = projectFile.getAbsolutePath();
                File importFile = new File(name.substring(0, name.lastIndexOf("-quokka")) + ".xml");

                if (importFile.exists()) {
                    dependencySet.setImportURL(URLs.toURL(importFile));
                }

                // Build resources
                dependencySet.addBuildResources(URLs.toURLEntries(projectFile.getParentFile(), "build/resources/"));
            }

            // Licenses
            List licenseEls = dependencySetEl.getChildren("license");

            for (Iterator i = licenseEls.iterator(); i.hasNext();) {
                Element licenseEl = (Element)i.next();
                Locator locator = LocatorDomParser.getLocator(licenseEl.getElement());

                String file = licenseEl.getAttribute("file");
                RepoArtifactId id = (RepoArtifactId)getConverter(RepoArtifactId.class).fromXml(licenseEl);
                Assert.isTrue(((file == null) && (id.getGroup() != null) && (id.getVersion() != null))
                    || ((file != null) && (id.getGroup() == null) && (id.getVersion() == null)), locator,
                    "license element must have either single a 'file' attribute, or have 'group' and 'version' attributes set");
                id = id.merge(new RepoArtifactId(null, null, "license", (Version)null)).mergeDefaults();
                dependencySet.addLicense(new License((file == null) ? null : new File(file), id));
            }

            return dependencySet;
        }

        private Element parseNestedDependencySet(RepoArtifact artifact) {
            //            URL url = URLs.toURL(artifact.getLocalCopy(), "depset.xml");
            String path = "META-INF/quokka/" + artifact.getId().toPathString() + "/depset.xml";
            URL url = URLs.toURL(artifact.getLocalCopy(), path);
            Assert.isTrue(url != null,
                "Nested dependency set cannot be found: " + artifact.getLocalCopy().getPath() + ", entry=" + path);

            return Document.parse(url).getRoot();
        }
    }

    public class DependencyConverter extends RepoXmlConverter.RepoDependencyConverter {
        public DependencyConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element dependencyEl) {
            Dependency dependency = (Dependency)super.fromXml(dependencyEl);

            return dependency;
        }

        public List filter(List pathSpecEls) {
            return applyProfiles(pathSpecEls);
        }
    }

    public class PathConverter extends RepoXmlConverter.RepoPathConverter {
        public PathConverter(Class clazz) {
            super(clazz);
        }
    }

    public class PluginDependencyConverter extends DependencyConverter {
        public PluginDependencyConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element dependencyEl) {
            PluginDependency dependency = (PluginDependency)super.fromXml(dependencyEl);

            // Force id to be of type 'plugin'
            RepoArtifactId id = dependency.getId();
            dependency.setId(new RepoArtifactId(id.getGroup(), id.getName(), "plugin", id.getVersion()));

            // Add targets in short-hand form
            List targets = Strings.commaSepList(dependencyEl.getAttribute("targets"));

            for (Iterator i = targets.iterator(); i.hasNext();) {
                PluginDependencyTarget target = new PluginDependencyTarget();
                target.setName((String)i.next());
                dependency.addTarget(target);
            }

            // Add explicity defined targets
            List targetEls = applyProfiles(dependencyEl.getChildren("target"));

            for (Iterator i = targetEls.iterator(); i.hasNext();) {
                Element targetEl = (Element)i.next();
                Converter converter = getConverter(PluginDependencyTarget.class);
                PluginDependencyTarget target = (PluginDependencyTarget)converter.fromXml(targetEl);
                dependency.addTarget(target);
            }

            // Ensure the to attribute has not been set
            for (Iterator i = dependency.getPathSpecs().iterator(); i.hasNext();) {
                PathSpec pathSpec = (PathSpec)i.next();
                Assert.isTrue(pathSpec.getTo() == null, pathSpec.getLocator(),
                    "The 'to' attribute is not valid for plugin path specifications");
            }

            List templates = Strings.commaSepList(dependencyEl.getAttribute("templates"));

            for (Iterator i = templates.iterator(); i.hasNext();) {
                String template = (String)i.next();
                dependency.addTemplate(template);
            }

            return dependency;
        }

        public boolean toRequired() {
            return false;
        }
    }

    public class ArtifactConverter extends AbstractProjectConverter {
        public ArtifactConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element artifactEl) {
            Artifact artifact = (Artifact)super.fromXml(artifactEl);
            Converter converter = getConverter(RepoArtifactId.class);

            RepoArtifactId id = (RepoArtifactId)converter.fromXml(artifactEl);
            Element nameEl = applyProfilesChild(artifactEl, "name");

            if (nameEl != null) {
                id = new RepoArtifactId(id.getGroup(), nameEl.getAttribute("value"), id.getType(), id.getVersion());
            }

            id = id.merge((RepoArtifactId)getContext("defaultId")).mergeDefaults();
            artifact.setId(id);

            // Exported paths
            List paths = Strings.commaSepList(artifactEl.getAttribute("paths"));

            for (Iterator i = paths.iterator(); i.hasNext();) {
                String path = (String)i.next();
                String[] tokens = Strings.split(path, ":");
                artifact.addExportedPath(tokens[0], (tokens.length > 1) ? tokens[1] : tokens[0]);
            }

            Element descriptionEl = artifactEl.getChild("description");

            if (descriptionEl != null) {
                artifact.setDescription(descriptionEl.getText());
            }

            return artifact;
        }
    }
}
