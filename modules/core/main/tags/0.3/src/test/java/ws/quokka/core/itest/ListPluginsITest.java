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


/**
 *
 */
public class ListPluginsITest extends AbstractMainIntegrationTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testArchetype() {
        setLogLevel(Project.MSG_INFO);

//        properties.put("all", "true");
//        properties.put("verbose", "true");
        properties.put("q.project.overrideCore", "true");
        properties.put("plugin", "quokka.plugin.help");
        ant(new String[] { "list-plugins" });
    }
}
