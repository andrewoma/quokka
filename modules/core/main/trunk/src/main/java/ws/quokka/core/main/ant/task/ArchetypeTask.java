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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;

import ws.quokka.core.bootstrap_util.ArtifactPropertiesParser;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.bootstrap_util.PropertiesUtil;
import ws.quokka.core.main.ant.ProjectHelper;
import ws.quokka.core.plugin_spi.support.AntUtils;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryAware;
import ws.quokka.core.repo_spi.RepositoryFactory;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.URLs;
import ws.quokka.core.util.xml.Document;
import ws.quokka.core.util.xml.Element;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRange;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;


/**
 *
 */
public class ArchetypeTask extends Task implements RepositoryAware {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private IOUtils ioUtils = new IOUtils();
    private AntUtils utils;
    private Logger logger;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void init() throws BuildException {
        logger = new ProjectLogger(getProject());
        utils = new AntUtils(getProject());
    }

    public void execute() throws BuildException {
        selectRepository();

        RepoArtifactId id = getArtifactId();

        // If no id, list the available archetypes
        if (id == null) {
            listArchetypes(!"true".equals(getProject().getProperty("all")));

            return;
        }

        // If the version is null, look for a compatible id
        if (id.getVersion() == null) {
            Collection ids = repository.listArtifactIds(id.getGroup(), id.getName(), "archetype", true);
            Assert.isTrue(ids.size() != 0,
                "There are no archetypes in the repository with an id of '" + id.toShortString() + "'");

            ArchetypeGroup group = (ArchetypeGroup)getArchetypes(ids).iterator().next();
            Assert.isTrue(group.compatible.size() != 0,
                "There appear to be no compatible versions. Specify the version explicitly if you wish to force it.\n"
                + "Available versions are: " + group.incompatible);

            // Choose the most current compatible
            List list = new ArrayList(group.compatible);
            Collections.reverse(list);
            id = new RepoArtifactId(null, null, null, (Version)list.get(0)).merge(id);
        }

        logger.info("Creating project based on artifact: " + id.toShortString());

        RepoArtifact artifact = repository.resolve(id);

        String root = "META-INF/quokka/" + artifact.getId().toPathString() + "/";

        File localCopy = artifact.getLocalCopy();

        if (checkProperties(localCopy, root)) {
            logger.info("Extracting the following archetype to " + getProject().getBaseDir().getAbsolutePath() + ":");
            displayEntries(localCopy, root);
            copyEntries(localCopy, root);
        }
    }

    private void listArchetypes(boolean onlyCompatible) {
        Collection ids = repository.listArtifactIds(null, null, "archetype", true);
        StringBuffer sb = new StringBuffer("\nAvailable Archetypes");
        sb.append("\n====================\n");

        for (Iterator i = getArchetypes(ids).iterator(); i.hasNext();) {
            ArchetypeGroup group = (ArchetypeGroup)i.next();

            if (onlyCompatible && (group.compatible.size() == 0)) {
                continue;
            }

            RepoArtifactId id = group.mostRecent.getId();
            String groupName = id.getGroup() + ":" + (id.isDefaultName() ? "" : (id.getName() + ":"));
            sb.append(Strings.pad(groupName, 34, ' ')).append(" ").append(Strings.join(group.compatible.iterator(), " "));
            sb.append((group.compatible.size() == 0) ? "" : "* ");

            if (!onlyCompatible) {
                sb.append(Strings.join(group.incompatible.iterator(), " "));
            }

            sb.append("\n    ").append(group.mostRecent.getShortDescription()).append("\n");
        }

        if (onlyCompatible) {
            sb.append("\nThe archetypes listed above are probably compatible with your quokka version.");
            sb.append("\nVersions marked with '*' are recommended and will be selected automatically if you");
            sb.append("\nrequest an archetype without the version. Use -Dall=true to show all versions.");
        }

        sb.append("\nTo use an archtype type: $ quokka archetype -Darchetype=group[:name][:version] [-Dprop1=value1]");
        sb.append("\n    e.g. $ quokka archetype -Darchetype=quokka.archetype.jar -Dgroup=mycompany.myproject");

        getProject().log(sb.toString());
    }

