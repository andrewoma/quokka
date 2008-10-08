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


package ws.quokka.core.version;


/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/Version.java,v 1.17 2007/02/20 00:07:22 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2007). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import ws.quokka.core.bootstrap_util.Assert;

import java.util.StringTokenizer;


/**
 * Version identifier for artifacts.
 * <p/>
 * <p/>
 * Version identifiers the following components.
 * <ol>
 * <li>Major version. A non-negative integer.</li>
 * <li>Minor version. A non-negative integer.</li>
 * <li>Micro version. A non-negative integer.</li>
 * <li>Update version. A non-negative integer.</li>
 * <li>Qualifier. A text string. See <code>Version(String)</code> for the
 * format of the qualifier string.</li>
 * <li>Repository version. A non-negative integer.</li>
 * </ol>
 * <p/>
 * <p/>
 * <code>Version</code> objects are immutable.
 * <p/>
 * <p/>
 * Modified by Andrew O'Malley to include the update field which corresponds to the JSR-277 early draft.
 * Also modify to use the JSR-277 '-qualifier' grammar. This is much nicer than the OSGi version that
 * requires all parts to be specified for a qualifier to be used. e.g. 1.0-qualifier vs. 1.0.0.0.qualifier.
 * A repository version has also been added that allows the metadata in the repository to be updated
 * independently of the release of the software.
 * <p/>
 * The class also accepts non-standard version strings. Such strings cannot be compared, only equated.
 * <p/>
 * NOTE: Qualifier handling is different in OSGi vs JSR 277. Qualified versions are greater than unqualified
 * in OSGi, but are less than in 277. This takes the 277 approach. Unlike 277, the underscore character
 * is not permitted anywhere within the version.
 */
public class Version implements Comparable {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String SEPARATOR = "."; //$NON-NLS-1$

    /**
     * The empty version "0.0.0.0". Equivalent to calling
     * <code>new Version(0,0,0,0)</code>.
     */
    public static final Version EMPTY_VERSION = new Version(0, 0, 0, 0);

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private int major;
    private int minor;
    private int micro;
    private int update;
    private String qualifier;
    private int repositoryVersion;
    private String nonStandardString;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a version identifier from the specified numerical components.
     * <p/>
     * <p/>
     * The qualifier is set to the empty string.
     *
     * @param major Major component of the version identifier.
     * @param minor Minor component of the version identifier.
     * @param micro Micro component of the version identifier.
     * @throws IllegalArgumentException If the numerical components are
     *                                  negative.
     */
    public Version(int major, int minor, int micro, int update) {
        this(major, minor, micro, update, null);
    }

