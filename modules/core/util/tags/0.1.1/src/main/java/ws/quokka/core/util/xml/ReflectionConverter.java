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

import ws.quokka.core.bootstrap_util.Reflect;
import ws.quokka.core.util.Annotated;

import java.io.File;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * ReflectionConverter
 */
public class ReflectionConverter extends AbstractConverter {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map defaults = new HashMap();
    private List excluded = new ArrayList();
    private Class clazz;
    private Reflect reflect = new Reflect();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ReflectionConverter(Class clazz) {
        this.clazz = clazz;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public boolean supports(Class clazz) {
        return clazz.isAssignableFrom(this.clazz);
    }

    public Object setObject(Object object, Element element, List fields) {
        for (Iterator i = fields.iterator(); i.hasNext();) {
            Field field = (Field)i.next();
            String fieldName = field.getName();
            Object value = element.getAttribute(toAttribute(fieldName));

            if (value != null) {
                if (field.getType().equals(Boolean.TYPE) || field.getType().equals(Boolean.class)) {
                    value = Boolean.valueOf((String)value);
                } else if (field.getType().equals(File.class)) {
                    value = new File((String)value);
                }
            }

            Object defaultValue = defaults.get(fieldName);
            value = (value == null) ? defaultValue : value;

            if (value == null) {
                continue;
            }

            reflect.set(field, object, value);
        }

        return object;
    }

    private String toAttribute(String fieldName) {
        StringBuffer attribute = new StringBuffer();

        for (int i = 0; i < fieldName.length(); i++) {
            char ch = fieldName.charAt(i);

            if (Character.isUpperCase(ch)) {
                attribute.append('-').append(Character.toLowerCase(ch));
            } else {
                attribute.append(ch);
            }
        }

        return attribute.toString();
    }

    public void setElement(Object object, Element element, List fields) {
        for (Iterator i = fields.iterator(); i.hasNext();) {
            Field field = (Field)i.next();
            String fieldName = field.getName();
            Object value = reflect.get(field, object);

            if ((value == null) || value.equals(defaults.get(fieldName))) {
                continue; // Don't set null or default values
            }

            element.setAttribute(toAttribute(fieldName), value.toString());
        }
    }

    public Object fromXml(Element element) {
        Object object = construct();

        if (object instanceof Annotated) {
            ((Annotated)object).getAnnotations().put("locator", LocatorDomParser.getLocator(element.getElement()));
        }

        return setObject(object, element, getFields());
    }

    public Object construct() {
        return reflect.construct(clazz);
    }

    private List getFields() {
        List fields = reflect.getFields(clazz);

        for (Iterator i = fields.iterator(); i.hasNext();) {
            Field field = (Field)i.next();

            if (excluded.contains(field.getName()) || Modifier.isStatic(field.getModifiers())
                    || Modifier.isTransient(field.getModifiers()) || !supportedType(field.getType())) {
                i.remove();
            }
        }

        return fields;
    }

    private boolean supportedType(Class type) {
        return type.equals(Boolean.class) || type.equals(Boolean.TYPE) || type.equals(String.class)
        || type.equals(File.class);
    }

    public void toXml(Object object, Element element) {
        setElement(object, element, getFields());
    }

    public void addDefault(String field, Object value) {
        defaults.put(field, value);
    }

    public void addExclusion(String field) {
        excluded.add(field);
    }
}
