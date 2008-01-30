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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.Override;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.PluginDependencyTarget;
import ws.quokka.core.model.Profile;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.model.Project;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.repo_spi.Repository;
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
import ws.quokka.core.version.VersionRangeUnion;

import java.io.File;
import java.io.FileOutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class ProjectParser {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File projectFile;
    private Profiles activeProfiles;
    private Repository repository;
    private XmlConverter converter = new XmlConverter();
    private Project project;
    private List traversedDependencySets = new ArrayList();
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
        converter.add(new AbstractProjectConverter(ws.quokka.core.model.Override.class));
        converter.add(new PluginDependendencyTargetConverter(PluginDependencyTarget.class));
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    private void activateAutomaticProfiles() {
        for (int i = 1; i <= 6; i++) {
            String version = "1." + i;
            activatePropertyProfile("source" + version, "quokka.project.java.source", version);
            activatePropertyProfile("target" + version, "quokka.project.java.target", version);
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

    private List applyProfiles(List elements) {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            Profiles elementProfiles = new Profiles(element.getAttribute("profiles"));

            for (Iterator j = elementProfiles.getElements().iterator(); j.hasNext();) {
                String profileId = (String)j.next();
                String absoluteProfileId = profileId;

                if (profileId.startsWith("-")) {
                    absoluteProfileId = profileId.substring(1);
                }

                Assert.isTrue(project.getProfiles().get(absoluteProfileId) != null,
                    LocatorDomParser.getLocator(element.getElement()),
                    "Profile '" + profileId + "' has not been defined in the project");
            }

            if (!activeProfiles.matches(elementProfiles)) {
                i.remove();
            }
        }

        return elements;
    }

    private RepoArtifactId override(RepoArtifactId id, String scope) {
        for (Iterator i = project.getOverrides().iterator(); i.hasNext();) {
            Override override = (Override)i.next();

            if (override.matches(scope, id)) {
                RepoArtifactId overridden = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(),
                        override.getWith());
                overridden.setAnnotations((Annotations)id.getAnnotations().clone());
                overridden.getAnnotations().put("overiddenVersion", id.getVersion());
                overridden.getAnnotations().put("overiddenBy", override);

                return overridden;
            }
        }

        return id;
    }

    /*
     * TODO: Define DTDs and add validation
     */
    public Project parse() {
        if (log.isDebugEnabled()) {
            log.debug("Parsing " + projectFile.getAbsolutePath());
        }

        QuokkaEntityResolver resolver = new QuokkaEntityResolver();
        resolver.addVersion("project", "0.1");

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

        // User properties
        final File file = new File(new File(new File(System.getProperty("user.home")), ".quokka"), "quokka.properties");

        if (!file.exists()) {
            File parentFile = file.getParentFile();
            System.out.println("Creating default properties: " + file.getAbsolutePath());
            Assert.isTrue(parentFile.exists() || parentFile.mkdirs(),
                "Cannot create quokka defaults dir: " + parentFile.getAbsolutePath());

            new VoidExceptionHandler() {
                    public void run() throws Exception {
                        new IOUtils().copyStream(ProjectParser.class.getClassLoader().getResourceAsStream("quokkadefaults.properties"),
                            new FileOutputStream(file));
                    }
                };
        }

        properties.putAll(getProperties(URLs.toURL(file)));

        // Ant properties ... those set by command line
        properties.putAll(antProperties);

        return properties;
    }

    private static AnnotatedProperties getProperties(URL url) {
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.load(url);

        return properties;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class PluginDependendencyTargetConverter extends AbstractProjectConverter {
        public PluginDependendencyTargetConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element element) {
            PluginDependencyTarget target = (PluginDependencyTarget)super.fromXml(element);
            List dependencies = Strings.commaSepList(element.getAttribute("depends"));

            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                String dependency = (String)i.next();
                target.addDependency(dependency);
            }

            return target;
        }
    }

    public static class AbstractProjectConverter extends ReflectionConverter {
        public AbstractProjectConverter(Class clazz) {
            super(clazz);
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

            Converter converter;

            // Dependency set
            // Important: parse first as dependency sets must be recursed first to discover profiles, overrids and paths
            Element dependencySetEl = projectEl.getChild("dependency-set");

            if (dependencySetEl != null) {
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

            if ((project.getName() == null) && (project.getArtifacts().size() > 0)) {
                project.setName(((Artifact)project.getArtifacts().iterator().next()).getId().getGroup());
            }

            Assert.isTrue(project.getName() != null, LocatorDomParser.getLocator(projectEl.getElement()),
                "The name attribute must be supplied if no artifacts are defined");

            return project;
        }

        private void addAutomaticProfiles() {
            addProfile("hasParent", "Automatic profile: activated if the project has been invoked by a parent",
                !topLevel);

            for (int i = 1; i <= 6; i++) {
                String version = "1." + i;
                addPropertyProfile("source" + version, "quokka.project.java.source", version);
                addPropertyProfile("target" + version, "quokka.project.java.target", version);
                addProfile("java" + version, "Automatic profile: activated if 'ant.java.version='" + version,
                    version.equals(projectProperties.get("ant.java.version")));
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

            if (dependencySet.getArtifact() != null) {
                traversedDependencySets.add(dependencySet.getArtifact().getId());
            }

            // Profiles
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("profile")).iterator(); i.hasNext();) {
                Element profileEl = (Element)i.next();
                converter = getConverter(Profile.class);

                Profile profile = (Profile)converter.fromXml(profileEl);
                dependencySet.addProfile(profile);
                project.addProfile(profile);
            }

            // Overrides
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("override")).iterator(); i.hasNext();) {
                Element overrideEl = (Element)i.next();
                converter = getConverter(ws.quokka.core.model.Override.class);

                Override override = (Override)converter.fromXml(overrideEl);
                override.setVersionRangeUnion(VersionRangeUnion.parse(overrideEl.getAttribute("version")));
                override.setWith(Version.parse(overrideEl.getAttribute("with")));
                dependencySet.addOverride(override);
                project.addOverride(override);
                verifyOverride(override, Override.SCOPE_ALL);
            }

            // Paths
            for (Iterator i = applyProfiles(dependencySetEl.getChildren("path")).iterator(); i.hasNext();) {
                Element pathEl = (Element)i.next();
                converter = getConverter(Path.class);

                Path path = (Path)converter.fromXml(pathEl);
                dependencySet.addPath(path);
            }

            // Nested dependency sets
            List dependencySetEls = applyProfiles(dependencySetEl.getChildren("dependency-set"));

            for (Iterator i = dependencySetEls.iterator(); i.hasNext();) {
                Element nestedSetEl = (Element)i.next();

                // TODO: Handle overrides
                // Get the dependency set from the repository
                converter = getConverter(RepoArtifactId.class);

                RepoArtifactId artifactId = ((RepoArtifactId)converter.fromXml(nestedSetEl)).mergeDefaults();
                artifactId = new RepoArtifactId(artifactId.getGroup(), artifactId.getName(), "jar",
                        artifactId.getVersion());
                artifactId = override(artifactId, Override.SCOPE_ALL);

                RepoArtifact artifact = ProjectParser.this.repository.resolve(artifactId);

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
                    nestedSet.setProperties(getProperties(url));

                    AnnotatedProperties temp = eagerProperties;
                    eagerProperties = new AnnotatedProperties();
                    eagerProperties.putAll(nestedSet.getProperties());
                    eagerProperties.putAll(temp);
                    activateAutomaticProfiles();
                }

                // Set the build resources
                nestedSet.addBuildResources(URLs.toURLEntries(artifact.getLocalCopy(),
                        "META-INF/quokka/" + nestedSet.getArtifact().getId().toPathString() + "/resources/"));
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

            for (Iterator i = dependencyEls.iterator(); i.hasNext();) {
                Element dependencyEl = (Element)i.next();
                converter = getConverter(PluginDependency.class);
                dependencySet.addDependency((PluginDependency)converter.fromXml(dependencyEl));
            }

            // Location of top level project properties, imports and resources are different to nested depsets
            if (dependencySet.getParent() == null) {
                // Set the properties
                //                URL url = getProjectProperties(projectFile);
                //                if (url != null) {
                //                    dependencySet.setProperties(url);
                //                }
                // Set the import file
                String name = projectFile.getAbsolutePath();
                File importFile = new File(name.substring(0, name.lastIndexOf("-quokka")) + ".xml");

                //                System.out.println("Looking for import: " + importFile.getAbsolutePath());
                if (importFile.exists()) {
                    //                    System.out.println("Import found");
                    dependencySet.setImportURL(URLs.toURL(importFile));
                }

                // Build resources
                dependencySet.addBuildResources(URLs.toURLEntries(projectFile.getParentFile(), "build/resources/"));
            }

            return dependencySet;
        }

        /*
         * Make sure that the override doesn't affect any dependency sets already traversed
         */
        private void verifyOverride(Override override, String scope) {
            for (Iterator i = traversedDependencySets.iterator(); i.hasNext();) {
                DependencySet set = (DependencySet)i.next();
                Assert.isTrue(!override.matches(scope, set.getArtifact().getId()), override.getLocator(),
                    override.toString() + " applies to a dependendency set that has already been traversed: "
                    + set.getArtifact().getId().toShortString()
                    + ". The override must be defined at a higher level (prior to traversal)");
            }
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
            dependency.setId(override(dependency.getId(), Override.SCOPE_ALL));

            return dependency;
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
                Assert.isTrue(target.isValid(), target.getLocator(),
                    "Target must specify both 'prefix' and 'template', or neither");
                dependency.addTarget(target);
            }

            return dependency;
        }
    }

    public static class ArtifactConverter extends AbstractProjectConverter {
        public ArtifactConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element artifactEl) {
            Artifact artifact = (Artifact)super.fromXml(artifactEl);
            Converter converter = getConverter(RepoArtifactId.class);
            RepoArtifactId id = (RepoArtifactId)converter.fromXml(artifactEl);
            id = id.merge((RepoArtifactId)getContext("defaultId")).mergeDefaults();
            Assert.isTrue(id.isValid(), id.getLocator(),
                "Values must be supplied for group, name, type & version attributes for an artifact: " + id);
            artifact.setId(id);

            // Exported paths
            List paths = Strings.commaSepList(artifactEl.getAttribute("paths"));

            for (Iterator i = paths.iterator(); i.hasNext();) {
                String path = (String)i.next();
                String[] tokens = Strings.split(path, ":");
                artifact.addExportedPath(tokens[0], (tokens.length > 1) ? tokens[1] : tokens[0]);
            }

            return artifact;
        }
    }
}
