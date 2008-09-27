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

import org.apache.tools.ant.Project;

import org.w3c.dom.Element;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ProfilesMatcher;
import ws.quokka.core.bootstrap_util.XmlParser;
import ws.quokka.core.version.VersionRangeUnion;

import java.io.File;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;


/**
 *
 */
public class BootStrapContraintsParser extends XmlParser {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Set activeProfiles;
    private File file;
    private ProfilesMatcher profilesMatcher = new ProfilesMatcher();
    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public BootStrapContraintsParser(Project project, File file, Set activeProfiles) {
        this.project = project;
        this.activeProfiles = activeProfiles;
        this.file = file;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public BootStrapConstraints parse() {
        Element projectEl = parseXml(file);
        List bootStrapEls = applyProfiles(getChildren(projectEl, "bootstrap", false));

        if (bootStrapEls.size() == 0) {
            return null;
        }

        Assert.isTrue(bootStrapEls.size() == 1, "There can only be one active bootstrap element at a time");

        Element bootStrapEl = (Element)bootStrapEls.iterator().next();

        // Check if the bootstrapping is defined in a separate file
        String fileAttribute = getAttribute(bootStrapEl, "file");

        if (fileAttribute != null) {
            String message = "If the 'file' attribute is specified, it must be the only attribute on 'bootstrap' "
                + " and 'bootstrap' can have no children";
            Assert.isTrue(bootStrapEl.getAttributes().getLength() == 1, message);

            // TODO: make sure there are no child elements. Easy way?
            file = project.resolveFile(project.replaceProperties(fileAttribute));
            Assert.isTrue(file.exists(),
                "'file' attribute specified by the 'bootstrap' element does not exist: " + file.getPath());

            return parse();
        }

        BootStrapConstraints bootStrap = parseShortHand(bootStrapEl);

        parseCores(bootStrapEl, bootStrap);
        parseJdks(bootStrapEl, bootStrap);
        parseDependencies(bootStrapEl, bootStrap);

        return bootStrap;
    }

    private BootStrapConstraints parseShortHand(Element bootStrapEl) {
        // These are shorthand contraints for the common options
        BootStrapConstraints bootStrap = new BootStrapConstraints();
        VersionRangeUnion core = parseVersionRange(getAttribute(bootStrapEl, "core"));

        if (core != null) {
            CoreConstraint coreConstraint = new CoreConstraint();
            coreConstraint.setVersion(core);
            bootStrap.getCoreConstraints().add(coreConstraint);
        }

        JdkConstraint jdkConstraint = parseJdkAttributes(bootStrapEl);

        if (!jdkConstraint.isEmpty()) {
            bootStrap.getJdkConstraints().add(jdkConstraint);
        }

        return bootStrap;
    }

    private void parseDependencies(Element bootStrapEl, BootStrapConstraints bootStrap) {
        for (Iterator i = applyProfiles(getChildren(bootStrapEl, "boot-dependency", false)).iterator(); i.hasNext();) {
            Element dependencyEl = (Element)i.next();
            String group = getAttribute(dependencyEl, "group");
            String name = getAttribute(dependencyEl, "name");
            name = (name == null) ? group.substring(group.lastIndexOf(".") + 1) : name;

            String version = getAttribute(dependencyEl, "version");

            DependencyConstraint dependency = new DependencyConstraint(group, name, parseVersionRange(version),
                    getAttribute(dependencyEl, "file"), getAttribute(dependencyEl, "url"));
            bootStrap.getDependencyContraints().add(dependency);
        }
    }

    private void parseJdks(Element bootStrapEl, BootStrapConstraints bootStrap) {
        // Parse all jkds & jdk elements, ensuring they are added in the order they are declared
        for (Iterator i = applyProfiles(getChildren(bootStrapEl, new String[] { "jdks", "jdk" }, false)).iterator();
                i.hasNext();) {
            Element element = (Element)i.next();

            if (element.getNodeName().equals("jdks")) {
                JdkConstraint defaults = parseJdk(element);

                for (Iterator j = applyProfiles(getChildren(element, "jdk", false)).iterator(); j.hasNext();) {
                    Element jdkEl = (Element)j.next();
                    JdkConstraint jdkConstraint = parseJdk(jdkEl);
                    jdkConstraint.setDefaults(defaults);
                    bootStrap.getJdkConstraints().add(jdkConstraint);
                }
            } else {
                bootStrap.getJdkConstraints().add(parseJdk(element));
            }
        }
    }

    private void parseCores(Element bootStrapEl, BootStrapConstraints bootStrap) {
        // Parse all cores & core elements, ensuring they are added in the order they are declared
        for (Iterator i = applyProfiles(getChildren(bootStrapEl, new String[] { "cores", "core" }, false)).iterator();
                i.hasNext();) {
            Element element = (Element)i.next();

            if (element.getNodeName().equals("cores")) {
                CoreConstraint defaults = parseCore(element);

                for (Iterator j = applyProfiles(getChildren(element, "core", false)).iterator(); j.hasNext();) {
                    Element coreEl = (Element)j.next();
                    CoreConstraint core = parseCore(coreEl);
                    core.setDefaults(defaults);
                    bootStrap.getCoreConstraints().add(core);
                }
            } else {
                bootStrap.getCoreConstraints().add(parseCore(element));
            }
        }
    }

    private JdkConstraint parseJdk(Element jdkEl) {
        JdkConstraint jdkConstraint = parseJdkAttributes(jdkEl);

        for (Iterator i = applyProfiles(getChildren(jdkEl, "sysproperty", false)).iterator(); i.hasNext();) {
            Element syspropertyEl = (Element)i.next();
            String key = getAttribute(syspropertyEl, "name");
            String value = getAttribute(syspropertyEl, "value");
            String requriedString = getAttribute(syspropertyEl, "required");
            boolean required = (requriedString == null) || Boolean.valueOf(requriedString).booleanValue();
            JdkConstraint.SystemPropertyValue propertyValue = new JdkConstraint.SystemPropertyValue(required, value);
            jdkConstraint.getSystemProperties().put(key, propertyValue);
        }

        return jdkConstraint;
    }

    private JdkConstraint parseJdkAttributes(Element jdkEl) {
        JdkConstraint jdkConstraint = new JdkConstraint();
        jdkConstraint.setJavaVendor(getAttribute(jdkEl, "java-vendor"));
        jdkConstraint.setJavaVersion(parseVersionRange(getAttribute(jdkEl, "java-version")));
        jdkConstraint.setJvmVersion(parseVersionRange(getAttribute(jdkEl, "jvm-version")));
        jdkConstraint.setSpecVersion(parseVersionRange(getAttribute(jdkEl, "spec-version")));
        jdkConstraint.setJavaJvmVendor(getAttribute(jdkEl, "jvm-vendor"));
        jdkConstraint.setJvmArgs(getAttribute(jdkEl, "jvm-args"));

        return jdkConstraint;
    }

    private VersionRangeUnion parseVersionRange(String range) {
        return (range == null) ? null : VersionRangeUnion.parse(range);
    }

    private List applyProfiles(List elements) {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            String expression = getAttribute(element, "profiles");

            if ((expression != null) && !profilesMatcher.matches(expression, activeProfiles)) {
                i.remove();
            }
        }

        return elements;
    }

    protected static Set toSet(String string) {
        Set tokens = new HashSet();

        if (string != null) {
            StringTokenizer tokenizer = new StringTokenizer(string, ",");

            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                tokens.add(token);
            }
        }

        return tokens;
    }

    private CoreConstraint parseCore(Element coreEl) {
        CoreConstraint core = new CoreConstraint();
        core.setVersion(parseVersionRange(getAttribute(coreEl, "version")));

        return core;
    }
}
