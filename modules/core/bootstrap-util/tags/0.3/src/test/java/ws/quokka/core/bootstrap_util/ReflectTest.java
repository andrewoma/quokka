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

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class ReflectTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Reflect reflect = new Reflect();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testClone() {
        Set set = new HashSet();
        set.add("Hello");
        set.add("There");

        Set clone = (Set)reflect.clone(set);
        assertEquals(set, clone);
    }

    public void testGetField() {
        Field field = reflect.getField(SomeClass.class, "field1");
        assertEquals(new Integer(1), reflect.get(field, new SomeClass()));

        field = reflect.getField(SomeExtendedClass.class, "field1");
        assertEquals(new Integer(1), reflect.get(field, new SomeExtendedClass()));

        field = reflect.getField(SomeExtendedClass.class, "field3");
        assertEquals("field3", reflect.get(field, new SomeExtendedClass()));
    }

    public void testGetNonExistentField() {
        try {
            reflect.getField(SomeExtendedClass.class, "xxxxxxxx");
            fail();
        } catch (BuildException e) {
            assertTrue(e.getCause() instanceof NoSuchFieldException);
        }
    }

    public void testGetIllegal() {
        try {
            Field field = reflect.getField(SomeExtendedClass.class, "field1");
            field.setAccessible(false);
            reflect.get(field, new SomeExtendedClass());
            fail();
        } catch (BuildException e) {
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }

    public void testGetMethod() {
        Method method = reflect.getMethod(SomeClass.class, "method1", new Class[] { String.class });
        String result = (String)reflect.invoke(method, new SomeClass(), new Object[] { "Hello" });
        assertEquals("method1", result);

        method = reflect.getMethod(SomeExtendedClass.class, "method1", new Class[] { String.class });
        result = (String)reflect.invoke(method, new SomeExtendedClass(), new Object[] { "Hello" });
        assertEquals("method1", result);

        method = reflect.getMethod(SomeExtendedClass.class, "method2", new Class[] { Integer.TYPE });
        result = (String)reflect.invoke(method, new SomeExtendedClass(), new Object[] { new Integer(1) });
        assertEquals("method2", result);
    }

    public void testGetNonExistentMethod() {
        try {
            reflect.getMethod(SomeExtendedClass.class, "xxxxxxxx", new Class[] {  });
            fail();
        } catch (BuildException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testInvoke() {
        assertEquals("method1", reflect.invoke(new SomeClass(), "method1", new Object[] { "Hello" }));
    }

    public void testInvokeException() {
        try {
            reflect.invoke(new SomeExtendedClass(), "method3", new Object[] {  });
            fail();
        } catch (RuntimeException e) {
            assertEquals("Fail", e.getMessage());
        }
    }

    public void testInvokeIllegal() {
        try {
            Method method = reflect.getMethod(SomeExtendedClass.class, "method1", new Class[] { String.class });
            method.setAccessible(false);
            reflect.invoke(method, new SomeExtendedClass(), new Object[] { "Hello" });
            fail();
        } catch (BuildException e) {
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }

    public void testGetFields() {
        Set names = new HashSet();
        names.add("field1");
        names.add("field2");
        names.add("field3");

        List fields = reflect.getFields(SomeExtendedClass.class);

        for (Iterator i = fields.iterator(); i.hasNext();) {
            Field field = (Field)i.next();

            if (names.contains(field.getName())) {
                names.remove(field.getName());
            }
        }

        assertEquals(0, names.size());
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class SomeClass {
        private int field1 = 1;
        public String field2 = "field2";

        private String method1(String param1) {
            return "method1";
        }
    }

    public static class SomeExtendedClass extends SomeClass {
        private String field3 = "field3";

        public String method2(int param) {
            return "method2";
        }

        public String method3() {
            throw new RuntimeException("Fail");
        }
    }
}