    /**
     * Creates a version identifier from the specifed components.
     *
     * @param major     Major component of the version identifier.
     * @param minor     Minor component of the version identifier.
     * @param micro     Micro component of the version identifier.
     * @param qualifier Qualifier component of the version identifier. If
     *                  <code>null</code> is specified, then the qualifier will be set
     *                  to the empty string.
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    public Version(int major, int minor, int micro, int update, String qualifier, int repositoryVersion) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.update = update;
        this.qualifier = qualifier;
        this.repositoryVersion = repositoryVersion;
        validate();
    }

    public Version(int major, int minor, int micro, int update, String qualifier) {
        this(major, minor, micro, update, qualifier, 0);
    }

    /**
     * Created a version identifier from the specified string.
     * <p/>
     * <p/>
     * Here is the grammar for version strings.
     * <p/>
     * <pre>
     * version ::= major('.'minor('.'micro('.'qualifier('~'repositoryVersion)?)?)?)?
     * major ::= digit+
     * minor ::= digit+
     * micro ::= digit+
     * update ::= digit+
     * qualifier ::= (alpha|digit|'-')+
     * repositoryVersion ::= digit+
     * digit ::= [0..9]
     * alpha ::= [a..zA..Z]
     * </pre>
     * <p/>
     * There must be no whitespace in version.
     *
     * @param version String representation of the version identifier.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public Version(String version) {
        Assert.isTrue(version != null, "version is null");

        String original = version;

        try {
            // Strip off the repository version
            int index = version.lastIndexOf('~');

            if ((index > 0) && (index < (version.length() - 1))) {
                repositoryVersion = Integer.parseInt(version.substring(index + 1));
                version = version.substring(0, index);
                original = version; // Even if subsequent parsing fails, keep the repository version separate
            }

            // Strip off the qualifier
            index = version.indexOf('-');

            if ((index > 0) && (index < (version.length() - 1))) {
                qualifier = version.substring(index + 1);
                version = version.substring(0, index);
            }

            StringTokenizer st = new StringTokenizer(version, SEPARATOR);
            major = Integer.parseInt(st.nextToken());
            minor = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
            micro = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
            update = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

            if (st.hasMoreTokens()) {
                throw new IllegalArgumentException();
            }

            validate();
        } catch (Exception e) {
            int length = original.length();

            for (int i = 0; i < length; i++) {
                if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-".indexOf(original.charAt(i)) == -1) {
                    throw new IllegalArgumentException("invalid characters in version: " + original);
                }
            }

            nonStandardString = original;
            major = 0;
            minor = 0;
            micro = 0;
            update = 0;
            qualifier = null;
        }
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public int getRepositoryVersion() {
        return repositoryVersion;
    }

    public boolean isStandard() {
        return nonStandardString == null;
    }

    /**
     * Called by the Version constructors to validate the version components.
     *
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    private void validate() {
        if (major < 0) {
            throw new IllegalArgumentException("negative major"); //$NON-NLS-1$
        }

        if (minor < 0) {
            throw new IllegalArgumentException("negative minor"); //$NON-NLS-1$
        }

        if (micro < 0) {
            throw new IllegalArgumentException("negative micro"); //$NON-NLS-1$
        }

        if (update < 0) {
            throw new IllegalArgumentException("negative update"); //$NON-NLS-1$
        }

        if (repositoryVersion < 0) {
            throw new IllegalArgumentException("negative update"); //$NON-NLS-1$
        }

        if (qualifier != null) {
            int length = qualifier.length();

            for (int i = 0; i < length; i++) {
                if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-".indexOf(qualifier.charAt(i)) == -1) { //$NON-NLS-1$
                    throw new IllegalArgumentException("invalid qualifier"); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Parses a version identifier from the specified string.
     * <p/>
     * <p/>
     * See <code>Version(String)</code> for the format of the version string.
     *
     * @param version String representation of the version identifier. Leading
     *                and trailing whitespace will be ignored.
     * @return A <code>Version</code> object representing the version
     *         identifier. If <code>version</code> is <code>null</code> or
     *         the empty string then <code>EMPTY_VERSION</code> will be
     *         returned.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public static Version parse(String version) {
        if (version == null) {
            return EMPTY_VERSION;
        }

        version = version.trim();

        if (version.length() == 0) {
            return EMPTY_VERSION;
        }

        return new Version(version);
    }

    /**
     * Returns the major component of this version identifier.
     *
     * @return The major component.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor component of this version identifier.
     *
     * @return The minor component.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the micro component of this version identifier.
     *
     * @return The micro component.
     */
    public int getMicro() {
        return micro;
    }

    public int getUpdate() {
        return update;
    }

    /**
     * Returns the qualifier component of this version identifier.
     *
     * @return The qualifier component.
     */
    public String getQualifier() {
        return qualifier;
    }

    public String getNonStandardString() {
        return nonStandardString;
    }

