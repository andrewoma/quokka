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


package ws.quokka.core.repo_spi;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.xml.Converter;
import ws.quokka.core.util.xml.Element;
import ws.quokka.core.util.xml.ReflectionConverter;
import ws.quokka.core.util.xml.XmlConverter;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;


/**
 * RepoXmlConverter converts repository related artifacts to and from XML
 */
public class RepoXmlConverter {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String PATH_SPEC_EL = "path-spec";
    private static XmlConverter xmlConverter = new XmlConverter();

    static {
        xmlConverter.add(new RepoArtifactIdConverter(RepoArtifactId.class));
        xmlConverter.add(new ReflectionConverter(RepoPathSpec.class));
        xmlConverter.add(new RepoPathConverter(RepoPath.class));
        xmlConverter.add(new RepoDependencyConverter(RepoDependency.class));
        xmlConverter.add(new RepoArtifactConverter(RepoArtifact.class));
        xmlConverter.add(new RepoOverrideConverter(RepoOverride.class));
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the xml converter instance which can be used to convert all repository objects
     */
    public static XmlConverter getXmlConverter() {
        return xmlConverter;
    }

    /**
     * Convenience method to write an artifact to a file
     */
    public static Writer toXml(final RepoArtifact artifact, final File file) {
        return (Writer)new ExceptionHandler() {
                public Object run() throws IOException {
                    Writer writer = new BufferedWriter(new FileWriter(file));

                    try {
                        toXml(artifact, writer);
                    } finally {
                        writer.close();
                    }

                    return writer;
                }
            }.soften();
    }

    /**
     * Converts an artifact to XML and writes it to the writer given
     */
    public static Writer toXml(RepoArtifact artifact, final Writer writer) {
        return getXmlConverter().toXml(artifact, writer, "artifact", "quokka.ws/dtd/repository-0.2",
            "http://quokka.ws/dtd/repository-0.2.dtd");
    }

    private static RepoPathSpec createPathSpec(XmlConverter xmlConverter, String pathSpec, boolean toRequired) {
        RepoPathSpec spec = createPathSpec(xmlConverter);
        spec.parseShorthand(pathSpec, toRequired);

        return spec;
    }

    private static RepoPathSpec createPathSpec(XmlConverter xmlConverter) {
        ReflectionConverter converter = (ReflectionConverter)xmlConverter.getConverter(RepoPathSpec.class);

        return (RepoPathSpec)converter.construct();
    }

    private static RepoPathSpec createPathSpec(XmlConverter xmlConverter, String fromPath, String toPath,
        String options, Boolean descend, Boolean mandatory) {
        RepoPathSpec spec = createPathSpec(xmlConverter);
        spec.setFrom(fromPath);
        spec.setTo(toPath);
        spec.setOptions(options);
        spec.setDescend(descend);
        spec.setMandatory(mandatory);

        return spec;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * Converts RepoArtifactIds to and from XML
     */
    public static class RepoArtifactIdConverter extends ReflectionConverter {
        public RepoArtifactIdConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element element) {
            RepoArtifactId id = (RepoArtifactId)super.fromXml(element);
            String version = element.getAttribute("version");
            RepoArtifactId copy = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(),
                    (version == null) ? null : new Version(version));
            copy.setAnnotations(id.getAnnotations());

            return copy;
        }

        public void toXml(Object object, Element element) {
            super.toXml(object, element);

            RepoArtifactId id = (RepoArtifactId)object;

            if (id.getVersion() != null) {
                element.setAttribute("version", id.getVersion().toString());
            }
        }
    }

    /**
     * Converts RepoPaths to and from XML
     */
    public static class RepoPathConverter extends ReflectionConverter {
        public RepoPathConverter(Class clazz) {
            super(clazz);
            addDefault("descendDefault", Boolean.TRUE);
            addDefault("mandatoryDefault", Boolean.TRUE);
        }
    }

    /**
     * Converts RepoDependencies to and from XML
     */
    public static class RepoDependencyConverter extends ReflectionConverter {
        public RepoDependencyConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element dependencyEl) {
            RepoDependency dependency = (RepoDependency)super.fromXml(dependencyEl);

            Converter converter = getConverter(RepoArtifactId.class);
            dependency.setId(((RepoArtifactId)converter.fromXml(dependencyEl)).mergeDefaults());

            // Add path specs added in long form
            List pathSpecEls = filter(dependencyEl.getChildren(PATH_SPEC_EL));

            for (Iterator i = pathSpecEls.iterator(); i.hasNext();) {
                Element pathSpecEl = (Element)i.next();
                converter = getConverter(RepoPathSpec.class);

                RepoPathSpec spec = (RepoPathSpec)converter.fromXml(pathSpecEl);
                dependency.addPathSpec(spec);
            }

            // Add path specs added in short form
            String pathSpecs = dependencyEl.getAttribute("paths");

            if (pathSpecs != null) {
                parseShorthand(dependency, pathSpecs);
            }

            return dependency;
        }

