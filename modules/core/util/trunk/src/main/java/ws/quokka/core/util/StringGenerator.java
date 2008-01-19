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

import ws.quokka.core.bootstrap_util.Reflect;

import java.io.File;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class StringGenerator {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final int ASSIGNMENT_MATCH_START = 100;
    private static final int DEFAULT_MATCH = 1000;
    public static final int EXACT_MATCH = 0;
    public static final int NO_MATCH = -1;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List generators = new ArrayList();
    private List exclusions = new ArrayList();
    private Map fieldsCache = new HashMap();
    private Map generatorCache = new HashMap();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public StringGenerator() {
        exclusions.add(new Exclusion() {
                public boolean isExcluded(Field field) {
                    return Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers());
                }
            });

        generators.add(new DefaultGenerator());
        generators.add(new SimpleTypesGenerator());
        generators.add(new AssignableGenerator(Class.class) {
                public void toString(StringBuffer sb, Object object, StringGenerator generator) {
                    sb.append(((Class)object).getName());
                }
            });
        generators.add(new AssignableGenerator(Collection.class) {
                public void toString(StringBuffer sb, Object object, StringGenerator generator) {
                    Collection collection = (Collection)object;
                    sb.append("[");

                    for (Iterator i = collection.iterator(); i.hasNext();) {
                        Object element = i.next();
                        sb.append(generator.toShortString(element));

                        if (i.hasNext()) {
                            sb.append(", ");
                        }
                    }

                    sb.append("]");
                }
            });
        generators.add(new AssignableGenerator(Map.class) {
                public void toString(StringBuffer sb, Object object, StringGenerator generator) {
                    Map map = (Map)object;
                    sb.append("[");

                    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
                        Map.Entry entry = (Map.Entry)i.next();
                        sb.append(entry.getKey()).append("=");
                        sb.append(generator.toShortString(entry.getValue()));

                        if (i.hasNext()) {
                            sb.append(", ");
                        }
                    }

                    sb.append("]");
                }
            });
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public synchronized void clear() {
        generators.clear();
        exclusions.clear();
        generatorCache.clear();
    }

    public synchronized void add(Generator generator) {
        generators.add(generator);
        generatorCache.clear();
    }

    public void add(Exclusion exclusion) {
        exclusions.add(exclusion);
        generatorCache.clear();
    }

    public String toString(Object object) {
        if (object == null) {
            return "null";
        }

        StringBuffer sb = new StringBuffer();
        Class clazz = object.getClass();
        sb.append(shortName(clazz)).append("["); // Short form of class name

        for (Iterator i = getFields(clazz).iterator(); i.hasNext();) {
            Field field = (Field)i.next();
            Object value = new Reflect().get(field, object);
            sb.append(field.getName()).append("=");
            sb.append(toShortString(value));

            if (i.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    public String toShortString(Object object) {
        if (object == null) {
            return "null";
        }

        if (object instanceof ShortString) {
            return (((ShortString)object).toShortString());
        } else {
            Generator generator = getGenerator(object.getClass());
            StringBuffer sb = new StringBuffer();
            generator.toString(sb, object, this);

            return sb.toString();
        }
    }

    private static String shortName(Class clazz) {
        return clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
    }

    private List getFields(Class type) {
        List fields = (List)fieldsCache.get(type);

        if (fields == null) {
            fields = new ArrayList();

            for (Iterator i = new Reflect().getFields(type).iterator(); i.hasNext();) {
                Field field = (Field)i.next();

                if (!excluded(field)) {
                    fields.add(field);
                }
            }

            fieldsCache.put(type, fields);
        }

        return fields;
    }

    private synchronized boolean excluded(Field field) {
        for (Iterator i = exclusions.iterator(); i.hasNext();) {
            Exclusion exclusion = (Exclusion)i.next();

            if (exclusion.isExcluded(field)) {
                return true;
            }
        }

        return false;
    }

    private synchronized Generator getGenerator(Class type) {
        Generator generator = (Generator)generatorCache.get(type);

        if (generator != null) {
            return generator;
        }

        int bestMatch = Integer.MAX_VALUE;
        Generator best = null;

        //        System.out.println(type.getName());
        for (Iterator i = generators.iterator(); i.hasNext();) {
            generator = (Generator)i.next();

            int match = generator.match(type);

            //            System.out.println("\t" + match + ": " + generator.getClass().getName());
            if (match != NO_MATCH) {
                if (match < bestMatch) {
                    bestMatch = match;
                    best = generator;
                }
            }
        }

        //        System.out.println("Selected: " + generator.getClass().getName());
        generatorCache.put(type, best);

        return best;
    }

    //~ Inner Interfaces -----------------------------------------------------------------------------------------------

    public static interface Exclusion {
        boolean isExcluded(Field field);
    }

    public static interface Generator {
        int match(Class type);

        void toString(StringBuffer sb, Object obj, StringGenerator generator);
    }

    public static interface ShortString {
        String toShortString();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public abstract static class AssignableGenerator implements Generator {
        private List types;

        protected AssignableGenerator(Class type) {
            types = Arrays.asList(new Class[] { type });
        }

        protected AssignableGenerator(List types) {
            this.types = types;
        }

        public int match(Class type) {
            for (Iterator i = types.iterator(); i.hasNext();) {
                Class genType = (Class)i.next();

                if (genType.isAssignableFrom(type)) {
                    // TODO: Handle interfaces properly (can't search super class)
                    int match = ASSIGNMENT_MATCH_START;

                    while ((genType != type) && (type != null)) {
                        match++;
                        type = type.getSuperclass();
                    }

                    return match;
                }
            }

            return NO_MATCH;
        }
    }

    public static class SimpleTypesGenerator extends AssignableGenerator {
        public static final Class[] TYPES = new Class[] {
                Boolean.class, String.class, Number.class, File.class, URL.class, Date.class
            };

        public SimpleTypesGenerator() {
            super(Arrays.asList(TYPES));
        }

        public void toString(StringBuffer sb, Object object, StringGenerator generator) {
            sb.append(object.toString());
        }
    }

    public static class DefaultGenerator implements Generator {
        public int match(Class type) {
            return DEFAULT_MATCH;
        }

        public void toString(StringBuffer sb, Object object, StringGenerator generator) {
            sb.append(shortName(object.getClass())).append("@").append(Integer.toHexString(System.identityHashCode(
                        object)));
        }
    }
}
