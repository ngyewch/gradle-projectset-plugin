package com.github.ngyewch.gradle.projectset;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ProjectSet {

  private final JavadocConfiguration javadocConfiguration;

  @Inject
  public ProjectSet(ObjectFactory objectFactory) {
    super();

    javadocConfiguration = objectFactory.newInstance(JavadocConfiguration.class);
  }

  public abstract Property<String> getId();

  public abstract ListProperty<Project> getProjects();

  public JavadocConfiguration getJavadoc() {
    return javadocConfiguration;
  }

  public void javadoc(Action<JavadocConfiguration> action) {
    action.execute(javadocConfiguration);
  }

  public String getCapitalizedId() {
    return StringUtils.capitalize(getId().getOrNull());
  }

  public Stream<Project> getResolvedProjects(Project project) {
    return getResolvedProjects(project, JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
  }

  public Stream<Project> getResolvedProjects(Project project, String configurationName) {
    return getResolvedProjectPaths(configurationName).map(project::project);
  }

  public Stream<String> getResolvedProjectPaths() {
    return getResolvedProjectPaths(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
  }

  public Stream<String> getResolvedProjectPaths(String configurationName) {
    return getProjects().get().stream()
        .map(project -> project.getConfigurations().getByName(configurationName)
            .getResolvedConfiguration().getResolvedArtifacts())
        .flatMap(Collection::stream)
        .collect(Collectors.toSet()).stream()
        .filter(resolvedArtifact ->
            resolvedArtifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier)
        .map(resolvedArtifact -> {
          final ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier) resolvedArtifact
              .getId().getComponentIdentifier();
          return projectComponentIdentifier.getProjectPath();
        });
  }
}
