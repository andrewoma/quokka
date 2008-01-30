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


package ws.quokka.core.itest;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * FilteredClassLoader will only load from the parent if it loadFromParent the inclusions and exclusions given.
 */
public class FilteredClassLoader extends URLClassLoader {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    Filter filter;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public FilteredClassLoader(URL[] urls, ClassLoader parent, Filter filter) {
        super(urls, parent);
        this.filter = filter;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);

        if (c == null) {
            if (filter.loadFromParent(name)) {
                try {
                    if (getParent() != null) {
                        c = getParent().loadClass(name);
                    }
                } catch (ClassNotFoundException e) {
                    // If still not found, then call findClass in order
                    // to find the class.
                    c = findClass(name);
                }
            } else {
                c = findClass(name);
            }
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }
}
