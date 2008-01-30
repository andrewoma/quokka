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

import ws.quokka.core.test.AbstractTest;

import java.util.HashMap;


/**
 *
 */
public class ServiceFactoryTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    ServiceFactory factory = new ServiceFactory();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testPrecedence() {
        // Check passed in properties have highest precedence
        HashMap properties = new HashMap();
        properties.put(SomeInterface.class.getName(), SomeService.class.getName());
        System.setProperty(SomeInterface.class.getName(), SomeService2.class.getName());

        // Service3 defined in META-INF/service/...
        SomeInterface someInterface = (SomeInterface)factory.getService(SomeInterface.class, properties);
        assertEquals(SomeService.class, someInterface.getClass());

        // Check System properties have second
        someInterface = (SomeInterface)factory.getService(SomeInterface.class, new HashMap());
        assertEquals(SomeService2.class, someInterface.getClass());

        // Check META-INF as third
        System.setProperty(SomeInterface.class.getName(), ""); // JDK 1.2 doesn't have clear
        someInterface = (SomeInterface)factory.getService(SomeInterface.class, new HashMap());
        assertEquals(SomeService3.class, someInterface.getClass());
    }

    //~ Inner Interfaces -----------------------------------------------------------------------------------------------

    public static interface SomeInterface {
        String getName();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class SomeService implements SomeInterface {
        public String getName() {
            return getClass().getName();
        }
    }

    public static class SomeService2 extends SomeService {
    }

    public static class SomeService3 extends SomeService {
    }
}
