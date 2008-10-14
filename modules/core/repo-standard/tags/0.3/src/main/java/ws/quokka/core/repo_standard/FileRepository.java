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


package ws.quokka.core.repo_standard;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.Strings;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 *
 */
public class FileRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File rootDir;
    private File installRootDir;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        super.initialise();
        rootDir = normalise(new File(getProperty("root", true)));

        String installRootDirName = getProperty("installRoot", null);
        installRootDir = (installRootDirName == null) ? null : normalise(new File(installRootDirName));
    }

    public RepoArtifact resolve(RepoArtifactId id, boolean retrieveArtifact) {
        File artifactFile = getArtifactFile(id);
        debug("Attempting to resolve: " + artifactFile.getAbsolutePath());

        File repositoryFile = getRepositoryFile(id);
        debug("Looking for repository file: " + repositoryFile);

        if (artifactFile.exists() || repositoryFile.exists()) {
            RepoArtifact artifact = repositoryFile.exists() ? parse(id, repositoryFile) : new RepoArtifact(id);
            Assert.isTrue(id.getType().equals("paths") || artifact.isStub() || artifactFile.exists(),
                "Repository is corrupt. repository.xml exists, but artifact is missing: " + artifactFile.getPath());

            if (artifactFile.exists()) {
                artifact.setLocalCopy(artifactFile);
            }

            return artifact;
        }

        // Artifact doesn't exist, so check parents
        ResolvedArtifact resolved = resolveFromParents(id, true, isConfirmImport(), retrieveArtifact);

        if (retrieveArtifact) {
            importArtifact(resolved.getArtifact(), artifactFile, repositoryFile, resolved.getRepository());
        }

        return resolved.getArtifact();
    }

    public File getArtifactFile(RepoArtifactId id) {
        RepoType type = getFactory().getType(id.getType());
        String extension = type.getId() + "." + type.getExtension();

        return normalise(new File(rootDir, getRelativePath(id, extension)));
    }

    public File getRepositoryFile(RepoArtifactId id) {
        RepoType type = getFactory().getType(id.getType());

        return normalise(new File(rootDir, getRelativePath(id, type.getId() + "_repository.xml")));
    }

    public void install(RepoArtifact artifact) {
        File oldRootDir = rootDir;

        try {
            rootDir = (installRootDir == null) ? rootDir : installRootDir;

            File artifactFile = getArtifactFile(artifact.getId());
            log().info("Installing '" + artifact.getId().toShortString() + "' into repository '" + getName() + "' at "
                + artifactFile.getParent());
            copyArtifact(artifact, artifactFile);
            writeRepositoryFile(artifact, getRepositoryFile(artifact.getId()));
        } finally {
            rootDir = oldRootDir;
        }
    }

    public void remove(RepoArtifactId artifactId) {
        File artifactFile = getArtifactFile(artifactId);

        if (artifactFile.exists()) {
            Assert.isTrue(artifactFile.delete(), "Unable to delete artifact file: " + artifactFile.getPath());
        }

        File repositoryFile = getRepositoryFile(artifactId);

        if (repositoryFile.exists()) {
            Assert.isTrue(repositoryFile.delete(), "Unable to delete repository file: " + repositoryFile.getPath());
        }
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        Set ids = isHierarchical() ? listArtifactIdsHierarchical() : listArtifactIdsFlat();

        if (includeReferenced) {
            ids.addAll(listParentIds());
        }

        return filterUnresolvable(ids);
    }

    private Set listArtifactIdsHierarchical() {
        try {
            Set ids = new TreeSet();
            List files = new ArrayList();
            listFiles(files, rootDir);

            String canonicalRootDir = rootDir.getCanonicalPath();

            for (Iterator i = files.iterator(); i.hasNext();) {
                File file = (File)i.next();
                String canonicalFile = file.getCanonicalPath();
                String relative = canonicalFile.substring(canonicalRootDir.length() + 1);

                String name = stripName(file.getName());
                String[] tokens = (name == null) ? null : Strings.splitPreserveAllTokens(name, "_");

                if (!relative.startsWith("_") && (name != null) && ((tokens.length == 2) || (tokens.length == 3))) {
                    String version = file.getParentFile().getName();
                    String group = deriveGroup(relative);

                    if (group == null) {
                        log().verbose("Skipping file as it is not a valid artifact: " + file.getPath());
                    } else {
                        String type = tokens[1];
                        autoRegisterType(type, getExtension(file.getName()));
                        ids.add(new RepoArtifactId(group, tokens[0], type, version));
                    }
                } else {
                    log().verbose("Skipping file as it is not a valid artifact: " + file.getPath());
                }
            }

            return ids;
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Automatically registers unkown types discovered when listing
     */
    private void autoRegisterType(String type, String extension) {
        if (extension == null) {
            return;
        }

        try {
            getFactory().getType(type);
        } catch (Exception e) {
            getFactory().registerType(new RepoType(type, "Automatically registered type", extension));
        }
    }

    private String stripName(String name) {
        int index = getExtensionStart(name);

        return (index == -1) ? null : name.substring(0, index);
    }

    private int getExtensionStart(String name) {
        int index = name.lastIndexOf("_");

        if (index == -1) {
            return -1; // Not a valid id
        }

        return name.indexOf(".", index);
    }

    private String getExtension(String name) {
        if (name.endsWith("_repository.xml")) {
            return null;
        }

        int index = getExtensionStart(name);

        return Strings.split(name.substring(index + 1), ".")[0]; // Ignore secondary extensions such as .MD5
    }

    private String deriveGroup(String relative) {
        StringBuffer group = new StringBuffer();
        String[] tokens = Strings.split(relative, File.separator);

        if (tokens.length < 3) {
            return null;
        }

        for (int i = 0; i < (tokens.length - 2); i++) {
            String token = tokens[i];
            group.append(token);

            if (i != (tokens.length - 3)) {
                group.append(".");
            }
        }

        return group.toString();
    }

    private void listFiles(List files, File dir) {
        File[] files1 = dir.listFiles();

        if (files1 == null) {
            return; // Repository dir doesn't exist yet or is not readable
        }

        for (int i = 0; i < files1.length; i++) {
            File file = files1[i];

            if (file.isDirectory()) {
                // TODO: Use ANT to list files to pick up default excludes ...
                if (!file.getName().equals(".svn")) { // Ignore subversion pollution
                    listFiles(files, file);
                }
            } else {
                files.add(file);
            }
        }
    }

    private Set listArtifactIdsFlat() {
        Set ids = new TreeSet();
        File[] files = rootDir.listFiles();

        if (files == null) {
            return ids;
        }

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String name = stripName(file.getName());
            String[] tokens = (name == null) ? null : Strings.splitPreserveAllTokens(name, "_");

            if ((name != null) && !name.startsWith("_") && ((tokens.length == 4) || (tokens.length == 5))) {
                String type = tokens[3];
                autoRegisterType(type, getExtension(file.getName()));

                RepoArtifactId id = new RepoArtifactId(tokens[0], tokens[2], type, tokens[1]);
                ids.add(id);
            } else {
                log().verbose("Skipping file as it is not a valid artifact: " + file.getPath());
            }
        }

        return ids;
    }

    protected File getRootDir() {
        return rootDir;
    }

    protected File getInstallRootDir() {
        return installRootDir;
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        RepoArtifact latest = null;

        try {
            // Check this repository
            RepoArtifact updated = resolve(artifact.getId()); // Cheap operation ...

            if (updated.isNewerThan(artifact)) {
                latest = updated;
            }
        } catch (UnresolvedArtifactException e) {
            // Ignore ... just means the artifact is not in this repository
        }

        // Check parents
        for (Iterator i = getParents().iterator(); i.hasNext();) {
            Repository parent = (Repository)i.next();
            RepoArtifact current = (latest == null) ? artifact : latest;
            RepoArtifact updated = parent.updateSnapshot(current);

            if ((updated != null) && updated.isNewerThan(current)) {
                latest = updated;
            }
        }

        return latest;
    }

    public void rebuildCaches() {
    }
}
