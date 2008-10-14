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


package ws.quokka.core.main.ant.task;

import org.apache.tools.ant.Task;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.main.ant.DefaultProjectModel;
import ws.quokka.core.main.ant.ProjectHelper;
import ws.quokka.core.model.Target;
import ws.quokka.core.repo_resolver.ResolvedPath;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.RepoPathSpec;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * ListPluginsTask is for the built-in "list-plugins" target that will list plugins that are
 * compatible with the running version of quokka
 */
public class ListPluginsTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private boolean overrideCore;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void execute() {
        repository = (Repository)getProject().getReference(ProjectHelper.REPOSITORY);
        overrideCore = "true".equals(getProject().getProperty("q.project.overrideCore"));

        RepoArtifactId id = getPluginId();
        Repository repository = (Repository)getProject().getReference(ProjectHelper.REPOSITORY);
        Collection ids = repository.listArtifactIds((id == null) ? null : id.getGroup(),
                (id == null) ? null : id.getName(), "plugin", true);

        log("\nAvailable Plugins");
        log("=================");

        ResolvedPath corePath = getCorePath();
        ResolvedPath existingPlugins = getExistingPlugins();

        boolean all = "true".equals(getProject().getProperty("all"));
        String verboseProperty = getProject().getProperty("verbose");
        boolean verbose = (verboseProperty == null) ? (id != null) : "true".equals(verboseProperty);

        boolean found = false;
        List overrides = getOverrides();

        for (Iterator i = getPluginGroups(ids).iterator(); i.hasNext();) {
            PluginGroup group = (PluginGroup)i.next();
            boolean foundGroup = processGroup(group, all, verbose, corePath, existingPlugins, overrides);
            found = found || foundGroup;
        }

        if (!found && !all) {
            log("There are no compatible plugins available. You can search for all plugins using the -Dfilter=all");
        }

        log("Versions may have additional status characters after them as shown below:");
        log("  !: One or more plugin dependencies are incompatible with the core or existing plugins.");
        log("     => You should not use any plugin versions with this status.");
        log("  *: One or more plugin dependencies will conflict and will need to be explicitly overridden.");
        log("     => If compatible, you may be able to use the plugin by specifying overrides.");
        log("        For quokka.core.* modules you can set the project property q.project.overrideCore=true.");
        log("        For plugin depencies you define an override, e.g. Force all plugins to use dev reports 0.3.1:");
        log("          <override group=\"quokka.plugin.devreport\" plugin-paths=\"*\" with=\"0.3.1\"/>");
        log("  >: One or more plugin dependencies have been overridden.");
        log("     => No action may be necessary. It is just a warning that if you use this plugin");
        log("        version, it will be using dependencies it wasn't explicitly tested against");

        if (!verbose) {
            log("For more information on conflicts, overrides and compatibility, use the -Dverbose=true option.");
        }

