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


package ws.quokka.core.plugin_spi.support;

import java.util.*;


/**
 * Keys is a helper to aggregrate keys easily for verification via TypedProperties.verify
 */
public class Keys {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Set keys = new HashSet();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Keys() {
    }

    public Keys(String key) {
        add(key);
    }

    public Keys(Collection keys) {
        add(keys);
    }

    public Keys(String[] keys) {
        add(keys);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Keys add(String[] keys) {
        this.keys.addAll(Arrays.asList(keys));

        return this;
    }

    public Keys add(Collection keys) {
        this.keys.addAll(keys);

        return this;
    }

    public Keys add(String key) {
        this.keys.add(key);

        return this;
    }

    public Set toSet() {
        return Collections.unmodifiableSet(keys);
    }
}
