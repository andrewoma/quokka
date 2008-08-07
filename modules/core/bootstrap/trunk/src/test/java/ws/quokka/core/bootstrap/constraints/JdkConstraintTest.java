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

import ws.quokka.core.bootstrap.resources.Jdk;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.version.VersionRangeUnion;

import java.io.File;

import java.util.Properties;


/**
 *
 */
public class JdkConstraintTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    Jdk jdk = new Jdk();
    JdkConstraint jdkConstraint = new JdkConstraint();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("java.vm.version", "1.2.0-b105");
        props.setProperty("java.vm.vendor", "VM Sun Microsystems Inc.");
        props.setProperty("java.specification.version", "1.6");
        props.setProperty("java.version", "1.9.0");
        props.setProperty("java.vendor", "Sun Microsystems Inc.");
        props.setProperty("sprop1", "value1");
        props.setProperty("sprop2", "value2");
        jdk.setProperties(props);

        jdk.setLocation(new File("C:\\SomeDir\\java.exe"));
        jdkConstraint.setJvmArgs("1024m");
    }

    public void testMatchJdkDefault() {
        assertTrue(jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkJavaVersion() {
        jdkConstraint.setJavaVersion(VersionRangeUnion.parse("1.6"));
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.setJavaVersion(VersionRangeUnion.parse("1.10"));
        assertTrue(!jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkJvmVersion() {
        jdkConstraint.setJvmVersion(VersionRangeUnion.parse("1.1"));
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.setJvmVersion(VersionRangeUnion.parse("1.4"));
        assertTrue(!jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkSpecVersion() {
        jdkConstraint.setSpecVersion(VersionRangeUnion.parse("1.5"));
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.setSpecVersion(VersionRangeUnion.parse("1.7"));
        assertTrue(!jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkVendor() {
        jdkConstraint.setJavaVendor("Sun Microsystems Inc.");
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.setJavaVendor("xxx");
        assertTrue(!jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkJvmVendor() {
        jdkConstraint.setJavaJvmVendor("VM Sun Microsystems Inc.");
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.setJavaJvmVendor("xxx");
        assertTrue(!jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkProperties() {
        jdkConstraint.addSystemPropery("sprop1", "value1", true);
        jdkConstraint.addSystemPropery("sprop2", "value2", true);

        // Non-matching, required
        assertTrue(jdkConstraint.matches(jdk, true));
        jdkConstraint.addSystemPropery("sprop1", "xxx", true);
        jdkConstraint.addSystemPropery("sprop2", "xxx", true);
        assertTrue(!jdkConstraint.matches(jdk, true));

        // Non-matching, optional
        jdkConstraint.addSystemPropery("sprop1", "xxx", false);
        jdkConstraint.addSystemPropery("sprop2", "xxx", false);
        assertTrue(jdkConstraint.matches(jdk, true));
    }

    public void testMatchJdkCombo() {
        jdkConstraint.setJavaVersion(VersionRangeUnion.parse("1.6"));
        jdkConstraint.setJvmVersion(VersionRangeUnion.parse("1.1"));
        jdkConstraint.setSpecVersion(VersionRangeUnion.parse("1.5"));
        jdkConstraint.setJavaVendor("Sun Microsystems Inc.");
        jdkConstraint.setJavaJvmVendor("VM Sun Microsystems Inc.");
        jdkConstraint.addSystemPropery("sprop1", "value1", true);
        jdkConstraint.addSystemPropery("sprop2", "value2", true);
        assertTrue(jdkConstraint.matches(jdk, true));

        // Fail one
        jdkConstraint.setSpecVersion(VersionRangeUnion.parse("1.7"));
        assertTrue(!jdkConstraint.matches(jdk, true));
    }
}
