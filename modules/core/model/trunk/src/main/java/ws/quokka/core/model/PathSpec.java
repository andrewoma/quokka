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

import ws.quokka.core.repo_spi.RepoPathSpec;


/**
 *
 */
public class PathSpec extends RepoPathSpec {
    //~ Constructors ---------------------------------------------------------------------------------------------------

    public PathSpec() {
    }

    public PathSpec(String shorthand, boolean toRequired) {
        super(shorthand, toRequired);
    }

    public PathSpec(String shorthand) {
        super(shorthand);
    }

    public PathSpec(String fromPath, String toPath, String options, Boolean descend, Boolean mandatory) {
        super(fromPath, toPath, options, descend, mandatory);
    }
}
