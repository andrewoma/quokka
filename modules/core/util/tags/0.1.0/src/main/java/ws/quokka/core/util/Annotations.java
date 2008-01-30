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
 *
 */
public class Annotations implements Cloneable {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map annotations = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void put(String key, Object value) {
        annotations.put(key, value);
    }

    public Object get(String key) {
        return annotations.get(key);
    }

    public Set entrySet() {
        return Collections.unmodifiableMap(annotations).entrySet();
    }

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
