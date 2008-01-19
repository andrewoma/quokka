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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.xml.Converter;
import ws.quokka.core.util.xml.Element;
import ws.quokka.core.util.xml.ReflectionConverter;
import ws.quokka.core.util.xml.XmlConverter;
import ws.quokka.core.version.Version;

import java.io.Writer;

import java.util.Iterator;
import java.util.List;


/**
 *
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
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public static XmlConverter getXmlConverter() {
        return xmlConverter;
    }

    public static Writer toXml(RepoArtifact artifact, final Writer writer) {
        new VoidExceptionHandler() {
                // TODO: revist all XML DOCTYPE and encoding handling ...
                public void run() throws Exception {
                    writer.write("<?xml version=\"1.0\"?>\n"
                        + "<!DOCTYPE artifact PUBLIC \"quokka.ws/dtd/repository-1.0-m01\" \"http://quokka.ws/dtd/repository-1.0-m01.dtd\">\n");
                }
            };

        return getXmlConverter().toXml(artifact, writer, "artifact");
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

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

    public static class RepoPathConverter extends ReflectionConverter {
        public RepoPathConverter(Class clazz) {
            super(clazz);
            addDefault("descendDefault", Boolean.TRUE);
            addDefault("mandatoryDefault", Boolean.TRUE);
        }
    }

    public static class RepoDependencyConverter extends ReflectionConverter {
        public RepoDependencyConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element dependencyEl) {
            RepoDependency dependency = (RepoDependency)super.fromXml(dependencyEl);

            Converter converter = getConverter(RepoArtifactId.class);
            dependency.setId(((RepoArtifactId)converter.fromXml(dependencyEl)).mergeDefaults());

            // Add path specs added in long form
            List pathSpecEls = dependencyEl.getChildren(PATH_SPEC_EL);

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

        public void toXml(Object object, Element element) {
            super.toXml(object, element);

            RepoDependency dependency = (RepoDependency)object;
            Converter converter = getConverter(RepoArtifactId.class);
            converter.toXml(dependency.getId(), element);

            converter = getConverter(RepoPathSpec.class);

            for (Iterator i = dependency.getPathSpecs().iterator(); i.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)i.next();
                converter.toXml(pathSpec, element.addChild(PATH_SPEC_EL));
            }
        }

        private void parseShorthand(RepoDependency dependency, String pathsSpecsAttr) {
            String[] pathSpecs = Strings.trim(Strings.splitTopLevel(pathsSpecsAttr, '(', ')', ','));

            for (int i = 0; i < pathSpecs.length; i++) {
                RepoPathSpec pathSpec = parseShorthand(pathSpecs[i]);
                dependency.addPathSpec(pathSpec);
            }
        }

        /**
         * Creates a path specification from the shorthand form. Care should be taken to only override the defaults
         * when a value is actually specified.
         */
        public RepoPathSpec parseShorthand(String pathSpec) {
            RepoPathSpec pathSpecification = new RepoPathSpec();

            String[] tokens = Strings.trim(Strings.splitIncludeDelimiters(pathSpec, "<+"));
            assertPathSpec((tokens.length >= 1) && (tokens.length <= 3), pathSpec);
            assertPathSpec(!isDelimeter(tokens[0], "<+") && !Strings.isBlank(tokens[0]), pathSpec);

            String toId = tokens[0];
            assertPathSpec(!toId.equals("?"), pathSpec);

            if (toId.endsWith("?")) {
                pathSpecification.setMandatory(Boolean.FALSE);
                toId = toId.substring(0, toId.length() - 1);
            }

            // toId[?][+|<][fromId][(options)]
            pathSpecification.setTo(toId);

            if (tokens.length >= 2) {
                assertPathSpec(isDelimeter(tokens[1], "<+"), pathSpec);
                pathSpecification.setDescend((tokens[1].equals("<")) ? Boolean.TRUE : Boolean.FALSE);
            }

            if (tokens.length == 3) {
                assertPathSpec(!isDelimeter(tokens[2], "<+") && !Strings.isBlank(tokens[0]), pathSpec);

                int optionsStart = tokens[2].indexOf('(');

                if (optionsStart == -1) {
                    if (!Strings.isBlank(tokens[2])) {
                        pathSpecification.setFrom(tokens[2]);
                    }
                } else {
                    assertPathSpec(tokens[2].charAt(tokens[2].length() - 1) == ')', pathSpec);
                    pathSpecification.setOptions(tokens[2].substring(optionsStart + 1, tokens[2].length() - 1).trim());

                    if (optionsStart != 0) {
                        pathSpecification.setFrom(tokens[2].substring(0, optionsStart).trim());
                    }
                }
            }

            // Hack until replaced with proper ANTLR grammar. This is the short shorthand form
            int optionsStart = toId.indexOf('(');

            if (optionsStart != -1) {
                pathSpecification.setTo(toId.substring(0, optionsStart));
                pathSpecification.setFrom("runtime");
                pathSpecification.setOptions(toId.substring(optionsStart + 1, toId.length() - 1).trim());
            }

            return pathSpecification;
        }

        private void assertPathSpec(boolean condition, String pathSpec) {
            Assert.isTrue(condition,
                "Path spec '" + pathSpec
                + "' is invalid. Valid syntax is: <toPathId> [<|+] [fromPathId] [(<option1>, ...)]");
        }

        private boolean isDelimeter(String string, String delimiters) {
            return (string.length() == 1) && (delimiters.indexOf(string.charAt(0)) != -1);
        }
    }

    public static class RepoArtifactConverter extends ReflectionConverter {
        public RepoArtifactConverter(Class clazz) {
            super(clazz);
            addExclusion("localCopy");
        }

        public Object fromXml(Element artifactEl) {
            RepoArtifact artifact = (RepoArtifact)super.fromXml(artifactEl);
            addContext("artifact", artifact);

            Converter converter = getConverter(RepoArtifactId.class);
            artifact.setId((RepoArtifactId)converter.fromXml(artifactEl));

            Element pathsEl = artifactEl.getChild("paths");

            if (pathsEl != null) {
                for (Iterator i = pathsEl.getChildren("path").iterator(); i.hasNext();) {
                    Element pathEl = (Element)i.next();
                    converter = getConverter(RepoPath.class);
                    artifact.addPath((RepoPath)converter.fromXml(pathEl));
                }
            }

            if (artifact.getPaths().size() == 0) {
                // Add a default path
                RepoPath path = new RepoPath("runtime", "Runtime path", true, true);
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
                        dependency.addPathSpec(new RepoPathSpec("runtime", path.getId(),
                                (path.isDescendDefault()) ? Boolean.TRUE : Boolean.FALSE,
                                (path.isMandatoryDefault()) ? Boolean.TRUE : Boolean.FALSE));
                    }
                }
            }

            return artifact;
        }

        public void toXml(Object object, Element artifactEl) {
            super.toXml(object, artifactEl);

            RepoArtifact artifact = (RepoArtifact)object;
            Converter converter = getConverter(RepoArtifactId.class);
            converter.toXml(artifact.getId(), artifactEl);

            if (artifact.getDependencies().size() != 0) {
                converter = getConverter(RepoDependency.class);

                Element dependenciesEl = artifactEl.addChild("dependencies");

                for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
                    converter.toXml(i.next(), dependenciesEl.addChild("dependency"));
                }
            }

            if (artifact.getPaths().size() != 0) {
                converter = getConverter(RepoPath.class);

                Element pathsEl = artifactEl.addChild("paths");

                for (Iterator i = artifact.getPaths().iterator(); i.hasNext();) {
                    converter.toXml(i.next(), pathsEl.addChild("path"));
                }
            }
        }
    }
}
