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


/**
 *
 */
public class RepoPathSpec extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String from;
    private String to;
    private String options;
    private Boolean descend;
    private Boolean mandatory;
    private RepoDependency dependency;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoPathSpec() {
    }

    public RepoPathSpec(String fromPath, String toPath, String options, Boolean descend, Boolean mandatory) {
        this.from = fromPath;
        this.to = toPath;
        this.options = options;
        this.descend = descend;
        this.mandatory = mandatory;
    }

    public RepoPathSpec(String fromPath, String toPath, Boolean descend, Boolean mandatory) {
        this.from = fromPath;
        this.to = toPath;
        this.descend = descend;
        this.mandatory = mandatory;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public Boolean isDescend() {
        return descend;
    }

    public void setDescend(Boolean descend) {
        this.descend = descend;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    void setDependency(RepoDependency dependency) {
        this.dependency = dependency;
    }

    public RepoDependency getDependency() {
        return dependency;
    }

    public void mergeDefaults(RepoPath path) {
        // Fill in defaults if not specified
        from = (from == null) ? "runtime" : from;
        descend = (descend == null) ? ((path.isDescendDefault()) ? Boolean.TRUE : Boolean.FALSE) : descend;
        mandatory = (mandatory == null) ? ((path.isMandatoryDefault()) ? Boolean.TRUE : Boolean.FALSE) : mandatory;
    }

    public String toShortString() {
        return (mandatory.booleanValue() ? "" : "?")
        + (
            (mandatory.booleanValue() && from.equals("runtime") && descend.booleanValue()) ? ""
                                                                                           : (
                from + (descend.booleanValue() ? "" : "+") + " -> "
            )
        ) + to + ((options == null) ? "" : ("(" + options + ")"));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoPathSpec pathSpec = (RepoPathSpec)o;

        if ((dependency != null) ? (!dependency.getId().equals(pathSpec.dependency.getId())) : (
                    pathSpec.dependency != null
                )) {
            return false;
        }

        if ((descend != null) ? (!descend.equals(pathSpec.descend)) : (pathSpec.descend != null)) {
            return false;
        }

        if ((from != null) ? (!from.equals(pathSpec.from)) : (pathSpec.from != null)) {
            return false;
        }

        if ((mandatory != null) ? (!mandatory.equals(pathSpec.mandatory)) : (pathSpec.mandatory != null)) {
            return false;
        }

        if ((options != null) ? (!options.equals(pathSpec.options)) : (pathSpec.options != null)) {
            return false;
        }

        return !((to != null) ? (!to.equals(pathSpec.to)) : (pathSpec.to != null));
    }

    public int hashCode() {
        int result;
        result = ((from != null) ? from.hashCode() : 0);
        result = (31 * result) + ((to != null) ? to.hashCode() : 0);
        result = (31 * result) + ((options != null) ? options.hashCode() : 0);
        result = (31 * result) + ((descend != null) ? descend.hashCode() : 0);
        result = (31 * result) + ((mandatory != null) ? mandatory.hashCode() : 0);

        return result;
    }
}
