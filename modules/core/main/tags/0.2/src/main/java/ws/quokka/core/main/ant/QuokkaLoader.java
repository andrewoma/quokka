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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.AntClassLoader;

import ws.quokka.core.model.Target;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * QuokkaLoader is a class loader that keeps track of the number of class loaders
 * allocated versus finalized to check for class laoder leaks. At present, the jalopy
 * plugin is known the leak loaders, although the underlying cause has not been identified.
 * <p/>
 * Useful options:
 * Debugging:   QUOKKA_OPTS=-verbose:gc -XX:+PrintClassHistogram -XX:+PrintGCDetails
 * Work-around: QUOKKA_OPTS=-XX:MaxPermSize=128m
 */
public class QuokkaLoader extends AntClassLoader {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static Map loaders = new TreeMap();

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String name;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public QuokkaLoader(Target target, ClassLoader parent, org.apache.tools.ant.Project project,
        org.apache.tools.ant.types.Path classpath) {
        super(parent, project, classpath);
        name = target.getName();
        add(1);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    private static void initialise() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.out.println("Checking for leaking class loaders ...");
                    System.gc();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.gc();

                    for (Iterator i = loaders.entrySet().iterator(); i.hasNext();) {
                        Map.Entry entry = (Map.Entry)i.next();
                        System.out.println(entry.getKey() + " -> " + entry.getValue());
                    }
                }
            });
    }

    protected void finalize() throws Throwable {
        super.finalize();
        add(-1);
    }

    private void add(int i) {
        synchronized (loaders) {
            if (loaders.size() == 0) {
                initialise();
            }

            Integer count = (Integer)loaders.get(name);

            if (count == null) {
                count = new Integer(0);
            }

            count = new Integer(count.intValue() + i);
            loaders.put(name, count);
        }
    }
}
