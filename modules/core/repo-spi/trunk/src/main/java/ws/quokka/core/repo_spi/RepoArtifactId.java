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


package ws.quokka.core.repo_spi;

import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.StringGenerator;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;


/**
 *
 */
public class RepoArtifactId extends AnnotatedObject implements Cloneable {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String ID_SEPARATOR = ":";
    public static final String PATH_SEPARATOR = "_";
    private static final String RESERVED_CHARS = "_ !:@;/\\*,%?<>+=#`(){}[]";

    static {
        STRING_GENERATOR.add(new StringGenerator.Generator() {
                public int match(Class type) {
                    return type.equals(Version.class) ? 1 : 0;
                }

                public void toString(StringBuffer sb, Object obj, StringGenerator generator) {
                    sb.append(obj.toString());
                }
            });
    }

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String group;
    private String name;
    private String type;
    private Version version;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    private RepoArtifactId() {
    }

    public RepoArtifactId(String group, String name, String type, String version) {
        this(group, name, type, new Version(version));
    }

    public RepoArtifactId(String group, String name, String type, Version version) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.type = type;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public RepoArtifactId merge(RepoArtifactId defaults) {
        return _merge(defaults);
    }

    private RepoArtifactId _merge(RepoArtifactId defaults) {
        return new RepoArtifactId(applyDefault(this.group, defaults.group), applyDefault(this.name, defaults.name),
            applyDefault(this.type, defaults.type), applyDefault(this.version, defaults.version));
    }

    public RepoArtifactId mergeDefaults() {
        String[] tokens = Strings.split(group, ".");

        return _merge(new RepoArtifactId(group, tokens[tokens.length - 1], "jar", version));
    }

    private String applyDefault(String value, String defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    private Version applyDefault(Version value, Version defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    public boolean isValid() {
        return isValid(group) && isValid(name) && isValid(type);
    }

    private boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            for (int j = 0; j < RESERVED_CHARS.length(); j++) {
                char reserved = RESERVED_CHARS.charAt(j);

                if (ch == reserved) {
                    return false;
                }
            }
        }

        return true;
    }

    public String toShortString() {
        return noNull(group) + ID_SEPARATOR + noNull(name) + ID_SEPARATOR + noNull(type) + ID_SEPARATOR
        + noNull(version);
    }

    public String toPathString() {
        return noNull(group) + PATH_SEPARATOR + noNull(name) + PATH_SEPARATOR + noNull(type) + PATH_SEPARATOR
        + noNull(version);
    }

    public static RepoArtifactId parse(String idString) {
        String[] tokens = Strings.trim(Strings.split(idString, ID_SEPARATOR));

        if ((tokens == null) || !(tokens.length == 4)) {
            throw new RuntimeException("id string is not is the format 'group" + ID_SEPARATOR + "name" + ID_SEPARATOR
                + "type" + ID_SEPARATOR + "version. value is " + idString);
        }

        return new RepoArtifactId(toNull(tokens[0]), toNull(tokens[1]), toNull(tokens[2]), new Version(tokens[3]));
    }

    private String noNull(Object object) {
        return (object == null) ? "" : object.toString();
    }

    private static String toNull(String string) {
        return string.equals("") ? null : string;
    }

    public boolean matches(RepoArtifactId id) {
        return ((id.group == null) || id.group.equals(group)) && ((id.name == null) || id.name.equals(name))
        && ((id.type == null) || id.type.equals(type)) && ((id.version == null) || id.version.equals(version));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoArtifactId that = (RepoArtifactId)o;

        if ((group != null) ? (!group.equals(that.group)) : (that.group != null)) {
            return false;
        }

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        if ((type != null) ? (!type.equals(that.type)) : (that.type != null)) {
            return false;
        }

        return !((version != null) ? (!version.equals(that.version)) : (that.version != null));
    }

    public int hashCode() {
        int result = 31 * ((group != null) ? group.hashCode() : 0);
        result = (31 * result) + ((name != null) ? name.hashCode() : 0);
        result = (31 * result) + ((type != null) ? type.hashCode() : 0);
        result = (31 * result) + ((version != null) ? version.hashCode() : 0);

        return result;
    }

    public Object clone() {
        try {
            RepoArtifactId clone = (RepoArtifactId)super.clone();
            clone.setAnnotations((Annotations)clone.getAnnotations().clone());

            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // Can't happen

            return null;
        }
    }

    public boolean isSnapShot() {
        return (version.getQualifier() != null) && version.getQualifier().endsWith("-ss");
    }
}