    /**
     * Returns the string representation of this version identifier.
     * <p/>
     * <p/>
     * The format of the version string will be <code>major.minor.micro</code>
     * if qualifier is the empty string or
     * <code>major.minor.micro.qualifier</code> otherwise.
     *
     * @return The string representation of this version identifier.
     */
    public String toString() {
        if (nonStandardString != null) {
            return nonStandardString + ((repositoryVersion == 0) ? "" : ("~" + repositoryVersion));
        }

        return major + SEPARATOR + minor + (((micro == 0) && (update == 0)) ? "" : (SEPARATOR + micro))
        + ((update == 0) ? "" : (SEPARATOR + update)) + ((qualifier == null) ? "" : ("-" + qualifier))
        + ((repositoryVersion == 0) ? "" : ("~" + repositoryVersion));
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return An integer which is a hash code value for this object.
     */
    public int hashCode() {
        if (nonStandardString != null) {
            return (nonStandardString.hashCode() * 31) + repositoryVersion;
        }

        int result;
        result = major;
        result = (31 * result) + minor;
        result = (31 * result) + micro;
        result = (31 * result) + update;
        result = (31 * result) + ((qualifier != null) ? qualifier.hashCode() : 0);
        result = (31 * result) + repositoryVersion;

        return result;
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <p/>
     * <p/>
     * A version is considered to be <b>equal to </b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.equals</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return <code>true</code> if <code>object</code> is a
     *         <code>Version</code> and is equal to this object;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        if (object == this) { // quicktest

            return true;
        }

        if (!(object instanceof Version)) {
            return false;
        }

        Version other = (Version)object;

        if (nonStandardString != null) {
            return nonStandardString.equals(other.nonStandardString) && (repositoryVersion == other.repositoryVersion);
        }

        boolean qualifierEqual = ((qualifier == null) && (other.qualifier == null))
            || ((qualifier != null) && (other.qualifier != null) && qualifier.equals(other.qualifier));

        return (major == other.major) && (minor == other.minor) && (micro == other.micro) && (update == other.update)
        && qualifierEqual && (repositoryVersion == other.repositoryVersion);
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <p/>
     * <p/>
     * A version is considered to be <b>less than </b> another version if its
     * major component is less than the other version's major component, or the
     * major components are equal and its minor component is less than the other
     * version's minor component, or the major and minor components are equal
     * and its micro component is less than the other version's micro component,
     * or the major, minor and micro components are equal and it's qualifier
     * component is less than the other version's qualifier component (using
     * <code>String.compareTo</code>).
     * <p/>
     * <p/>
     * A version is considered to be <b>equal to</b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.compareTo</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return A negative integer, zero, or a positive integer if this object is
     *         less than, equal to, or greater than the specified
     *         <code>Version</code> object.
     * @throws ClassCastException If the specified object is not a
     *                            <code>Version</code>.
     */
    public int compareTo(Object object) {
        if (object == this) { // quicktest

            return 0;
        }

        Version other = (Version)object;

        // Treat non-standard strings lesser than anything with a standard version
        if ((nonStandardString == null) && (other.nonStandardString != null)) {
            return 1;
        }

        if ((nonStandardString != null) && (other.nonStandardString == null)) {
            return -1;
        }

        if ((nonStandardString != null) && (other.nonStandardString != null)) {
            int result = nonStandardString.compareTo(other.nonStandardString);

            if (result != 0) {
                return result;
            }

            return repositoryVersion - other.repositoryVersion;
        }

        int result = major - other.major;

        if (result != 0) {
            return result;
        }

        result = minor - other.minor;

        if (result != 0) {
            return result;
        }

        result = micro - other.micro;

        if (result != 0) {
            return result;
        }

        result = update - other.update;

        if (result != 0) {
            return result;
        }

        if (!((qualifier == null) && (other.qualifier == null))) {
            // NOTE: Unqualfied is greater than qualified as per 277. This is opposite to OSGi.
            if (qualifier == null) {
                return 1;
            }

            if (other.qualifier == null) {
                return -1;
            }

            result = qualifier.compareTo(other.qualifier);

            if (result != 0) {
                return result;
            }
        }

        return repositoryVersion - other.repositoryVersion;
    }

    public boolean isSnapShot() {
        if (qualifier == null) {
            return false;
        }

        String qualifierL = qualifier.toLowerCase();

        return qualifierL.equals("ss") || qualifierL.endsWith("-ss");
    }
}
