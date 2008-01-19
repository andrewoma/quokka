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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Reflect;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * ServiceFactory provides an extensible mechansim for detecting "services" that implement a given interface.
 */
public class ServiceFactory {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Uses a similar approach to ant to get the resolver instance. It first checks the properties passed to
     * it, then system properties, then a "service entry" from the class path.
     */
    public Object getService(Class serviceInterface, Map properties) {
        // Define class loaders in precedence
        List classLoaders = new ArrayList();

        //        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        //        if (loader != null) {
        //            classLoaders.add(loader);
        //        }
        classLoaders.add(getClass().getClassLoader());

        //        classLoaders.add(ClassLoader.getSystemClassLoader());
        String serviceClass = (String)properties.get(serviceInterface.getName());

        if ((serviceClass == null) || serviceClass.trim().equals("")) {
            serviceClass = System.getProperty(serviceInterface.getName());

            if ((serviceClass == null) || serviceClass.trim().equals("")) {
                serviceClass = getFromMetaInf(serviceInterface, classLoaders);
            }
        }

        if (serviceClass == null) {
            return null;
        }

        Class clazz = null;

        for (Iterator i = classLoaders.iterator(); i.hasNext();) {
            ClassLoader classLoader = (ClassLoader)i.next();

            try {
                clazz = classLoader.loadClass(serviceClass);
            } catch (ClassNotFoundException ex) {
                // try next method
            }
        }

        Assert.isTrue(clazz != null,
            "Cannot locate service class on the thread, class or system class loaders: " + serviceInterface);

        return new Reflect().construct(clazz);
    }

    private String getFromMetaInf(Class serviceInterface, List classLoaders) {
        // Try a JDK1.3 'service' with entry in  META-INF/services.
        // Copied idea from ant which copied from JAXP.
        String serviceId = "META-INF/services/" + serviceInterface.getName();

        InputStream is = null;

        for (Iterator i = classLoaders.iterator(); i.hasNext() && (is == null);) {
            ClassLoader classLoader = (ClassLoader)i.next();
            is = classLoader.getResourceAsStream(serviceId);
        }

        if (is == null) {
            return null;
        }

        // Snippet copied from ant ... cleaned up stream handling a little
        // This code is needed by EBCDIC and other strange systems.
        // It's a fix for bugs reported in xerces
        InputStreamReader isr;

        try {
            isr = new InputStreamReader(is, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            isr = new InputStreamReader(is);
        }

        final BufferedReader rd = new BufferedReader(isr); // never throws

        final String[] resolverClass = new String[] { null };
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    try {
                        resolverClass[0] = rd.readLine();
                    } finally {
                        rd.close();
                    }
                }
            };

        return ((resolverClass[0] == null) || resolverClass[0].equals("")) ? null : resolverClass[0];
    }
}