        if (!all) {
            log("By default only latest exact match, latest compatible match without overrides and latest");
            log("compatbile match are shown. To show all versions use the -Dall=true option.");
        }
    }

    private ResolvedPath getExistingPlugins() {
        Set plugins = new HashSet();
        DefaultProjectModel model = getProjectModel();

        for (Iterator i = model.getTargets().values().iterator(); i.hasNext();) {
            Target target = (Target)i.next();
            plugins.add(target.getPlugin().getArtifact());
        }

        return new ResolvedPath("Plugins", new ArrayList(plugins));
    }

    private boolean processGroup(PluginGroup group, boolean all, boolean verbose, ResolvedPath corePath,
        ResolvedPath existingPlugins, List overrides) {
        PluginGroupCompatibility compatibility = analyseCompatibility(group, corePath, existingPlugins, overrides);
        SortedMap results = compatibility.compatibilites;

        if (!all) {
            // Get the latest versions of the following
            results = compatibility.filter(false, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE); // Exact match
            results.putAll(compatibility.filter(false, Boolean.FALSE, Boolean.TRUE, null)); // Compatible, no conflict
            results.putAll(compatibility.filter(false, null, Boolean.TRUE, null)); // Compatible, may conflict
        }

        StringBuffer sb = new StringBuffer();
        RepoArtifactId id = group.id;
        String groupName = id.getGroup() + ":" + (id.isDefaultName() ? "" : (id.getName() + ":"));
        sb.append(Strings.pad(groupName, 34, ' ')).append(" ");

        if (verbose) {
            sb.append("\n    ").append(compatibility.mostRecent.getShortDescription());
        }

        for (Iterator i = results.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Version version = (Version)entry.getKey();
            CompatibilitySet compatibilitySet = (CompatibilitySet)entry.getValue();

            if (verbose) {
                sb.append("\n    ").append(version).append(compatibilitySet.getStatusFlag());

                int[] widths = new int[3];

                for (Iterator j = compatibilitySet.set.iterator(); j.hasNext();) {
                    DependencyCompatibility compat = (DependencyCompatibility)j.next();

                    if (!compat.original.getVersion().equals(compat.overridden)
                            || !compat.original.getVersion().equals(compat.runtime)) {
                        widths[0] = Math.max(widths[0],
                                (compat.original.getVersion().toString() + compat.getStatusFlag()).length());
                        widths[1] = Math.max(widths[1], compat.overridden.toString().length());
                        widths[2] = Math.max(widths[2], compat.runtime.toString().length());
                    }
                }

                for (Iterator j = compatibilitySet.set.iterator(); j.hasNext();) {
                    DependencyCompatibility compat = (DependencyCompatibility)j.next();

                    if (!compat.original.getVersion().equals(compat.overridden)
                            || !compat.original.getVersion().equals(compat.runtime)) {
                        sb.append("\n        ");
                        sb.append(Strings.pad(compat.original.getVersion() + compat.getStatusFlag(), widths[0] + 2, ' '));
                        sb.append(Strings.pad(compat.overridden.toString(), widths[1] + 2, ' '));
                        sb.append(Strings.pad(compat.runtime.toString(), widths[2] + 2, ' '));
                        sb.append(compat.original.getGroup()).append(":");
                        sb.append(compat.original.getName());
                    }
                }
            } else {
                sb.append(version).append(compatibilitySet.getStatusFlag()).append(" ");
            }
        }

        if (verbose) {
            sb.append("\n ");
        } else {
            sb.append("\n    ").append(compatibility.mostRecent.getShortDescription()).append("\n\n");
        }

        log(sb.toString());

        return true;
    }

    /**
     * Analyses the direct dependencies of the plugin to check the compatibility.
     * TODO: Support transitive dependencies without incurring a large performance overhead
     */
    private PluginGroupCompatibility analyseCompatibility(PluginGroup group, ResolvedPath corePath,
        ResolvedPath existingPlugins, List overrides) {
        PluginGroupCompatibility groupCompatibility = new PluginGroupCompatibility();

        for (Iterator i = group.versions.iterator(); i.hasNext();) {
            Version version = (Version)i.next();
            RepoArtifactId id = group.id.merge(new RepoArtifactId(null, null, null, version));
            RepoArtifact plugin = repository.resolve(id, false);

            CompatibilitySet compatibilitySet = new CompatibilitySet();
            groupCompatibility.compatibilites.put(version, compatibilitySet);

            anlaysePluginCompatibility(corePath, existingPlugins, overrides, plugin, compatibilitySet);

            if (!i.hasNext()) {
                groupCompatibility.mostRecent = plugin;
            }
        }

        return groupCompatibility;
    }

    private void anlaysePluginCompatibility(ResolvedPath corePath, ResolvedPath existingPlugins, List overrides,
        RepoArtifact plugin, CompatibilitySet compatibilitySet) {
        for (Iterator i = plugin.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();
            DependencyCompatibility compatibility = new DependencyCompatibility();
            compatibility.original = dependency.getId();

            if (dependency.getId().getType().equals("plugin")) {
                compatibilitySet.set.add(compatibility);
                compatibility.overridden = applyOverrides(plugin, dependency, overrides, corePath);

                for (Iterator k = existingPlugins.getArtifacts().iterator(); k.hasNext();) {
                    RepoArtifact existing = (RepoArtifact)k.next();
                    RepoArtifactId dId = dependency.getId();

                    if (existing.getId().matches(new RepoArtifactId(dId.getGroup(), dId.getName(), dId.getType(),
                                    (Version)null))) {
                        compatibility.runtime = existing.getId().getVersion();

                        break;
                    }
                }

                if (compatibility.runtime == null) {
                    compatibility.runtime = dependency.getId().getVersion(); // Nothing, so runtime will be this plugin
                }
            } else {
                compatibility.runtime = getCoreRuntime(corePath, dependency.getId());

                if (compatibility.runtime == null) {
                    continue; // Not a core dependency
                }

                compatibilitySet.set.add(compatibility);
                compatibility.overridden = applyOverrides(plugin, dependency, overrides, corePath);
            }
        }
    }

    private Version applyOverrides(RepoArtifact plugin, RepoDependency dependency, List overrides, ResolvedPath corePath) {
        // Apply project overrides
        for (Iterator i = overrides.iterator(); i.hasNext();) {
            ws.quokka.core.model.Override override = (ws.quokka.core.model.Override)i.next();

            if ((override.getWithVersion() != null) && override.matches(dependency.getId())) {
                Set paths = override.matchingPluginPaths(plugin.getId());

                if ((paths.size() == 1) && paths.iterator().next().equals("*")) {
                    return override.getWithVersion();
                }

                for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                    RepoPathSpec pathSpec = (RepoPathSpec)j.next();

                    if (paths.contains(pathSpec.getTo())) {
                        return override.getWithVersion();
                    }
                }
            }
        }

        // Override core dependencies
        if (overrideCore) {
            Version version = getCoreRuntime(corePath, dependency.getId());

            if (version != null) {
                return version;
            }
        }

        // No override, set to the original
        return dependency.getId().getVersion();
    }

    private Version getCoreRuntime(ResolvedPath corePath, RepoArtifactId id) {
        for (Iterator i = corePath.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifactId coreId = ((RepoArtifact)i.next()).getId();

            if (coreId.matches(new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), (Version)null))) {
                return coreId.getVersion();
            }
        }

        return null;
    }

    public ResolvedPath getCorePath() {
        DefaultProjectModel model = getProjectModel();

        return model.getCorePath();
    }

    private DefaultProjectModel getProjectModel() {
        return (DefaultProjectModel)getProject().getReference("q.projectModel");
    }

    public List getOverrides() {
        return getProjectModel().getOverrides();
    }

    /**
     * Returns a collection of groups, sorted by group then name
     */
    protected Collection getPluginGroups(Collection ids) {
        // Sorted by group, then name
        Map groups = new TreeMap(new Comparator() {
                    public int compare(Object o1, Object o2) {
                        RepoArtifactId lhs = (RepoArtifactId)o1;
                        RepoArtifactId rhs = (RepoArtifactId)o2;
                        int result = lhs.getGroup().compareTo(rhs.getGroup());
                        result = (result != 0) ? result : lhs.getName().compareTo(rhs.getName());

                        return result;
                    }
                });

        // Split the ids into groups
        for (Iterator i = ids.iterator(); i.hasNext();) {
            RepoArtifactId id = (RepoArtifactId)i.next();

            RepoArtifactId unversioned = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), (Version)null);
            PluginGroup group = (PluginGroup)groups.get(unversioned);

            if (group == null) {
                group = new PluginGroup();
                group.id = unversioned;
                groups.put(unversioned, group);
            }

            group.versions.add(id.getVersion());
        }

        return groups.values();
    }

    public void log(String msg) {
        getProject().log(msg); // Don't want the task prefix to apply
    }

    public RepoArtifactId getPluginId() {
        String id = getProject().getProperty("plugin");

        if (id == null) {
            return null;
        }

        String[] tokens = Strings.splitPreserveAllTokens(id, ":");
        Assert.isTrue((tokens.length >= 1) && (tokens.length <= 3), "plugin format is 'group[:name][:version]': " + id);

        String group = tokens[0];
        String name = (tokens.length >= 2) ? tokens[1] : RepoArtifactId.defaultName(group);
        String version = (tokens.length == 3) ? tokens[2] : null;

        return new RepoArtifactId(group, name, "plugin", (version == null) ? null : Version.parse(version));
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    private static class PluginGroup {
        RepoArtifactId id;
        SortedSet versions = new TreeSet();
    }

    private static class PluginGroupCompatibility {
        RepoArtifact mostRecent;
        SortedMap compatibilites = new TreeMap(); // <Version, CompatibilitySet>

        SortedMap filter(boolean all, Boolean conflict, Boolean compatible, Boolean overridden) {
            SortedMap filtered = new TreeMap();

            for (Iterator i = compatibilites.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Compatibility compat = (Compatibility)entry.getValue();

                if (match(conflict, compat.isConflict()) && match(compatible, compat.isCompatible())
                        && match(overridden, compat.isOverridden())) {
                    filtered.put(entry.getKey(), compat);
                }
            }

            if (!all) {
                for (Iterator i = filtered.entrySet().iterator(); i.hasNext();) {
                    i.next();

                    if (i.hasNext()) {
                        i.remove(); // Remove all but last
                    }
                }
            }

            return filtered;
        }

        private boolean match(Boolean filter, boolean value) {
            return (filter == null) || (filter.booleanValue() == value);
        }
    }

    /**
     * Defines how compatible a dependency is
     */
    private abstract static class Compatibility {
        /**
         * Returns true if a conflict will occur with the current project runtime
         */
        abstract boolean isConflict();

        /**
         * Returns true if the dependency is compatible with the current project runtime
         */
        abstract boolean isCompatible();

        /**
         * Returns true if the dependency has been overridden
         */
        abstract boolean isOverridden();

        /**
         * Returns a status flag indicating the above 3 states
         */
        String getStatusFlag() {
            return "" + (isCompatible() ? "" : "!") + (isConflict() ? "*" : "") + (isOverridden() ? ">" : "");
        }
    }

    /**
     * CompatibilitySet holds a set of Compatibility objects and returns the aggregated states
     */
    private static class CompatibilitySet extends Compatibility {
        SortedSet set = new TreeSet();

        boolean isConflict() {
            // True if any depencency conflicts
            for (Iterator i = set.iterator(); i.hasNext();) {
                DependencyCompatibility compatibility = (DependencyCompatibility)i.next();

                if (compatibility.isConflict()) {
                    return true;
                }
            }

            return false;
        }

        boolean isCompatible() {
            // True if all dependencies are compatible
            for (Iterator i = set.iterator(); i.hasNext();) {
                DependencyCompatibility compatibility = (DependencyCompatibility)i.next();

                if (!compatibility.isCompatible()) {
                    return false;
                }
            }

            return true;
        }

        boolean isOverridden() {
            // True if any dependency has been overridden
            for (Iterator i = set.iterator(); i.hasNext();) {
                DependencyCompatibility compatibility = (DependencyCompatibility)i.next();

                if (compatibility.isOverridden()) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * DependencyCompatibility holds the exact compatibility details for a given dependency
     * to allow verbose logging of what overrides are required
     */
    private static class DependencyCompatibility extends Compatibility implements Comparable {
        RepoArtifactId original;
        Version overridden;
        Version runtime;

        public int compareTo(Object o) {
            DependencyCompatibility other = (DependencyCompatibility)o;
            int result = original.getGroup().compareTo(other.original.getGroup());

            return (result != 0) ? result : original.getName().compareTo(other.original.getName());
        }

        boolean isConflict() {
            return !overridden.equals(runtime);
        }

        boolean isCompatible() {
            if (original.getType().equals("plugin")) {
                // Plugins are considered compatible if the major and minor versions match
                // as either the runtime or original plugins can be overridden to make them match
                return (original.getVersion().getMajor() == runtime.getMajor())
                && (original.getVersion().getMinor() == runtime.getMinor());
            } else {
                // For core dependencies, the runtime version must be >= original version
                // and < next minor version
                VersionRange compatible = VersionRange.parse("[" + original.getVersion() + ","
                        + original.getVersion().getMajor() + "." + (original.getVersion().getMinor() + 1) + ")");

                return compatible.isInRange(runtime);
            }
        }

        boolean isOverridden() {
            return !original.getVersion().equals(overridden);
        }
    }
}
