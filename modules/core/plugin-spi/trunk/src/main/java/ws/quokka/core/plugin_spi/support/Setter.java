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


package ws.quokka.core.plugin_spi.support;

import org.apache.tools.ant.IntrospectionHelper;

import ws.quokka.core.bootstrap_util.Assert;

import java.util.Locale;
import java.util.Map;


/**
 * Setter provides a mechanism to set fields on an object via reflection from property values.
 * In general, it is used to conveniently set a large set of optional attributes on an Ant task.
 */
public class Setter {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private TypedProperties properties;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Setter(TypedProperties properties) {
        this.properties = properties;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Sets the attributes on the target with values from the properties file.
     * If there is no property for a given attribute the target will NOT be set.
     * This allows default values to remain set.
     *
     * @param target
     * @param attributes must match the setter name without the "set" prefix. The property value will be
     * automatically converted to the required type using the same mechanism as Ant does with its own
     * XML attributes.
     */
    public void set(Object target, String[] attributes) {
        IntrospectionHelper helper = IntrospectionHelper.getHelper(target.getClass());
        verifyAttributes(helper, attributes);

        for (int i = 0; i < attributes.length; i++) {
            setAttribute(helper, target, attributes[i], attributes[i]);
        }
    }

    /**
     * This provides the equivalent of a compilation check for attributes, ensuring that the
     * provided attributes actually exist. It provides an extra level of safety as the integration tests
     * often will not verify all attributes.
     */
    private void verifyAttributes(IntrospectionHelper helper, String[] attributes) {
        if ("true".equals(properties.getProject().getProperty("q.internal.verifySetterAttributes"))) {
            Map actualAttributes = helper.getAttributeMap();

            for (int i = 0; i < attributes.length; i++) {
                String attribute = attributes[i];
                Assert.isTrue(actualAttributes.containsKey(attribute.toLowerCase(Locale.US)),
                    "Plugin error: attribute '" + attribute + "' does not exist");
            }
        }
    }

    private void setAttribute(IntrospectionHelper helper, Object target, String attribute, String property) {
        String prop = properties.getString(property, null);

        if (prop != null) {
            helper.setAttribute(properties.getProject(), target, attribute, prop);
        }
    }
}