        /**
         * Allows descendents to filter the pathspecs (e.g. get rid of some matching certain profiles)
         */
        public List filter(List pathSpecEls) {
            return pathSpecEls;
        }

        public void toXml(Object object, Element element) {
            super.toXml(object, element);

            RepoDependency dependency = (RepoDependency)object;
            Converter converter = getConverter(RepoArtifactId.class);
            converter.toXml(dependency.getId(), element);

            converter = getConverter(RepoPathSpec.class);

            RepoArtifact artifact = (RepoArtifact)getContext("artifact");
            StringBuffer pathSpecs = new StringBuffer();

            for (Iterator i = dependency.getPathSpecs().iterator(); i.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)i.next();
                pathSpecs.append(pathSpec.toShortHand(artifact.getPath(pathSpec.getTo())));

                if (i.hasNext()) {
                    pathSpecs.append(", ");
                }

                element.setAttribute("paths", pathSpecs.toString());
            }
        }

        public void parseShorthand(RepoDependency dependency, String pathsSpecsAttr) {
            String[] pathSpecs = Strings.trim(Strings.splitTopLevel(pathsSpecsAttr, '(', ')', ','));
            RepoArtifact artifact = (RepoArtifact)getContext("artifact");

            for (int i = 0; i < pathSpecs.length; i++) {
                RepoPathSpec pathSpec = createPathSpec(getXmlConverter(), pathSpecs[i], toRequired());

                if (artifact != null) {
                    pathSpec.mergeDefaults(artifact.getPath(pathSpec.getTo()));
                }

                dependency.addPathSpec(pathSpec);
            }
        }

        /**
         * Override to control how the path specification should be parsed
         */
        public boolean toRequired() {
            return true;
        }
    }

    /**
     * Converts RepoArtifacts to and from XML
     */
    public static class RepoArtifactConverter extends ReflectionConverter {
        private static final String LICENSE = "license";
        private static final String LICENSES = "licenses";
        private static final String CONFLICTS = "conflicts";
        private static final String CONFLICT = "conflict";
        private static final String KIND = "kind";

        public RepoArtifactConverter(Class clazz) {
            super(clazz);
            addExclusion("localCopy");
            addExclusion("description");
            addExclusion("hash");
            addExclusion("importedFrom");
            addDefault("stub", Boolean.FALSE);
        }

        public DateFormat getDateFormat() {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));

            return format;
        }

        public Object fromXml(Element artifactEl) {
            RepoArtifact artifact = (RepoArtifact)super.fromXml(artifactEl);
            addContext("artifact", artifact);

            Converter converter = getConverter(RepoArtifactId.class);
            artifact.setId((RepoArtifactId)converter.fromXml(artifactEl));

            String timestamp = artifactEl.getAttribute("timestamp");

            if (timestamp != null) {
                try {
                    artifact.setTimestamp(getDateFormat().parse(timestamp));
                } catch (ParseException e) {
                    throw new BuildException(e);
                }
            }

            Element conflictsEl = artifactEl.getChild(CONFLICTS);

            if (conflictsEl != null) {
                for (Iterator i = conflictsEl.getChildren(CONFLICT).iterator(); i.hasNext();) {
                    Element conflictEl = (Element)i.next();
                    RepoConflict conflict = new RepoConflict();
                    RepoArtifactId id = (RepoArtifactId)getConverter(RepoArtifactId.class).fromXml(conflictEl);
                    conflict.setId(id.merge(new RepoArtifactId(null, null, artifact.getId().getType(), (Version)null))
                        .mergeDefaults());
                    conflict.setKind(conflictEl.getAttribute(KIND));
                    conflict.validate();
                    artifact.addConflict(conflict);
                }
            }

            Element pathsEl = artifactEl.getChild("paths");

            if (pathsEl != null) {
                for (Iterator i = pathsEl.getChildren("path").iterator(); i.hasNext();) {
                    Element pathEl = (Element)i.next();
                    converter = getConverter(RepoPath.class);
                    artifact.addPath((RepoPath)converter.fromXml(pathEl));
                }
            }

            Element licensesEl = artifactEl.getChild(LICENSES);

            if (licensesEl != null) {
                for (Iterator i = licensesEl.getChildren(LICENSE).iterator(); i.hasNext();) {
                    Element licenseEl = (Element)i.next();
                    RepoArtifactId license = ((RepoArtifactId)getConverter(RepoArtifactId.class).fromXml(licenseEl));
                    license = license.merge(new RepoArtifactId(null, RepoArtifactId.defaultName(license.getGroup()),
                                LICENSE, (Version)null));
                    license.validate();
                    Assert.isTrue(license.getType().equals(LICENSE) || license.getType().equals("notice"),
                        license.getLocator(),
                        "Only types of 'license' and 'notice' are permitted for licenses: license="
                        + license.toShortString());
                    artifact.addLicense(license);
                }
            }

            if (artifact.getPaths().size() == 0) {
                // Add a default path
                RepoPath path = (RepoPath)((ReflectionConverter)xmlConverter.getConverter(RepoPath.class)).construct();
                path.setId("runtime");
                path.setDescription("Runtime path");
                path.setMandatoryDefault(true);
                path.setDescendDefault(true);
                artifact.addPath(path);
            }

            Element dependenciesEl = artifactEl.getChild("dependencies");

            if (dependenciesEl != null) {
                for (Iterator i = dependenciesEl.getChildren("dependency").iterator(); i.hasNext();) {
                    Element dependencyEl = (Element)i.next();
                    converter = getConverter(RepoDependency.class);

                    RepoDependency dependency = (RepoDependency)converter.fromXml(dependencyEl);
                    artifact.addDependency(dependency);

                    // Verify dependencies match the paths
                    for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                        RepoPathSpec pathSpec = (RepoPathSpec)j.next();
                        RepoPath path = artifact.getPath(pathSpec.getTo());
                        Assert.isTrue(path != null, dependency.getLocator(),
                            "Path spec refers to an undefined path '" + pathSpec.getTo() + "'");
                        pathSpec.mergeDefaults(path);
                    }

                    // Add a default path specification if none is specified
                    if (dependency.getPathSpecs().size() == 0) {
                        Assert.isTrue(artifact.getPaths().size() == 1, dependency.getLocator(),
                            "A path spec must be defined for dependency '" + dependency.getId() + "'");

                        RepoPath path = (RepoPath)artifact.getPaths().iterator().next();
                        dependency.addPathSpec(createPathSpec(getXmlConverter(), "runtime", path.getId(), null,
                                (path.isDescendDefault()) ? Boolean.TRUE : Boolean.FALSE,
                                (path.isMandatoryDefault()) ? Boolean.TRUE : Boolean.FALSE));
                    }
                }
            }

            Element overridesEl = artifactEl.getChild("overrides");

            if (overridesEl != null) {
                for (Iterator i = overridesEl.getChildren("override").iterator(); i.hasNext();) {
                    Element overrideEl = (Element)i.next();
                    converter = getConverter(RepoOverride.class);

                    RepoOverride override = (RepoOverride)converter.fromXml(overrideEl);
                    artifact.addOverride(override);
                }
            }

            Element descriptionEl = artifactEl.getChild("description");

            if (descriptionEl != null) {
                artifact.setDescription(descriptionEl.getText());
            }

            Element importedFromEl = artifactEl.getChild("imported-from");

            if (importedFromEl != null) {
                artifact.setImportedFrom(importedFromEl.getText());
            }

            return artifact;
        }

        public void toXml(Object object, Element artifactEl) {
            super.toXml(object, artifactEl);

            RepoArtifact artifact = (RepoArtifact)object;
            addContext("artifact", artifact);

            Converter converter = getConverter(RepoArtifactId.class);
            converter.toXml(artifact.getId(), artifactEl);

            if (artifact.getTimestamp() != null) {
                artifactEl.setAttribute("timestamp", getDateFormat().format(artifact.getTimestamp()));
            }

            if (artifact.getDescription() != null) {
                artifactEl.addChild("description").addText(artifact.getDescription());
            }

            if (artifact.getPaths().size() != 0) {
                converter = getConverter(RepoPath.class);

                Element pathsEl = artifactEl.addChild("paths");

                for (Iterator i = artifact.getPaths().iterator(); i.hasNext();) {
                    converter.toXml(i.next(), pathsEl.addChild("path"));
                }
            }

            if (artifact.getDependencies().size() != 0) {
                converter = getConverter(RepoDependency.class);

                Element dependenciesEl = artifactEl.addChild("dependencies");

                for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
                    converter.toXml(i.next(), dependenciesEl.addChild("dependency"));
                }
            }

            if (artifact.getOverrides().size() != 0) {
                converter = getConverter(RepoOverride.class);

                Element overridesEl = artifactEl.addChild("overrides");

                for (Iterator i = artifact.getOverrides().iterator(); i.hasNext();) {
                    converter.toXml(i.next(), overridesEl.addChild("override"));
                }
            }

            if (artifact.getLicenses().size() != 0) {
                Element licensesEl = artifactEl.addChild(LICENSES);

                for (Iterator i = artifact.getLicenses().iterator(); i.hasNext();) {
                    RepoArtifactId license = (RepoArtifactId)i.next();
                    Element licenseEl = licensesEl.addChild(LICENSE);

                    if (license.getGroup() != null) {
                        licenseEl.setAttribute("group", license.getGroup());
                    }

                    if ((license.getName() != null)
                            && !license.getName().equals(RepoArtifactId.defaultName(license.getGroup()))) {
                        licenseEl.setAttribute("name", license.getName());
                    }

                    if (license.getVersion() != null) {
                        licenseEl.setAttribute("version", license.getVersion().toString());
                    }
                }
            }

            if (artifact.getConflicts().size() != 0) {
                Element conflictsEl = artifactEl.addChild(CONFLICTS);

                for (Iterator i = artifact.getConflicts().iterator(); i.hasNext();) {
                    RepoConflict conflict = (RepoConflict)i.next();
                    Element conflictEl = conflictsEl.addChild(CONFLICT);
                    conflictEl.setAttribute(KIND, conflict.getKind());
                    getConverter(RepoArtifactId.class).toXml(conflict.getId(), conflictEl);
                }
            }

            if (artifact.getImportedFrom() != null) {
                artifactEl.addChild("imported-from").addText(artifact.getImportedFrom());
            }
        }
    }

    /**
     * Converts RepoOverrides to and from XML
     */
    public static class RepoOverrideConverter extends ReflectionConverter {
        public RepoOverrideConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element overrideEl) {
            RepoOverride override = (RepoOverride)super.fromXml(overrideEl);
            String version = overrideEl.getAttribute("version");
            override.setVersion((version == null) ? null : VersionRangeUnion.parse(version));

            String with = overrideEl.getAttribute("with");
            override.setWithVersion((with == null) ? null : Version.parse(with));

            String withPaths = overrideEl.getAttribute("with-paths");

            if (withPaths != null) {
                String[] pathSpecs = Strings.trim(Strings.splitTopLevel(withPaths, '(', ')', ','));

                for (int i = 0; i < pathSpecs.length; i++) {
                    RepoPathSpec pathSpec = createPathSpec(getXmlConverter(), pathSpecs[i], false);
                    override.addWithPathSpec(pathSpec);
                }
            }

            // Add path specs added in long form
            List pathSpecEls = overrideEl.getChildren(PATH_SPEC_EL);

            for (Iterator i = pathSpecEls.iterator(); i.hasNext();) {
                Element pathSpecEl = (Element)i.next();
                Converter converter = getConverter(RepoPathSpec.class);

                RepoPathSpec spec = (RepoPathSpec)converter.fromXml(pathSpecEl);
                Assert.isTrue(spec.getTo() == null, spec.getLocator(), "'to' attribute is not valid for overrides");
                override.addWithPathSpec(spec);
            }

            String paths = overrideEl.getAttribute("paths");

            if (paths != null) {
                for (Iterator i = Strings.commaSepList(paths).iterator(); i.hasNext();) {
                    String path = (String)i.next();
                    override.addPath(path);
                }
            }

            return override;
        }

        public void toXml(Object object, Element element) {
            super.toXml(object, element);

            RepoOverride override = (RepoOverride)object;
            element.setAttribute("paths", Strings.join(override.getPaths().iterator(), ", "));

            if (override.getVersion() != null) {
                element.setAttribute("version", override.getVersion().toString());
            }

            if (override.getWithVersion() != null) {
                element.setAttribute("with", override.getWithVersion().toString());
            }

            StringBuffer pathSpecs = new StringBuffer();

            for (Iterator i = override.getWithPathSpecs().iterator(); i.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)i.next();
                pathSpecs.append(pathSpec.toShortHand());

                if (i.hasNext()) {
                    pathSpecs.append(", ");
                }

                element.setAttribute("with-paths", pathSpecs.toString());
            }
        }
    }
}
