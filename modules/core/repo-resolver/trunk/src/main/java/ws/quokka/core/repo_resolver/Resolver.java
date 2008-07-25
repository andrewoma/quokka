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


package ws.quokka.core.repo_resolver;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.repo_spi.*;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;

import java.util.*;


/**
 *
 */
public class Resolver {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String DECLARED_BY = "declaredBy";
    private static final String PARENT = "parent";
    private static final String CONFLICT = "conflict";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private Logger log;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Resolver(Repository repository, Logger log) {
        this.repository = repository;
        this.log = log;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public ResolvedPath resolvePath(String pathId, RepoArtifact artifact) {
        Assert.isTrue(artifact.getPath(pathId) != null, "Path '" + pathId + "' does not exist in artifact: " + artifact);

        ResolvedPath path = new ResolvedPath();
        path.setId("Path: " + pathId); // Note: this may be overridden by something more contextually relevant

        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();
            List overrides = filterOverrides(pathId, artifact.getOverrides());
            dependency = applyOverrides(dependency, overrides);

            Set pathSpecs = dependency.getPathSpecsTo(pathId);

            for (Iterator j = pathSpecs.iterator(); j.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)j.next();
                resolvePath(path, pathSpec, new HashSet(), false, null, overrides);
            }
        }

        return path;
    }

    /**
     * Filters the overrides to those that match the path. It then sets the path to a wildcard so that
     * the override applies to all descendents.
     */
    private List filterOverrides(String pathId, List overrides) {
        List filtered = new ArrayList();

        for (Iterator i = overrides.iterator(); i.hasNext();) {
            RepoOverride override = (RepoOverride)i.next();

            if (override.matches(pathId)) {
                Set wildcard = new HashSet(Collections.singleton("*"));
                filtered.add(new RepoOverride(wildcard, override.getGroup(), override.getName(), override.getType(),
                        override.getVersion(), override.getWithVersion(), override.getWithPathSpecs()));
            }
        }

        return filtered;
    }

    private RepoDependency applyOverrides(RepoDependency dependency, List overrides) {
        for (Iterator i = overrides.iterator(); i.hasNext();) {
            RepoOverride override = (RepoOverride)i.next();

            if (override.matches(dependency.getId())) {
                RepoDependency overridden = new RepoDependency();

                // Override version if specified
                overridden.setId((override.getWithVersion() == null) ? dependency.getId()
                                                                     : override(dependency.getId(),
                        override.getWithVersion()));

                // Copy and possibly override path specifications
                for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                    RepoPathSpec pathSpec = (RepoPathSpec)j.next();
                    RepoPathSpec overiddenPathSpec = override.getOverridden(pathSpec);

                    if (overiddenPathSpec != null) {
                        overiddenPathSpec.setTo(pathSpec.getTo());

                        // Use the existing descend and mandatory values if none were explicitly defined
                        if (overiddenPathSpec.isDescend() == null) {
                            overiddenPathSpec.setDescend(pathSpec.isDescend());
                        }

                        if (overiddenPathSpec.isMandatory() == null) {
                            overiddenPathSpec.setMandatory(pathSpec.isMandatory());
                        }

                        log.verbose("Overriding path spec for dependency=" + dependency.toShortString() + " from '"
                            + pathSpec.toShortString() + "' to '" + overiddenPathSpec.toShortString() + "'");

                        pathSpec = overiddenPathSpec;
                    }

                    RepoPathSpec copy = new RepoPathSpec(pathSpec.getFrom(), pathSpec.getTo(), pathSpec.getOptions(),
                            pathSpec.isDescend(), pathSpec.isMandatory());
                    overridden.addPathSpec(copy);
                }

                return overridden; // First matching override wins
            }
        }

        return dependency;
    }

    private void resolvePath(ResolvedPath path, RepoPathSpec pathSpec, Set options, boolean force,
        RepoArtifactId declaredBy, List overrides) {
        // TODO: do proper cycle detection
        if (path.getArtifacts().size() > 150) {
            StringBuffer sb = new StringBuffer("Cycle detected!\n");

            for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
                RepoArtifact artifact = (RepoArtifact)i.next();
                sb.append(artifact.getId().toShortString()).append("\n");
            }

            log.error(sb.toString());
            throw new BuildException("Dependency cycle detected. Quokka does not support circular dependencies");
        }

        if (pathSpec.getOptions() != null) {
            options.add(pathSpec.getOptions());
        }

        if ((options.size() == 0) && !pathSpec.isMandatory().booleanValue() && !force) {
            return; // The artifact is not mandatory and has not been added as an option
        }

        // Add the artifact to the path
        RepoArtifact artifact = getArtifact(pathSpec.getDependency().getId());
        artifact.getId().getAnnotations().put(DECLARED_BY, declaredBy);

