package ws.quokka.core.repo_resolver;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

/**
 *
 */
public class ResolvedPath {
    private String id;
    private List artifacts = new ArrayList();

    public ResolvedPath() {
    }

    public ResolvedPath(List artifacts) {
        this.artifacts = artifacts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void add(RepoArtifact artifact) {
        artifacts.add(artifact);
    }

    public List getArtifacts() {
        return artifacts;
    }

    public boolean contains(RepoArtifactId id) {
        for (Iterator i = artifacts.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact) i.next();
            if (artifact.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
