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


package ws.quokka.core.plugin_spi;

import org.apache.tools.ant.BuildException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;


/**
 * PluginState provides a storage mechanism for plugins. A global instance of PluginState is
 * available to all plugins via {@link ws.quokka.core.plugin_spi.Resources#getPluginState()}.
 * <br>
 * Currently, each plugin target runs with it's own class loader. Therefore, if you want to
 * communicate between targets, you must ensure that your data only uses classes available
 * on the core classloader, or you need to serialize the data.
 * <br>
 * There are 2 mechanisms for storing state. The first uses the get and set methods and
 * assumes that data you are storing is of classes accessible on the core class loader (this includes
 * java primitives, collections and Ant classes). The second uses serialise and deserialise and assumes you are using
 * a class that is defined within your plugin.
 * <br>
 * Currently, access to the plugin state is single-threaded. However, in the future targets
 * may be run in parallel. In particular, there are plans to run multiple sub-modules in parallel
 * for multi-project builds. In this case it will become important to serialise access to the
 * parent project's plugin state. Therefore, if you are performing multiple operations you should
 * do so within an synchronized block with a lock. e.g. within a plugin
 * <pre>
 * PluginState state = getResources().getPluginState();
 * String key = "someKey";
 * synchronized (state.getLock(key)) {
 *     MyClass myObject = state.deserialise(key);
 *     myObject = myObject != null ? myObject : new MyClass();
 *     myObject.setValue(...); // Perform any updates to the state
 *     state.serialise(key, myObject);
 * }
 *</pre>
 * If you are not serialising, you can use the {@link #get(String, Object)} method that accepts a default value as
 * this will do the guarding for you. Note however, that if you subsequently update the state of the object, you should
 * take care that the modification is threadsafe.
 */
public class PluginState {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map state = new HashMap();
    private Map locks = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the object for the given key and if it doesn't exist, adds the default value
     */
    public synchronized Object get(String key, Object defaultValue) {
        Object value = state.get(key);

        if (value == null) {
            if (defaultValue != null) {
                state.put(key, defaultValue);
            }

            return defaultValue;
        }

        return value;
    }

    /**
     * Returns the object with the given key, or null if it doesn't exist
     */
    public synchronized Object get(String key) {
        return state.get(key);
    }

    /**
     * Stores the value with the given key
     */
    public synchronized void put(String key, Object value) {
        state.put(key, value);
    }

    /**
     * Serialises the value to a byte array and stores it with the key given
     */
    public synchronized void serialise(String key, Serializable value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            state.put(key, baos.toByteArray());
        } catch (IOException e) {
            throw new BuildException("Could not serialise object: key=" + key, e);
        }
    }

    /**
     * Deserialises the object with the given key. Returns null if not found
     */
    public synchronized Serializable deserialise(String key) {
        try {
            byte[] data = (byte[])state.get(key);

            if (data == null) {
                return null;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);

            return (Serializable)ois.readObject();
        } catch (Exception e) {
            throw new BuildException("Could not deserialise object: key=" + key, e);
        }
    }

    /**
     * Returns a lock that can be synchronized on to prevent concurrent access.
     */
    public synchronized Object getLock(String key) {
        Object lock = locks.get(key);

        if (lock == null) {
            lock = new Object();
            locks.put(key, lock);
        }

        return lock;
    }
}
