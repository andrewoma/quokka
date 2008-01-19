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

import org.apache.tools.ant.BuildException;


/**
 * Source taken from Apache Felix 1.0 release, variable names changed to match conventions
 */
public class VersionRange {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final VersionRange INFINITE_RANGE = new VersionRange(Version.EMPTY_VERSION, true, null, true);

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Version low = null;
    private boolean isLowInclusive = false;
    private Version high = null;
    private boolean isHighInclusive = false;
    private String toString = null;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public VersionRange(Version low, boolean isLowInclusive, Version high, boolean isHighInclusive) {
        this.low = low;
        this.isLowInclusive = isLowInclusive;
        this.high = high;
        this.isHighInclusive = isHighInclusive;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Version getLow() {
        return low;
    }

    public boolean isLowInclusive() {
        return isLowInclusive;
    }

    public Version getHigh() {
        return high;
    }

    public boolean isHighInclusive() {
        return isHighInclusive;
    }

    public boolean isInRange(Version version) {
        // The only non-standard version that can be in range is an exact match
        if (!low.isStandard()) {
            return low.equals(version);
        }

        // We might not have an upper end to the range.
        if (high == null) {
            return (version.compareTo(low) >= 0);
        } else if (isLowInclusive() && isHighInclusive()) {
            return (version.compareTo(low) >= 0) && (version.compareTo(high) <= 0);
        } else if (isHighInclusive()) {
            return (version.compareTo(low) > 0) && (version.compareTo(high) <= 0);
        } else if (isLowInclusive()) {
            return (version.compareTo(low) >= 0) && (version.compareTo(high) < 0);
        }

        return (version.compareTo(low) > 0) && (version.compareTo(high) < 0);
    }

    public static VersionRange parse(String range) {
        VersionRange versionRange = parse_(range);

        if (!versionRange.getLow().isStandard()
                || ((versionRange.getHigh() != null) && !versionRange.getHigh().isStandard())) {
            if (versionRange.getHigh() != null) {
                throw new BuildException("Version ranges can only be used with standard version formats: " + range);
            }
        }

        return versionRange;
    }

    private static VersionRange parse_(String range) {
        // Check if the version is an interval.
        if (range.indexOf(',') >= 0) {
            String s = range.substring(1, range.length() - 1);
            String vlo = s.substring(0, s.indexOf(',')).trim();
            String vhi = s.substring(s.indexOf(',') + 1, s.length()).trim();

            return new VersionRange(new Version(vlo), (range.charAt(0) == '['), new Version(vhi),
                (range.charAt(range.length() - 1) == ']'));
        } else {
            return new VersionRange(new Version(range), true, null, false);
        }
    }

    public String toString() {
        if (toString == null) {
            if (high != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(isLowInclusive ? '[' : '(');
                sb.append(low.toString());
                sb.append(',');
                sb.append(high.toString());
                sb.append(isHighInclusive ? ']' : ')');
                toString = sb.toString();
            } else {
                toString = low.toString();
            }
        }

        return toString;
    }
}