    /**
     * Returns a list of Archetypes sorted by group then name.
     */
    protected Collection getArchetypes(Collection ids) {
        Version coreVersion = getCoreVersion();

        // Sorted by group, then name
        Map archetypes = new TreeMap(new Comparator() {
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
            RepoArtifact artifact = repository.resolve(id, false);

            RepoArtifactId unversioned = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), (Version)null);
            ArchetypeGroup group = (ArchetypeGroup)archetypes.get(unversioned);

            if (group == null) {
                group = new ArchetypeGroup();
                group.mostRecent = artifact;
                archetypes.put(unversioned, group);
            }

            // Treat anything >= the archetype min version that has the same major and minor version as compatible
            Version archetypeMinVersion = getArchetypeMinVersion(artifact);
            VersionRange compatible = VersionRange.parse("[" + archetypeMinVersion + ","
                    + archetypeMinVersion.getMajor() + "." + (archetypeMinVersion.getMinor() + 1) + ")");

            // Split compatible from incompatible
            if (compatible.isInRange(coreVersion)) {
                group.compatible.add(id.getVersion());
            } else {
                group.incompatible.add(id.getVersion());
            }

            // Keep track of the most recent as the description is probably the best to use
            if (group.mostRecent.getId().getVersion().compareTo(artifact.getId().getVersion()) < 0) {
                group.mostRecent = artifact;
            }
        }

