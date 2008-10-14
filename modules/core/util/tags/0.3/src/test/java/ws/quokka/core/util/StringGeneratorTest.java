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

import junit.framework.TestCase;

import ws.quokka.core.bootstrap_util.IOUtils;

import java.io.File;

import java.lang.reflect.Field;

import java.net.URL;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 */
public class StringGeneratorTest extends TestCase {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private StringGenerator generator = new StringGenerator();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testBeanExcludingDate() {
        generator.add(new StringGenerator.Exclusion() {
                public boolean isExcluded(Field field) {
                    return field.getDeclaringClass().equals(SomeBean.class) && field.getName().equals("date");
                }
            });

        String string = generator.toString(new SomeBean());
        assertEquals("StringGeneratorTest$SomeBean[id=1, string=aString, file=C:\\SomeDir, anInt=5, url=http://quokka.ws, list=[item1, item2], map=[key1=value1, key2=value2], aClass=java.lang.String, cycle=id=1]",
            string);
    }

    public void testBeanWithDateGenerator() {
        generator.add(new StringGenerator.Generator() {
                public int match(Class type) {
                    return type.equals(Date.class) ? StringGenerator.EXACT_MATCH : StringGenerator.NO_MATCH;
                }

                public void toString(StringBuffer sb, Object obj, StringGenerator generator) {
                    sb.append(format.format((Date)obj));
                }
            });

        String string = generator.toString(new SomeBean());
        assertEquals("StringGeneratorTest$SomeBean[id=1, string=aString, date=2001-03-02, file=C:\\SomeDir, anInt=5, url=http://quokka.ws, list=[item1, item2], map=[key1=value1, key2=value2], aClass=java.lang.String, cycle=id=1]",
            string);
    }

    public void testNull() {
        String string = generator.toString(null);
        assertEquals("null", string);
        string = generator.toShortString(null);
        assertEquals("null", string);
    }

    public void testUnknown() {
        String string = generator.toString(new UnknownBeanContainer());
        assertTrue(string.startsWith(
                "StringGeneratorTest$UnknownBeanContainer[unknownBean=StringGeneratorTest$UnknownBean@"));
    }

    public void testClear() {
        generator.clear();
        generator.add(new StringGenerator.Generator() {
                public int match(Class type) {
                    return 0;
                }

                public void toString(StringBuffer sb, Object obj, StringGenerator generator) {
                    sb.append("hello");
                }
            });

        String string = generator.toString(new UnknownBeanContainer());
        assertEquals("StringGeneratorTest$UnknownBeanContainer[unknownBean=hello]", string);
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class UnknownBeanContainer {
        private UnknownBean unknownBean = new UnknownBean();
    }

    public static class UnknownBean {
    }

    public static class SomeBean implements StringGenerator.ShortString {
        private Long id = new Long(1);
        private String string = "aString";
        private Date date;
        private File file = new File("C:\\SomeDir");
        private int anInt = 5;
        private URL url = new IOUtils().createURL("http://quokka.ws");
        private List list = new ArrayList();
        private Map map = new TreeMap();
        private Class aClass = String.class;
        private SomeBean cycle = this;

        public SomeBean() {
            try {
                date = format.parse("2001-03-02");
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }

            list.add("item1");
            list.add("item2");
            map.put("key1", "value1");
            map.put("key2", "value2");
        }

        public String toShortString() {
            return "id=" + id;
        }
    }
}
