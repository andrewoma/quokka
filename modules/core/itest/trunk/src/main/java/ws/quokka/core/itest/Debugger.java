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
        //        debugger.debug("archetype\\jar", "install");
        //        debugger.debug("archetype\\depset", "install");
        //        debugger.debug("core\\main", "install");
        //        debugger.debug("core\\repo-spi", "install");
        //        debugger.debug("core\\bootstrap-util", "install");
        //        debugger.debug("plugin\\junitreport", "install");
        //                debugger.debug("plugin\\jalopy", "install");
        //        debugger.debug("plugin\\subversion", "install");
        //        debugger.debug("plugin\\help", "install");
        //        debugger.debug("plugin\\cobertura", "install");
        //        debugger.debug("plugin\\standard-lifecycle", "install");
        //        debugger.debug("plugin\\devreport", "install");
        //        debugger.debug("plugin\\jarbundle", "install");
        //        debugger.debug("archetype\\multiproject", "clean");
        //        debugger.debug("archetype\\multiproject", "install");
        //        debugger.debug("plugin\\release", "install");
        //        debugger.debug("plugin\\lifecycle", "install");
        //        debugger.debug("internal\\master", "clean");
        //        debugger.debug("internal\\master", "install-all");
        //        debugger.debug("internal\\module-depset", "install");
        //        debugger.debug("internal\\master", "reports-all");
        //        debugger.properties.put("profiles", "skiptest");
        //        debugger.debug("archetype\\jar", "install");
        //        debugger.debug("archetype\\depset", "install");
        //        debugger.debug("core\\main", "install");
        //        debugger.debug("core\\repo-spi", "install");
        //        debugger.debug("core\\bootstrap-util", "install");
        //        debugger.debug("plugin\\junitreport", "install");
        //        debugger.debug("plugin\\junit", "install");
        //        debugger.debug("plugin\\subversion", "install");
        //        debugger.debug("plugin\\help", "install");
        //        debugger.debug("plugin\\cobertura", "install");
        //        debugger.debug("plugin\\standard-lifecycle", "install");
        //        debugger.debug("plugin\\devreport", "install");
        //        debugger.debug("plugin\\jarbundle", "install");
        //        debugger.debug("archetype\\multiproject", "clean");
        //        debugger.debug("archetype\\multiproject", "install");
        //        debugger.debug("plugin\\release", "install");
        //        debugger.debug("plugin\\lifecycle", "install");
        //        debugger.debug("internal\\master", "clean");
        //        debugger.debug("internal\\master", "install-all");
        //        debugger.debug("internal\\module-depset", "install");
        //        debugger.debug("internal\\master", "reports-all");
        //        debugger.debug("internal\\master", "install-all");
        debugger.debug("internal\\docbook-depset", "install");
        debugger.debug("core\\bundle", "docbook-pdf");

        //        debugger.debug("internal\\docbook-depset", "clean", "install");
        //        debugger.debug("xmlcat\\site-naut05-custom", "clean", "install");
        //        debugger.debug("internal\\site", "clean");
        //        debugger.debug("internal\\site", "site");
        //        debugger.debug("internal\\site", "help");
        //        debugger.debug("internal\\site", "testunzip");
        //        debugger.debug("internal\\site", "deploy-site");
        //        debugger.debug("internal\\site", "deploy-reports");
        //        debugger.debug("internal\\site", "deploy-all");
        //        debugger.debug("core\\bundle", "clean", "dist");
        //        debugger.debug("core\\bundle", "clean", "docbook");
        //        debugger.debug("core\\bundle", "docbook-pdf");
        //        debugger.debug("core\\bundle", "docbook-pdf");
        //        debugger.debug("core\\repo-spi", "install");
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
            ant(new File("C:\\Data\\Dev\\Projects\\quokka\\" + module + "\\build.xml"), targets);
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
