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


package ws.quokka.core.itest;

import org.apache.tools.ant.Project;

import ws.quokka.core.bootstrap_util.IOUtils;

import java.io.File;


/**
 *
 */
public class ArchetypeITest extends IntegrationTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testArchetype() {
//        setLogLevel(Project.MSG_VERBOSE);
        File temp = new IOUtils().createTempFile("quokka-archetype", ".xml");

        try {
            properties.put("quokka.project.specialTarget", "archetype");

//            properties.put("archetype", "quokka.archetype.jar");
            ant(temp, new String[] { "archetype" });
        } finally {
            temp.delete();
        }
    }
}
