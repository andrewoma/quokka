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

import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.AnnotatedObject;

import java.io.File;


/**
 *
 */
public class XmlConverterTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private XmlConverter converter = new XmlConverter();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testConvert() {
        SomeBeanConverter someBeanConverter = new SomeBeanConverter();
        converter.add(someBeanConverter);
        converter.add(new ReflectionConverter(SomeOtherBean.class));

        // Add some context
        someBeanConverter.addContext("context1", "value1");

        // Convert to xml
        SomeBean someBean = new SomeBean("name", null, true, Boolean.TRUE, new File("C:\\Temp"),
                new SomeOtherBean("otherName"));
        String xml = converter.toXml(someBean, "somebean", "pub", "sys");

        // Convert back
        SomeBean converted = (SomeBean)converter.fromXml(SomeBean.class, xml, new Document.NullEntityResolver());
        assertEquals(someBean, converted);

        // Remove context
        converter.removeContext("context1");
        assertNull(converter.getContext("context1"));
    }

    public void testContext() {
        converter.addContext("key1", "value1");
        converter.addContext("key2", "value2");
        assertEquals("value1", converter.getContext("key1"));
        assertEquals("value2", converter.getContext("key2"));
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class SomeBean {
        private String name;
        private String desc;
        private boolean boolValue;
        private Boolean booleanValue;
        private File file;
        private SomeOtherBean otherBean;

        public SomeBean() {
        }

        public SomeBean(String name, String desc, boolean boolValue, Boolean booleanValue, File file,
            SomeOtherBean otherBean) {
            this.name = name;
            this.desc = desc;
            this.boolValue = boolValue;
            this.booleanValue = booleanValue;
            this.file = file;
            this.otherBean = otherBean;
        }

        public SomeOtherBean getOtherBean() {
            return otherBean;
        }

        public void setOtherBean(SomeOtherBean otherBean) {
            this.otherBean = otherBean;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }

            SomeBean someBean = (SomeBean)o;

            if (boolValue != someBean.boolValue) {
                return false;
            }

            if ((booleanValue != null) ? (!booleanValue.equals(someBean.booleanValue)) : (
                        someBean.booleanValue != null
                    )) {
                return false;
            }

            if ((desc != null) ? (!desc.equals(someBean.desc)) : (someBean.desc != null)) {
                return false;
            }

            if ((file != null) ? (!file.equals(someBean.file)) : (someBean.file != null)) {
                return false;
            }

            if ((name != null) ? (!name.equals(someBean.name)) : (someBean.name != null)) {
                return false;
            }

            if ((otherBean != null) ? (!otherBean.equals(someBean.otherBean)) : (someBean.otherBean != null)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = ((name != null) ? name.hashCode() : 0);
            result = (31 * result) + ((desc != null) ? desc.hashCode() : 0);
            result = (31 * result) + (boolValue ? 1 : 0);
            result = (31 * result) + ((booleanValue != null) ? booleanValue.hashCode() : 0);
            result = (31 * result) + ((file != null) ? file.hashCode() : 0);
            result = (31 * result) + ((otherBean != null) ? otherBean.hashCode() : 0);

            return result;
        }
    }

    public static class SomeOtherBean extends AnnotatedObject {
        private String name;

        public SomeOtherBean() {
        }

        public SomeOtherBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }

            SomeOtherBean that = (SomeOtherBean)o;

            if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return ((name != null) ? name.hashCode() : 0);
        }
    }

    public static class SomeBeanConverter extends ReflectionConverter {
        public SomeBeanConverter() {
            super(SomeBean.class);
        }

        public void toXml(Object object, Element element) {
            super.toXml(object, element);

            SomeBean someBean = (SomeBean)object;

            if (someBean.getOtherBean() != null) {
                Converter converter = getConverter(SomeOtherBean.class);
                converter.toXml(someBean.getOtherBean(), element.addChild("otherBean"));
            }
        }

        public Object fromXml(Element element) {
            SomeBean someBean = (SomeBean)super.fromXml(element);
            Element otherBeanEl = element.getChild("otherBean");

            if (otherBeanEl != null) {
                Converter converter = getConverter(SomeOtherBean.class);
                someBean.setOtherBean((SomeOtherBean)converter.fromXml(otherBeanEl));
            }

            // Test context retrieval
            assertEquals("value1", getContext("context1"));
            assertEquals("value1", getXmlConverter().getContext("context1"));

            return someBean;
        }
    }
}
