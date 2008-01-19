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


package ws.quokka.core.util;

import org.xml.sax.Locator;

import java.lang.reflect.Field;


/**
 * AnnotatedObject allows annotations to be stored for object instance. Annotations include information
 * such as the location of the declaration of the object in an xml file.
 */
public class AnnotatedObject extends AbstractObject implements Annotated {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String LOCATOR = "locator";

    static {
        STRING_GENERATOR.add(new StringGenerator.Exclusion() {
                public boolean isExcluded(Field field) {
                    return field.getDeclaringClass().equals(AnnotatedObject.class)
                    && field.getName().equals("annotations");
                }
            });
    }

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Annotations annotations = new Annotations();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Annotations getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotations annotations) {
        this.annotations = annotations;
    }

    public Locator getLocator() {
        return (Locator)annotations.get(LOCATOR);
    }

    public void setLocator(Locator locator) {
        annotations.put(LOCATOR, locator);
    }
}
