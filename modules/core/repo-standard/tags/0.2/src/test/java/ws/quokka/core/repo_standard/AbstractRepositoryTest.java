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


package ws.quokka.core.repo_standard;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.AnnotatedProperties;


/**
 *
 */
public abstract class AbstractRepositoryTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    protected Repository repository;
    protected String name;
    protected String className;
    protected AnnotatedProperties properties = new AnnotatedProperties();
    protected RepositoryFactoryImpl factory = new RepositoryFactoryImpl();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void initialise() {
        Project project = new Project();
        project.setDefaultInputStream(System.in);

        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);
        project.init();

        put("class", (className == null) ? name : className);
        factory.setProject(project);
        factory.setProperties(properties);
        factory.registerType(new RepoType("jar", ".jar file", "jar"));
        factory.registerType(new RepoType("paths", "Repository file", "xml"));
        repository = factory.getOrCreate(name, true);
    }

    protected void put(String key, String value) {
        properties.put(AbstractStandardRepository.PREFIX + name + "." + key, value);
    }

    protected void put(String name, String key, String value) {
        properties.put(AbstractStandardRepository.PREFIX + name + "." + key, value);
    }

    protected RepoArtifact resolveArtifact(RepoArtifactId id, int numDependencies) {
        RepoArtifact artifact = repository.resolve(id);
        assertEquals(numDependencies, artifact.getDependencies().size());

        return artifact;
    }

    protected void remove(RepoArtifactId id) {
        try {
            repository.remove(id);
        } catch (Exception e) {
            System.out.println(e.getMessage());

            // ignore
        }
    }
}
