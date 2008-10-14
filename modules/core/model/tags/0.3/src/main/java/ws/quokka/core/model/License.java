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


package ws.quokka.core.model;

import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.util.AnnotatedObject;

import java.io.File;


/**
 *
 */
public class License extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File file;
    private RepoArtifactId id;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public License(File file, RepoArtifactId id) {
        this.file = file;
        this.id = id;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public File getFile() {
        return file;
    }

    public RepoArtifactId getId() {
        return id;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        License license = (License)o;

        if ((file != null) ? (!file.equals(license.file)) : (license.file != null)) {
            return false;
        }

        if ((id != null) ? (!id.equals(license.id)) : (license.id != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((file != null) ? file.hashCode() : 0);
        result = (31 * result) + ((id != null) ? id.hashCode() : 0);

        return result;
    }
}
