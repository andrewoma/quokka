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


package ws.quokka.core.bootstrap.constraints;

import ws.quokka.core.bootstrap.resources.BootStrapResources;
import ws.quokka.core.bootstrap.resources.DependencyResource;
import ws.quokka.core.bootstrap.resources.Jdk;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.io.File;

import java.util.List;


/**
 *
 */
public class BootStrapConstraintsTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private BootStrapConstraints constraints = new BootStrapConstraints();
    private BootStrapResources resources = new BootStrapResources();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        JdkConstraint constraint = new JdkConstraint();
        constraint.addSystemPropery("constraint", "constraint1", true);
        constraints.getJdkConstraints().add(constraint);

        constraint = new JdkConstraint();
        constraint.addSystemPropery("constraint", "constraint2", true);
        constraints.getJdkConstraints().add(constraint);

        Jdk jdk = new Jdk();
        jdk.getProperties().setProperty("constraint", "constraint2");
        jdk.setLocation(new File("location1"));
        resources.getJdks().add(jdk);

        jdk = new Jdk();
        jdk.getProperties().setProperty("constraint", "constraint3");
        jdk.setLocation(new File("location2"));
        resources.getJdks().add(jdk);

        CoreConstraint coreConstraint = new CoreConstraint();
        coreConstraint.setVersion(VersionRangeUnion.parse("[1.1,1.1]"));
        constraints.getCoreConstraints().add(coreConstraint);

        coreConstraint = new CoreConstraint();
        coreConstraint.setVersion(VersionRangeUnion.parse("[1.2,1.2]"));
        constraints.getCoreConstraints().add(coreConstraint);

        constraints.getDependencyContraints().add(new DependencyConstraint("group", "name",
                VersionRangeUnion.parse("[1.2,1.3]")));
        constraints.getDependencyContraints().add(new DependencyConstraint("quokka.bundle", "core",
                VersionRangeUnion.parse("[1.2,1.2]")));

        addLibrary("quokka.bundle", "core", "1.3");
        addLibrary("quokka.bundle", "core", "1.2");
        addLibrary("quokka.bundle", "core", "1.1");
        addLibrary("quokka.bundle", "core", "1.0");

        addLibrary("group", "name", "1.4");
        addLibrary("group", "name", "1.3");
        addLibrary("group", "name", "1.2");
        addLibrary("group", "name", "1.1");
    }

    private void addLibrary(String group, String name, String version) {
        resources.getAvailableLibraries().add(new DependencyResource(group, name, Version.parse(version)));
    }

    public void testfindMatchingJdk() {
        Jdk jdk = constraints.findMatchingJdk(resources);
        assertEquals("constraint2", jdk.getProperties().getProperty("constraint"));
    }

    public void testfindMatchingCore() {
        DependencyResource core = constraints.findMatchingCore(resources);
        assertEquals(Version.parse("1.1"), core.getVersion());
    }

    public void testfindMatchingDependencies() {
        List dependencies = constraints.findMatchingDependencies(resources);
        assertEquals(2, dependencies.size());

        DependencyResource resource1 = (DependencyResource)dependencies.get(0);
        DependencyResource resource2 = (DependencyResource)dependencies.get(1);
        assertEquals(Version.parse("1.3"), resource1.getVersion());
        assertEquals(Version.parse("1.2"), resource2.getVersion());
    }
}
