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

import ws.quokka.core.bootstrap_util.ExceptionHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Annotations essentially wraps a map, allowing objects to store information that is not related
 * to their core functionality. e.g. Quokka uses annotations to store the location (line number) that
 * the object was defined in within a configuration file
 */
public class Annotations implements Cloneable {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map annotations = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Stores an annotation
     */
    public void put(String key, Object value) {
        annotations.put(key, value);
    }

    /**
     * Returns the annotation, or null if it doesn't exist
     */
    public Object get(String key) {
        return annotations.get(key);
    }

    /**
     * Removes an annotation
     */
    public Object remove(String key) {
        return annotations.remove(key);
    }

    /**
     * Returns the keys of all annotations
     */
    public Set entrySet() {
        return Collections.unmodifiableMap(annotations).entrySet();
    }

    /**
     * Clones the underlying map, but not the values contained within it. i.e. a shallow copy
     */
    public Object clone() {
        return new ExceptionHandler() {
                public Object run() throws CloneNotSupportedException {
                    Annotations clone = (Annotations)Annotations.super.clone();
                    clone.annotations = (Map)((HashMap)clone.annotations).clone();

                    return clone;
                }
            }.soften();
    }
}
