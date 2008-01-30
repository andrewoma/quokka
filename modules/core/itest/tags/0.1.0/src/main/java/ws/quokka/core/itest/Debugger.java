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

import java.io.File;

import java.util.Properties;


/**
 *
 */
public class Debugger {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        DebuggerTest debugger = new DebuggerTest();
        debugger.setUp();
        debugger.setLogLevel(Project.MSG_INFO);

//        debugger.properties.put("profiles", "skiptest");
//        debugger.debug("plugin\\xml", "install");
//        debugger.debug("core\\itest", "install");
//        debugger.debug("core\\repo-spi", "install");
//        debugger.debug("plugin\\jalopy", "install");
//        debugger.debug("core\\main", "install");
//
//        debugger.debug("core\\main", "install");
//        debugger.debug("plugin\\junit", "install");
//        debugger.debug("core\\bundle", "docbook-pdf");
//        debugger.debug("core\\util", "install");
//        debugger.debug("core\\bundle", "install");
//        debugger.debug("internal\\site", "clean");
//        debugger.debug("internal\\site", "site");
//        debugger.debug("core\\bundle", "install-quokka");
//        debugger.debug("internal\\master", "install-all");
        debugger.debug("internal\\master", "update-release-repo");
        debugger.debug("core\\bundle", "install-quokka");

//
//        debugger.debug("internal\\master", "install-quokka");
//        debugger.debug("xmlcat\\docbook-dtd-4-5", "install");
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class DebuggerTest extends IntegrationTest {
        Properties properties = new Properties();

        public void debug(String module, String target) {
            ant(module, target);
        }

        public void debug(String module, String target1, String target2) {
            ant(module, target1, target2);
        }

        public void ant(String module, String target1) {
            ant(module, new String[] { target1 });
        }

        public void ant(String module, String[] targets) {
            ant(new File("C:\\Data\\Dev\\Projects\\quokka-google\\all\\trunks\\" + module + "\\build.xml"), targets);
        }

        public void ant(String module, String target1, String target2) {
            ant(module, new String[] { target1, target2 });
        }

        public void addProperties(Properties properties) {
            properties.putAll(this.properties);
        }

        public void setUp() throws Exception {
            super.setUp();
        }
    }
}
