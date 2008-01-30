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

import ws.quokka.core.util.AnnotatedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class PathGroup extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String id;
    private List paths = new ArrayList();
    private Boolean mergeWithCore = Boolean.TRUE;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    private PathGroup() {
    }

    public PathGroup(String id, List paths, Boolean mergeWithCore) {
        this.id = id;
        this.paths = paths;
        this.mergeWithCore = mergeWithCore;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public List getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public Boolean getMergeWithCore() {
        return mergeWithCore;
    }
}
