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
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 */
public class JdkConstraint {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private VersionRangeUnion javaVersion;
    private String javaVendor;
    private VersionRangeUnion jvmVersion;
    private String javaJvmVendor;
    private VersionRangeUnion specVersion;
    private String jvmArgs;
    private Map systemProperties = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public void setJavaVendor(String javaVendor) {
        this.javaVendor = javaVendor;
    }

    public VersionRangeUnion getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(VersionRangeUnion javaVersion) {
        this.javaVersion = javaVersion;
    }

    public VersionRangeUnion getJvmVersion() {
        return jvmVersion;
    }

    public void setJvmVersion(VersionRangeUnion jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    public VersionRangeUnion getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(VersionRangeUnion specVersion) {
        this.specVersion = specVersion;
    }

    public String getJavaJvmVendor() {
        return javaJvmVendor;
    }

    public void setJavaJvmVendor(String javaJvmVendor) {
        this.javaJvmVendor = javaJvmVendor;
    }

    public Map getSystemProperties() {
        return systemProperties;
    }

    public void addSystemPropery(String key, String value, boolean required) {
        systemProperties.put(key, new SystemPropertyValue(required, value));
    }

    public void setDefaults(JdkConstraint defaults) {
        javaVersion = applyDefault(javaVersion, defaults.javaVersion);
        javaVendor = applyDefault(javaVendor, defaults.javaVendor);
        jvmVersion = applyDefault(jvmVersion, defaults.jvmVersion);
        javaJvmVendor = applyDefault(javaJvmVendor, defaults.javaJvmVendor);
        specVersion = applyDefault(specVersion, defaults.specVersion);
        jvmArgs = applyDefault(jvmArgs, defaults.jvmArgs);

        for (Iterator i = defaults.getSystemProperties().entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();

            if (!systemProperties.containsKey(entry.getKey())) {
                systemProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private String applyDefault(String value, String defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    private VersionRangeUnion applyDefault(VersionRangeUnion value, VersionRangeUnion defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    public boolean matches(Jdk available, boolean matchOptional) {
        boolean match = matches(available, "java.vendor", javaVendor);
        match = match && matches(available, "java.vm.vendor", javaJvmVendor);
        match = match && matches(available, "java.version", javaVersion, true);
        match = match && matches(available, "java.specification.version", specVersion, true);
        match = match && matches(available, "java.vm.version", jvmVersion, false);

        for (Iterator i = systemProperties.entrySet().iterator(); match && i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            JdkConstraint.SystemPropertyValue value = (JdkConstraint.SystemPropertyValue)entry.getValue();

            if (value.isRequired() || (!value.isRequired() && !matchOptional)) {
                String availableValue = (String)available.getProperties().get(key);
                match = value.getValue().equals(availableValue);
                Log.get().debug("   System property " + key + (match ? " matches" : " doesn't match") + ": constraint="
                    + value.getValue() + ", jdk=" + availableValue);
            }
        }

        return match;
    }

    private boolean matches(Jdk jdk, String property, VersionRangeUnion constraintValue, boolean replaceUnderscores) {
        String jdkValue = (String)jdk.getProperties().get(property);

        if (jdkValue == null) {
            return constraintValue == null;
        }

        jdkValue = replaceUnderscores ? jdkValue.replace('_', '.') : jdkValue;

        boolean matches = (constraintValue == null) || constraintValue.isInRange(new Version(jdkValue));

        if (constraintValue != null) {
            Log.get().debug("   " + property + (matches ? " matches" : " doesn't match") + ": constraint="
                + constraintValue + ", jdk=" + jdkValue);
        }

        return matches;
    }

    private boolean matches(Jdk jdk, String property, String constraintValue) {
        String jdkValue = (String)jdk.getProperties().get(property);
        boolean matches = (constraintValue == null) || constraintValue.equals(jdkValue);

        if (constraintValue != null) {
            Log.get().debug("   " + property + (matches ? " matches: " : " doesn't match: ") + "constraint="
                + constraintValue + ", jdk=" + jdkValue);
        }

        return matches;
    }

    public boolean isEmpty() {
        return (javaJvmVendor == null) && (javaVendor == null) && (javaVersion == null) && (jvmVersion == null)
        && (jvmArgs == null) && (systemProperties.size() == 0) && (specVersion == null);
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class SystemPropertyValue {
        private boolean required;
        private String value;

        public SystemPropertyValue(boolean required, String value) {
            this.required = required;
            this.value = value;
        }

        public boolean isRequired() {
            return required;
        }

        public String getValue() {
            return value;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }

            SystemPropertyValue that = (SystemPropertyValue)o;

            if (required != that.required) {
                return false;
            }

            return value.equals(that.value);
        }

        public int hashCode() {
            int result;
            result = (required ? 1 : 0);
            result = (31 * result) + value.hashCode();

            return result;
        }
    }
}
