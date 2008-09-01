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

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * UnresolvedArtifactException is thrown when a repository is unable to resolve an artifact
 */
public class UnresolvedArtifactException extends RuntimeException {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId artifactId;
    private List urls = new ArrayList();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public UnresolvedArtifactException(RepoArtifactId artifactId, URL url) {
        this(artifactId, new ArrayList(Collections.singleton(url)), null);
    }

    public UnresolvedArtifactException(RepoArtifactId artifactId, URL url, String message) {
        this(artifactId, new ArrayList(Collections.singleton(url)), message);
    }

    public UnresolvedArtifactException(RepoArtifactId artifactId) {
        this(artifactId, Collections.EMPTY_LIST, null);
    }

    public UnresolvedArtifactException(RepoArtifactId artifactId, List urls) {
        this(artifactId, urls, null);
    }

    public UnresolvedArtifactException(RepoArtifactId artifactId, List urls, String message) {
        super("Unable to resolve artifact '" + artifactId.toShortString() + "'"
            + ((message == null) ? "" : (": " + message + toString(urls))));
        this.artifactId = artifactId;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    private static String toString(List urls) {
        if (urls.size() == 0) {
            return "";
        }

        StringBuffer sb = new StringBuffer(". URLs attempted:\n");

        for (Iterator i = urls.iterator(); i.hasNext();) {
            URL url = (URL)i.next();
            sb.append("\t").append(url.toString());
        }

        return sb.toString();
    }

    public RepoArtifactId getArtifactId() {
        return artifactId;
    }

    public List getUrls() {
        return urls;
    }
}
