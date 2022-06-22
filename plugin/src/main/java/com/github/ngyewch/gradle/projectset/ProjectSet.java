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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

  public Set<Project> getResolvedProjects(Project project) {
    return getResolvedProjects(project, JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
  }

  public Set<Project> getResolvedProjects(Project project, String configurationName) {
    return getResolvedProjectPaths(configurationName).stream()
        .map(project::project)
        .collect(Collectors.toSet());
  }

  public Set<String> getResolvedProjectPaths() {
    return getResolvedProjectPaths(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
  }

  public Set<String> getResolvedProjectPaths(String configurationName) {
    final Set<String> paths = new HashSet<>();
    getProjects().get().stream().map(Project::getPath).forEach(paths::add);
    getProjects().get().stream()
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
        })
        .forEach(paths::add);
    return paths;
  }
}
