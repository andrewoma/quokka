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


package ws.quokka.core.bootstrap_util;

import org.apache.tools.ant.BuildException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Reflect is a helper class for using reflection
 */
public class Reflect {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Clones an object by invoking the clone method
     */
    public Object clone(Object object) {
        return invoke(object, "clone", new Object[] {  });
    }

    /**
     * Returns all fields for a class (including those defined in superclasses) and set them to accessible
     */
    public List getFields(Class clazz) {
        List result = new ArrayList();

        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            Field.setAccessible(fields, true);

            for (int i = fields.length - 1; i >= 0; i--) {
                Field field = fields[i];
                result.add(field);
            }

            clazz = clazz.getSuperclass();
        }

        Collections.reverse(result);

        return result;
    }

    /**
     * Invokes a method on an object with the parameters provided
     */
    public Object invoke(Object object, String name, Object[] parameters) {
        Class[] parameterTypes = new Class[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Object parameter = parameters[i];
            parameterTypes[i] = parameter.getClass();
        }

        Method method = getMethod(object.getClass(), name, parameterTypes);

        return invoke(method, object, parameters);
    }

    /**
     * Invokes a method on an a method object with the parameters provided
     */
    public Object invoke(Method method, Object object, Object[] parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException)e.getTargetException();
            }

            throw new BuildException(e.getTargetException());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Returns a method matching the class, name and parameter types provided (searches superclasses as well)
     */
    public Method getMethod(Class clazz, String name, Class[] parameterTypes) {
        while (true) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);

                return method;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();

                if (clazz == null) {
                    throw new BuildException(e);
                }
            }
        }
    }

    /**
     * Returns the value of a field from the given object
     */
    public Object get(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Sets the value of a field for given object
     */
    public void set(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Constructs an object using its no-args constructor, softening any exceptions
     */
    public Object construct(Class clazz) {
        Constructor constructor;

        try {
            constructor = clazz.getDeclaredConstructor(new Class[] {  });
        } catch (NoSuchMethodException e) {
            throw new BuildException("A no-arg constructor must be provided", e);
        }

        constructor.setAccessible(true);

        try {
            return constructor.newInstance(new Object[] {  });
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Returns a field from a class matching the name given (searches super classes also)
     */
    public Field getField(Class clazz, String name) {
        while (true) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);

                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();

                if (clazz == null) {
                    throw new BuildException(e);
                }
            }
        }
    }
}