        return archetypes.values();
    }

    private Version getArchetypeMinVersion(RepoArtifact artifact) {
        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            if (dependency.getId().getGroup().equals("quokka.core.main")) {
                return dependency.getId().getVersion();
            }
        }

        return new Version(0, 0, 0, 0); // Make it incompatible with everything
    }

    private Version getCoreVersion() {
        Properties properites = new ArtifactPropertiesParser().parse("quokka.core.main", "main", "jar");
        String version = properites.getProperty("artifact.id.version");
        Assert.isTrue(version != null, "Could not find quokka.core.main version in the core class path");

        return Version.parse(version);
    }

    private void selectRepository() {
        RepositoryFactory factory = (RepositoryFactory)getProject().getReference(ProjectHelper.REPOSITORY_FACTORY);
        String repositoryUrl = getProject().getProperty("repository");

        if (repositoryUrl != null) {
            factory.getProperties().put("q.repo.archetype.url", repositoryUrl);
            repository = factory.getOrCreate("archetype", true);
        }

        String repositoryId = getProject().getProperty("repositoryId");

        if (repositoryId != null) {
            repository = factory.getOrCreate(repositoryId, true);
        }
    }

    private void copyEntries(File localCopy, String root) {
        File baseDir = getProject().getBaseDir();

        File toDelete = null;

        if (!localCopy.isDirectory()) {
            // ZipFileSets don't work with empty directories, or allow specifying a subdir as a root, so extract
            Expand unzip = (Expand)utils.init(new Expand(), "unjar");
            unzip.setDest(baseDir);
            unzip.setSrc(localCopy);
            unzip.perform();
            localCopy = baseDir;
            toDelete = new File(baseDir, "META-INF");
        }

        // Copy the archetype to the base directory, applying filters
        Copy copy = utils.createCopyTask();
        copy.setFiltering(true);
        copy.setTodir(baseDir);
        copy.setIncludeEmptyDirs(true);

        // Add project properties for filtering
        FilterSet filterSet = copy.createFilterSet();
        Map properties = PropertiesUtil.getProperties(getProject());

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();
            filterSet.addFilter(key, value);
        }

        // Add the artifact fileset (either .jar or exploded)
        FileSet fileSet = utils.createFileSet();
        fileSet.setDir(utils.normalise(localCopy + "/" + root + "archetype"));
        copy.addFileset(fileSet);
        copy.perform();

        if (toDelete != null) {
            utils.delete(toDelete);
        }
    }

    private boolean checkProperties(File localCopy, String root) {
        Document document = Document.parse(URLs.toURL(localCopy, root + "archetype.xml"),
                new Document.NullEntityResolver());
        boolean first = true;
        List properties = new ArrayList();

        for (Iterator i = document.getRoot().getChildren("property").iterator(); i.hasNext();) {
            Element propertyEl = (Element)i.next();
            String name = propertyEl.getAttribute("name");
            properties.add(name);

            boolean mandatory = "true".equals(propertyEl.getAttribute("mandatory"));
            String defaultValue = propertyEl.getAttribute("default");
            String prompt = propertyEl.getAttribute("prompt");
            Vector choices = new Vector();
            choices.addAll(Strings.commaSepList(propertyEl.getAttribute("choices")));

            if (getProject().getProperty(name) == null) {
                if (first) {
                    first = false;
                    logger.info(
                        "    Properties can be used to configure this archetype. Those ending in * are mandatory.");
                    logger.info("    Choices are shown in brackets, defaults in square brackets.");
                }

                prompt = "Enter " + name + (mandatory ? "*" : "") + ":" + ((prompt == null) ? "" : (" " + prompt));

                String value;

                do {
                    InputRequest request = (choices.size() == 0) ? new InputRequest(prompt)
                                                                 : new MultipleChoiceInputRequest(prompt, choices);
                    request.setDefaultValue(defaultValue);
                    getProject().getInputHandler().handleInput(request);
                    value = request.getInput().trim();
                    value = value.equals("") ? defaultValue : value;

                    if ((choices.size() != 0) && !choices.contains(value)) {
                        value = ""; // Not sure why, but choice validation not working properly in test, so double check here
                    }

                    if ((value != null) && !value.equals("")) {
                        getProject().setProperty(name, value);
                    }
                } while (!mandatory || (value == null) || value.equals(""));
            }
        }

        if (properties.size() != 0) {
            logger.info("The following properties have been set:");

            for (Iterator i = properties.iterator(); i.hasNext();) {
                String name = (String)i.next();
                String value = getProject().getProperty(name);

                if (value != null) {
                    logger.info("    " + name + " -> " + value);
                }
            }

            InputRequest request = new MultipleChoiceInputRequest("Do you want to continue?",
                    new Vector(Arrays.asList(new String[] { "y", "n" })));
            request.setDefaultValue("y");
            getProject().getInputHandler().handleInput(request);

            return request.getInput().equals("") || request.getInput().equals(request.getDefaultValue());
        }

        return true;
    }

    private void displayEntries(File localCopy, String rootURL) {
        // Build entries into a tree
        Map entries = URLs.toURLEntries(localCopy, rootURL + "archetype/");
        SortedSet set = new TreeSet(entries.keySet()); // sort so nodes are loaded in sequence

        Map nodes = new HashMap(); // stores currently loaded nodes for easy lookup of parent nodes
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null, true);
        nodes.put("", root);

        for (Iterator i = set.iterator(); i.hasNext();) {
            String entry = (String)i.next();
            String originalEntry = entry;
            boolean dir = entry.endsWith("/");
            entry = dir ? entry.substring(0, entry.length() - 1) : entry; // Strip trailing "/"

            String parent = entry.substring(0, entry.lastIndexOf("/") + 1);
            entry = entry.substring(entry.lastIndexOf("/") + 1);

            MutableTreeNode parentNode = (MutableTreeNode)nodes.get(parent);
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(entry, dir);
            nodes.put(originalEntry, child);
            parentNode.insert(child, 0);
        }

        // Display the tree
        display(root, "    ");
    }

    /*
     * Displays tree with padding, files first (sorted), followed by directories (sorted)
     */
    private void display(DefaultMutableTreeNode node, String padding) {
        Comparator comparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            };

        Set files = new TreeSet(comparator);
        Set dirs = new TreeSet(comparator);

        for (Enumeration e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();

            if (child.getAllowsChildren()) {
                dirs.add(child);
            } else {
                files.add(child);
            }
        }

        for (Iterator i = files.iterator(); i.hasNext();) {
            DefaultMutableTreeNode file = (DefaultMutableTreeNode)i.next();
            logger.info(padding + file.getUserObject());
        }

        for (Iterator i = dirs.iterator(); i.hasNext();) {
            DefaultMutableTreeNode dir = (DefaultMutableTreeNode)i.next();
            logger.info(padding + dir.getUserObject() + "/");
            display(dir, padding + "    ");
        }
    }

    private RepoArtifactId getArtifactId() {
        String id = getProject().getProperty("archetype");

        if (id == null) {
            return null;
        }

        String[] tokens = Strings.splitPreserveAllTokens(id, ":");
        Assert.isTrue((tokens.length >= 1) && (tokens.length <= 3),
            "archetype format is 'group[:name][:version]': " + id);

        String group = tokens[0];
        String name = (tokens.length >= 2) ? tokens[1] : RepoArtifactId.defaultName(group);
        String version = (tokens.length == 3) ? tokens[2] : null;

        return new RepoArtifactId(group, name, "archetype", (version == null) ? null : Version.parse(version));
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    private static class ArchetypeGroup {
        RepoArtifact mostRecent;
        SortedSet compatible = new TreeSet();
        SortedSet incompatible = new TreeSet();
    }
}
