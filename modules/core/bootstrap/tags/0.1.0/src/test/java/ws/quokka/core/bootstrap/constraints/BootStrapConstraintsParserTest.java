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

import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class BootStrapConstraintsParserTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map sysproperties = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void addSysProperty(String key, String value, boolean required) {
        sysproperties.put(key, new JdkConstraint.SystemPropertyValue(required, value));
    }

    public void testParsingShorthand() {
        BootStrapContraintsParser parser = new BootStrapContraintsParser(getTestCaseResource(
                    "bootstrap-quokka-shorthand.xml"), Collections.EMPTY_SET);
        BootStrapConstraints bootStrap = parser.parse();
        List cores = bootStrap.getCoreConstraints();
        assertEquals(1, cores.size());
        assertCore("1.1", (CoreConstraint)cores.get(0));

        List jdks = bootStrap.getJdkConstraints();
        assertEquals(1, jdks.size());
        assertJdk(null, null, null, null, "[1.4,1.6]", null, new HashMap(), (JdkConstraint)jdks.get(0));
    }

    public void testParsingWithDefaults() {
        BootStrapContraintsParser parser = new BootStrapContraintsParser(getTestCaseResource(
                    "bootstrap-quokka-defaults.xml"), Collections.EMPTY_SET);
        BootStrapConstraints bootStrap = parser.parse();

        // Check the cores
        List cores = bootStrap.getCoreConstraints();
        assertEquals(4, cores.size());
        assertCore("pre", (CoreConstraint)cores.get(0));
        assertCore("overriddenVersion", (CoreConstraint)cores.get(1));
        assertCore("defaultVersion", (CoreConstraint)cores.get(2));
        assertCore("post", (CoreConstraint)cores.get(3));

        // Check the jdks
        List jdks = bootStrap.getJdkConstraints();
        assertEquals(5, jdks.size());

        assertJdk("prejdk", null, null, null, null, null, new HashMap(), (JdkConstraint)jdks.get(0));

        addSysProperty("default-sysproperty1", "default-sysproperty1-value1", true);
        addSysProperty("overridden-sysproperty1", "overridden-sysproperty1-value1", true);
        assertJdk("overridden-java-version1", "overridden-java-vendor1", "overridden-jvm-version1",
            "overridden-jvm-vendor1", "overridden-spec-version1", "overridden-max-memory1", sysproperties,
            (JdkConstraint)jdks.get(1));

        sysproperties.remove("overridden-sysproperty1");
        assertJdk("default-java-version1", "default-java-vendor1", "default-jvm-version1", "default-jvm-vendor1",
            "default-spec-version1", "default-max-memory1", sysproperties, (JdkConstraint)jdks.get(2));

        assertJdk("postjdk", null, null, null, null, null, new HashMap(), (JdkConstraint)jdks.get(3));

        sysproperties.clear();
        addSysProperty("name1", "value1", true);
        addSysProperty("opt1", "value2", false);
        addSysProperty("def1", "value3", true);
        assertJdk("java-version1", "java-vendor1", "jvm-version1", "jvm-vendor1", "spec-version1", "max-memory1",
            sysproperties, (JdkConstraint)jdks.get(4));

        // Check the dependencies
        List dependencies = bootStrap.getDependencyContraints();
        assertEquals(2, dependencies.size());
        assertDependency("group1", "name1", "version1", (DependencyConstraint)dependencies.get(0));
        assertDependency("group2", "name2", "version2", (DependencyConstraint)dependencies.get(1));
    }

    public void testParsingWithProfiles() {
        Set profiles = new HashSet();
        profiles.add("p1");

        BootStrapContraintsParser parser = new BootStrapContraintsParser(getTestCaseResource(
                    "bootstrap-quokka-profiles.xml"), profiles);
        BootStrapConstraints bootStrap = parser.parse();

        // Check the cores
        List cores = bootStrap.getCoreConstraints();
        assertEquals(4, cores.size());
        assertCore("cores-default", (CoreConstraint)cores.get(0));
        assertCore("cores-p1", (CoreConstraint)cores.get(1));
        assertCore("core-p1", (CoreConstraint)cores.get(2));
        assertCore("core-default", (CoreConstraint)cores.get(3));

        // Check the jdks
        List jdks = bootStrap.getJdkConstraints();
        assertEquals(4, jdks.size());
        addSysProperty("sysprop-default", "sysprop-default", true);
        addSysProperty("sysprop-p1", "sysprop-p1", true);
        assertJdk("jdk-p1", null, null, null, null, null, sysproperties, (JdkConstraint)jdks.get(0));
        sysproperties.clear();
        assertJdk("jdk-default", null, null, null, null, null, sysproperties, (JdkConstraint)jdks.get(1));
        assertJdk("jdk-p1-1", null, null, null, null, null, sysproperties, (JdkConstraint)jdks.get(2));
        assertJdk("jdk-default-1", null, null, null, null, null, sysproperties, (JdkConstraint)jdks.get(3));

        // Check the dependencies
        List dependencies = bootStrap.getDependencyContraints();
        assertEquals(2, dependencies.size());

        // This also checks the default values if none are supplied in attributes
        assertDependency("group-default", "group-default", "1", (DependencyConstraint)dependencies.get(0));
        assertDependency("group-p1", "group-p1", "1", (DependencyConstraint)dependencies.get(1));
    }

    private void assertDependency(String group, String name, String version, DependencyConstraint dependency) {
        assertEquals(group, dependency.getGroup());
        assertEquals(name, dependency.getName());
        assertVersion(version, dependency.getVersion());
    }

    private void assertJdk(String javaVersion, String javaVendor, String jvmVersion, String jvmVendor,
        String specVersion, String maxHeap, Map systemProperties, JdkConstraint jdkConstraint) {
        assertVersion(javaVersion, jdkConstraint.getJavaVersion());
        assertEquals(javaVendor, jdkConstraint.getJavaVendor());
        assertVersion(jvmVersion, jdkConstraint.getJvmVersion());
        assertEquals(jvmVendor, jdkConstraint.getJavaJvmVendor());
        assertVersion(specVersion, jdkConstraint.getSpecVersion());
        assertEquals(maxHeap, jdkConstraint.getMaxMemory());
        assertEquals(systemProperties, jdkConstraint.getSystemProperties());
    }

    private void assertCore(String version, CoreConstraint core) {
        assertEquals(version, (core.getVersion() == null) ? null : core.getVersion().toString());
    }

    private void assertVersion(String version, VersionRangeUnion rangeUnion) {
        version = (version == null) ? null : VersionRangeUnion.parse(version).toString();
        assertEquals(version, (rangeUnion == null) ? null : rangeUnion.toString());
    }
}
