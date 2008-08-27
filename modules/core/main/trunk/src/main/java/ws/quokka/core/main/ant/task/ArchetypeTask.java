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
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.PropertiesUtil;
import ws.quokka.core.bootstrap_util.TaskLogger;
import ws.quokka.core.main.ant.ProjectHelper;
import ws.quokka.core.plugin_spi.support.AntUtils;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryAware;
import ws.quokka.core.repo_spi.RepositoryFactory;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.URLs;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
        logger = new TaskLogger(this);
        utils = new AntUtils(getProject());
    }

    public void execute() throws BuildException {
        RepoArtifactId id = getArtifactId();
        logger.info("Creating project based on artifact: " + id.toShortString());

        selectRepository();

        RepoArtifact artifact = repository.resolve(id);
        logger.info("Extracting the following archetype to " + getProject().getBaseDir().getAbsolutePath() + ":");

        String root = "META-INF/quokka/" + artifact.getId().toPathString() + "/";

        File localCopy = artifact.getLocalCopy();
        loadDefaults(localCopy, root);
        checkProperties(localCopy, root);

        displayEntries(localCopy, root);
        copyEntries(localCopy, root);
    }

    private void selectRepository() {
        RepositoryFactory factory = (RepositoryFactory)getProject().getReference(ProjectHelper.REPOSITORY_FACTORY);
        String repositoryUrl = getProject().getProperty("repository");

        if (repositoryUrl != null) {
            factory.getProperties().put("quokka.repo.archetype.url", repositoryUrl);
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
            utils.deleteFile(toDelete);
        }
    }

    private void checkProperties(File localCopy, String root) {
        Properties archetypeProperties = ioUtils.loadProperties(URLs.toURL(localCopy, root + "archetype.properties"));
        List mandatory = new ArrayList(Strings.commaSepList(archetypeProperties.getProperty("mandatory")));

        for (Iterator i = mandatory.iterator(); i.hasNext();) {
            String key = (String)i.next();

            if (getProject().getProperty(key) != null) {
                i.remove();
            }
        }

        Assert.isTrue(mandatory.size() == 0, "The following properties must be set for this archetype: " + mandatory);
    }

    private void loadDefaults(File localCopy, String root) {
        URL url = URLs.toURL(localCopy, root + "default.properties");

        if (url != null) {
            Properties defaults = ioUtils.loadProperties(url);

            for (Iterator i = defaults.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();

                if (getProject().getProperty((String)entry.getKey()) == null) {
                    getProject().setProperty((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }

        // Add default name as a special case of the last segment of group if it was supplied
        if (getProject().getProperty("name") == null) {
            String group = getProject().getProperty("group");

            if (group != null) {
                getProject().setProperty("name", group.substring(group.lastIndexOf(".") + 1));
            }
        }
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
        Assert.isTrue(id != null, "'archetype' property is required");

        String[] tokens = Strings.split(id, ":");
        Assert.isTrue((tokens.length == 2) || (tokens.length == 3), "archetype format is 'group:[name]:version': " + id);

        String name = (tokens.length == 3) ? tokens[1] : tokens[0].substring(tokens[0].lastIndexOf('.') + 1);
        String version = tokens[tokens.length - 1];

        return new RepoArtifactId(tokens[0], name, "jar", version);
    }
}
