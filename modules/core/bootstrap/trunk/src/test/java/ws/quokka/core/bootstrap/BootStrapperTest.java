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


package ws.quokka.core.bootstrap;

import org.apache.tools.ant.Project;

import ws.quokka.core.bootstrap.constraints.JdkConstraint;
import ws.quokka.core.bootstrap.resources.Jdk;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.test.AbstractTest;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 *
 */
public class BootStrapperTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private BootStrapper bootStrapper = new BootStrapper();
    private Jdk jdk = new Jdk();
    private JdkConstraint jdkConstraint = new JdkConstraint();
    private String root;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        Log.set(new ProjectLogger(new Project()));
        root = File.separatorChar == '\\' ? "C:\\" : "/";

        Properties properties = new Properties();
        properties.setProperty("prop1", "value1");
        properties.setProperty("prop2", "value2 space");
        properties.setProperty("prop2", "value2\" quote");
        bootStrapper.setProperties(properties);

        System.setProperty("ant.home", normalise(root + "ant home"));

        List targets = new ArrayList();
        targets.add("clean");
        targets.add("install");

        bootStrapper.setArguments(targets);

        Properties jdkProperties = new Properties();
        jdkProperties.setProperty("java.vm.version", "1.2.0-b105");
        jdkProperties.setProperty("java.vm.vendor", "VM Sun Microsystems Inc.");
        jdkProperties.setProperty("java.specification.version", "1.6");
        jdkProperties.setProperty("java.version", "1.9.0");
        jdkProperties.setProperty("java.vendor", "Sun Microsystems Inc.");
        jdkProperties.setProperty("sprop1", "value1");
        jdkProperties.setProperty("sprop2", "value2");
        jdk.setProperties(jdkProperties);

        jdk.setLocation(new File(normalise(root + "javahome/java.exe")));
        jdkConstraint.setMaxMemory("1024m");
    }

    public void testCreateCommandLine() {
        jdk.setMatchedConstraint(jdkConstraint);

        List path = new ArrayList();
        path.add(new File(normalise(root + "libs/jar1.jar")));
        path.add(new File(normalise(root + "libs/jar1.jar")));

        String command = bootStrapper.createCommandLine(jdk, path);
        if (File.separatorChar == '\\') {
            assertEquals("C:\\javahome\\java.exe -Dorg.apache.tools.ant.ProjectHelper=ws.quokka.core.main.ant.ProjectHelper \"-Dant.home=C:\\ant home\" \"-Dant.library.dir=C:\\ant home\\antlib\" -Dquokka.bootstrap.maxMemory=1024m -Xmx1024m org.apache.tools.ant.launch.Launcher -main ws.quokka.core.main.ant.QuokkaMain '-Dprop2=value2\" quote' -Dprop1=value1 clean install",
                    command);

        } else {
            assertEquals("/javahome/java.exe -Dorg.apache.tools.ant.ProjectHelper=ws.quokka.core.main.ant.ProjectHelper \"-Dant.home=/ant home\" \"-Dant.library.dir=/ant home/antlib\" -Dquokka.bootstrap.maxMemory=1024m -Xmx1024m org.apache.tools.ant.launch.Launcher -main ws.quokka.core.main.ant.QuokkaMain '-Dprop2=value2\" quote' -Dprop1=value1 clean install",
                    command);
        }
    }
}
