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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.StringGenerator;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;


/**
 *
 */
public class RepoArtifactId extends AnnotatedObject implements Cloneable, Comparable {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String ID_SEPARATOR = ":";
    public static final String PATH_SEPARATOR = "_";
    private static final String VALID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-";

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
        validate();
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
        return _merge(new RepoArtifactId(group, defaultName(group), "jar", version));
    }

    public static String defaultName(String group) {
        if (group == null) {
            return null;
        }

        String[] tokens = Strings.split(group, ".");

        return tokens[tokens.length - 1];
    }

    private String applyDefault(String value, String defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    private Version applyDefault(Version value, Version defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    public void validate() {
        validate(group);
        validate(name);
        validate(type);
    }

    private void validate(String string) {
        if (string != null) {
            int length = string.length();

            for (int i = 0; i < length; i++) {
                Assert.isTrue(VALID_CHARS.indexOf(string.charAt(i)) != -1, this.getLocator(),
                    "There are invalid characters in the id: " + string);
            }
        }
    }

    public void validateComplete() {
        Assert.isTrue(!Strings.isBlank(group) && !Strings.isBlank(name) && !Strings.isBlank(type) && (version != null),
            "Id is not valid: " + toShortString());
    }

    public String toShortString() {
        return noNull(group) + ID_SEPARATOR + noNull(name) + ID_SEPARATOR + noNull(type) + ID_SEPARATOR
        + noNull(version);
    }

    public String toPathString() {
        return noNull(group) + PATH_SEPARATOR + noNull(name) + PATH_SEPARATOR + noNull(type) + PATH_SEPARATOR
        + ((version == null) ? "" : version.toString());
    }

    public static RepoArtifactId parse(String idString) {
        String[] tokens = Strings.trim(Strings.splitPreserveAllTokens(idString, ID_SEPARATOR));
        Assert.isTrue(tokens.length <= 4,
            "id string is not is the format 'group" + ID_SEPARATOR + "name" + ID_SEPARATOR + "type" + ID_SEPARATOR
            + "version. value is " + idString);

        int len = tokens.length;

        return new RepoArtifactId((len >= 1) ? toNull(tokens[0]) : null, (len >= 2) ? toNull(tokens[1]) : null,
            (len >= 3) ? toNull(tokens[2]) : null, (len >= 4) ? new Version(tokens[3]) : null);
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

    public int compareTo(Object o) {
        RepoArtifactId other = (RepoArtifactId)o;
        int result = nullSafeCompare(group, other.group);
        result = (result == 0) ? nullSafeCompare(version, other.version) : result;
        result = (result == 0) ? nullSafeCompare(name, other.name) : result;
        result = (result == 0) ? nullSafeCompare(type, other.type) : result;

        return result;
    }

    private int nullSafeCompare(Comparable lhs, Comparable rhs) {
        if ((lhs == null) && (rhs == null)) {
            return 0;
        } else if (lhs == null) {
            return -1;
        } else if (rhs == null) {
            return 1;
        } else {
            return lhs.compareTo(rhs);
        }
    }
}
