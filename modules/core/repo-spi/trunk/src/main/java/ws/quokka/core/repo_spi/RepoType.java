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

import ws.quokka.core.util.AnnotatedObject;


/**
 *
 */
public class RepoType extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String id;
    private String description;
    private String extension;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoType(String id, String description, String extension) {
        this.id = id;
        this.description = description;
        this.extension = extension;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }

    public String toShortString() {
        return id;
    }
}
