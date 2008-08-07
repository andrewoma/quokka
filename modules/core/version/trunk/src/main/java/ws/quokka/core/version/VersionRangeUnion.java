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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


/**
 *
 */
public class VersionRangeUnion {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    List ranges = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public boolean isInRange(Version version) {
        for (Iterator i = ranges.iterator(); i.hasNext();) {
            VersionRange range = (VersionRange) i.next();

            if (range.isInRange(version)) {
                return true;
            }
        }

        return false;
    }

    public void add(VersionRange range) {
        ranges.add(range);
    }

    public static VersionRangeUnion parse(String rangeUnion) {
        VersionRangeUnion versionRangeUnion = new VersionRangeUnion();

        StringTokenizer tokenzier = new StringTokenizer(rangeUnion, ";");

        while (tokenzier.hasMoreTokens()) {
            String range = tokenzier.nextToken().trim();

            if (range.length() > 0) {
                versionRangeUnion.add(VersionRange.parse(range));
            }
        }

        return versionRangeUnion;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Iterator i = ranges.iterator(); i.hasNext();) {
            VersionRange range = (VersionRange) i.next();
            sb.append(range.toString());

            if (i.hasNext()) {
                sb.append(";");
            }
        }

        return sb.toString();
    }

    public List getRanges() {
        return Collections.unmodifiableList(ranges);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionRangeUnion that = (VersionRangeUnion) o;

        if (ranges != null ? !ranges.equals(that.ranges) : that.ranges != null) return false;

        return true;
    }

    public int hashCode() {
        return (ranges != null ? ranges.hashCode() : 0);
    }
}
