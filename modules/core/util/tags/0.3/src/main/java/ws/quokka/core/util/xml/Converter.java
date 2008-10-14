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


package ws.quokka.core.util.xml;


/**
 * Converter
 */
public interface Converter {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * This method will be called when the converter is added to an {@link ws.quokka.core.util.xml.XmlConverter}
     */
    void setXmlConverter(XmlConverter converter);

    /**
     * Returns true if this converter supports the class given
     */
    boolean supports(Class clazz);

    /**
     * Converts the element to an object
     */
    Object fromXml(Element element);

    /**
     * Converts the object to an element
     */
    void toXml(Object object, Element parent);
}