//        System.out.println("Adding: " + artifact.getId().toShortString() + " declared by " + (declaredBy == null ? "" : declaredBy.toShortString()));
        path.add(artifact);

        if ((options.size() == 0) && !pathSpec.isDescend().booleanValue()) {
            return; // Not required to descend and no options to force it
        }

        // Process dependencies
        Set topLevelOptions = splitTopLevelOptions(options);

        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();
            List combinedOverrides = new ArrayList(overrides);
            combinedOverrides.addAll(filterOverrides(pathSpec.getFrom(), artifact.getOverrides()));
            dependency = applyOverrides(dependency, combinedOverrides);

            for (Iterator j = dependency.getPathSpecsTo(pathSpec.getFrom()).iterator(); j.hasNext();) {
                RepoPathSpec dependencyPathSpec = (RepoPathSpec)j.next();

                // This dependency is path of the path
                Set matchingOptions = new HashSet();
                Version override = findMatchingOptions(artifact, dependencyPathSpec, topLevelOptions, matchingOptions);

                // Handle explicit overrides
                if (override != null) {
                    dependency.setId(override(dependency.getId(), override));
                }

                if (pathSpec.isDescend().booleanValue() || (matchingOptions.size() > 0)) {
                    resolvePath(path, dependencyPathSpec, nextLevelOptions(matchingOptions),
                        matchingOptions.size() > 0, artifact.getId(), combinedOverrides);
                }
            }
        }

        Assert.isTrue(topLevelOptions.size() == 0, pathSpec.getLocator(),
            "Options do not match dependencies of artifact: artifact=" + artifact.getId() + ", options="
            + topLevelOptions + ", dependencies=" + artifact.getDependencies());
    }

    private RepoArtifact getArtifact(RepoArtifactId artifactId) {
        return (RepoArtifact)repository.resolve(artifactId).clone(); // Clone to allow additional annotations to be added within context
    }

    private Set splitTopLevelOptions(Set options) {
        Set topLevelOptions = new HashSet();

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();
            String[] split = Strings.splitTopLevel(option, '(', ')', ',');
            topLevelOptions.addAll(Arrays.asList(split));
        }

        return topLevelOptions;
    }

    private Version findMatchingOptions(RepoArtifact artifact, RepoPathSpec pathSpec, Set options, Set matching) {
        //        System.out.println("    matching options: options=" + options + ", artifact=" + artifact.getId() + ", pathSpec=" + pathSpec);
        Version override = null;

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();

            // Find matching artifact dependency
            String[] groupName = Strings.split(Strings.split(option, "(")[0], RepoArtifactId.ID_SEPARATOR);
            String name = groupName[0].trim();
            String group = (groupName.length > 1) ? groupName[1] : null;
            String[] nameVersion = Strings.split(name, "@");
            Assert.isTrue((nameVersion.length == 1) || (nameVersion.length == 2),
                "Invalid option format: valid format is [group][:]<name>[@][version]: options=" + option);

            if (nameVersion.length == 2) {
                name = nameVersion[0];

                Version version = new Version(nameVersion[1]);
                Assert.isTrue((override == null) || override.equals(version),
                    "Multiple overrides are specified for " + artifact.getId().toShortString()
                    + " that are inconsistent: " + version + " and " + override);
                override = version;
            }

            if (matches(group, name, artifact, pathSpec)) {
                i.remove(); // To see if any remain unmatched later

                //                System.out.println("Matched " + dependency + " to " + option);
                matching.add(option);
            }
        }

        return override;
    }

    private boolean matches(String group, String name, RepoArtifact artifact, RepoPathSpec pathSpec) {
        RepoArtifactId id = pathSpec.getDependency().getId();

        if (group != null) {
            return id.getGroup().equals(group) && id.getName().equals(name); // Exact match
        }

        if (!id.getName().equals(name)) {
            return false; // names don't match
        }

        // Make sure matching by name is unambiguos for all dependencies in the path
        int count = 0;

        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec dependencyPathSpec = (RepoPathSpec)j.next();

                if (dependencyPathSpec.getTo().equals(pathSpec.getTo())) {
                    // This dependency is path of the path
                    if (id.getName().equals(dependencyPathSpec.getDependency().getId().getName())) {
                        count++;
                    }

                    Assert.isTrue(count <= 1,
                        "Option does not uniquely identify the dependency. Specify the group as well");
                }
            }
        }

        return true;
    }

    private RepoArtifactId override(RepoArtifactId id, Version version) {
        log.debug("Overriding " + id.toShortString() + " to " + version);

        RepoArtifactId overridden = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), version);
        overridden.setAnnotations((Annotations)id.getAnnotations().clone());

        return overridden;
    }

    private Set nextLevelOptions(Set options) {
        Set nextLevel = new HashSet();

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();

            if (option.indexOf("(") > 0) {
                nextLevel.add(option.substring(option.indexOf('(') + 1, option.lastIndexOf(')')));
            }
        }

        return nextLevel;
    }

    public ResolvedPath merge(Collection paths) {
        // Build a map of all artifacts based on unversioned ids
        StringBuffer id = new StringBuffer("Merged: [");
        Map merged = new HashMap();

        for (Iterator i = paths.iterator(); i.hasNext();) {
            ResolvedPath path = (ResolvedPath)i.next();
            id.append(path.getId());

            if (i.hasNext()) {
                id.append(", ");
            }

            for (Iterator j = path.getArtifacts().iterator(); j.hasNext();) {
                RepoArtifact artifact = (RepoArtifact)j.next();
                setConflict(artifact.getId(), null); // Clear conflict annotation

                RepoArtifactId unversioned = toUnversionedId(artifact.getId());
                List artifacts = (List)merged.get(unversioned);

                if (artifacts == null) {
                    artifacts = new ArrayList();
                    merged.put(unversioned, artifacts);
                }

                artifacts.add(artifact);
            }
        }

        id.append("]");

        boolean conflict = false;
        ResolvedPath mergedPath = new ResolvedPath();
        mergedPath.setId(id.toString());

        // Make sure all artifacts are of the same version
        int conflictIndex = 1;
        Set versions = new HashSet();

        for (Iterator i = merged.values().iterator(); i.hasNext();) {
            List artifacts = (List)i.next();
            versions.clear();

            for (Iterator j = artifacts.iterator(); j.hasNext();) {
                RepoArtifact artifact = (RepoArtifact)j.next();
                versions.add(artifact.getId().getVersion());
            }

            if (versions.size() == 1) {
                RepoArtifact artifact = (RepoArtifact)artifacts.iterator().next();
                artifact.getId().getAnnotations().remove(DECLARED_BY); // In case the merged path is merged again or printed
                mergedPath.add(artifact); // Any artifact will do ... all are equal
            } else {
                // Conflict detected.
                conflict = true;
                markConflicted(artifacts, conflictIndex++);
            }
        }

        if (conflict) {
            throw new BuildException("Conflicts have occurred between the following artifacts:\n"
                + formatPaths(paths, true));
        } else {
            return mergedPath;
        }
    }

    /**
     * Marks all the artifacts as conflicted using an annotation. It also marks the parents to
     * enable just the conflicting paths to be printed later.
     */
    private void markConflicted(List artifacts, int conflictIndex) {
        for (Iterator i = artifacts.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            setConflict(artifact.getId(), Integer.toString(conflictIndex));

            for (RepoArtifactId id = getDeclaredBy(artifact.getId()); id != null; id = getDeclaredBy(id)) {
                String conflict = getConflict(id);

                if (conflict == null) {
                    setConflict(id, PARENT);
                }
            }
        }
    }

    private void setConflict(RepoArtifactId artifactId, String value) {
        if (value == null) {
            artifactId.getAnnotations().remove(CONFLICT);
        } else {
            artifactId.getAnnotations().put(CONFLICT, value);
        }
    }

    private String getConflict(RepoArtifactId artifactId) {
        return (String)artifactId.getAnnotations().get(CONFLICT);
    }

    private RepoArtifactId getDeclaredBy(RepoArtifactId artifactId) {
        return (RepoArtifactId)artifactId.getAnnotations().get(DECLARED_BY);
    }

    public String formatPaths(Collection paths, boolean onlyConflicted) {
        StringBuffer sb = new StringBuffer();

        for (Iterator i = paths.iterator(); i.hasNext();) {
            ResolvedPath path = (ResolvedPath)i.next();
            sb.append(formatPath(path, onlyConflicted));
        }

        return sb.toString();
    }

    public String formatPath(ResolvedPath path, boolean onlyConflicted) {
        StringBuffer sb = new StringBuffer();

        Map roots = new TreeMap();

        for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            RepoArtifactId declaredBy = getDeclaredBy(artifact.getId());

            if (declaredBy == null) { // Find root nodes
                roots.put(artifact.getId(), artifact);
            }
        }

        for (Iterator i = roots.values().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            formatPath(path.getArtifacts(), artifact, onlyConflicted, sb, "    ");
        }

        if (sb.length() != 0) {
            return path.getId() + "\n" + sb.toString();
        }

        return "";
    }

    private void formatPath(List artifacts, RepoArtifact artifact, boolean onlyConflicted, StringBuffer sb,
        String indent) {
        Map children = new TreeMap();
        String conflict = getConflict(artifact.getId());

        if (!onlyConflicted || (conflict != null)) {
            String conflictId = ((conflict != null) && !conflict.equals(PARENT)) ? (" (conflict " + conflict + ")") : "";
            sb.append(indent).append(artifact.getId().toShortString()).append(conflictId).append("\n");

            for (Iterator i = artifacts.iterator(); i.hasNext();) {
                RepoArtifact child = (RepoArtifact)i.next();

                if (artifact.getId().equals(getDeclaredBy(child.getId()))) {
                    children.put(child.getId(), child);
                }
            }
        }

        for (Iterator i = children.values().iterator(); i.hasNext();) {
            RepoArtifact child = (RepoArtifact)i.next();
            formatPath(artifacts, child, onlyConflicted, sb, indent + "    ");
        }
    }

    private RepoArtifactId toUnversionedId(RepoArtifactId id) {
        return new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), Version.parse("0"));
    }
}
