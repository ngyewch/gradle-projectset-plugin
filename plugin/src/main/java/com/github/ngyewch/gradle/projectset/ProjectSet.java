package com.github.ngyewch.gradle.projectset;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
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
    return new ProjectPathCollector().collect(configurationName);
  }

  private class ProjectPathCollector {

    private final Set<String> projectPaths = new HashSet<>();

    public Set<String> collect(String configurationName) {
      for (final Project p : getProjects().get()) {
        collect(p, configurationName);
      }
      return projectPaths;
    }

    private void collect(Project p, String configurationName) {
      if (!projectPaths.add(p.getPath())) {
        return;
      }
      final Optional<Configuration> configurationOptional = p.getConfigurations().stream()
          .filter(c -> c.getName().equals(configurationName))
          .findFirst();
      if (!configurationOptional.isPresent()) {
        return;
      }
      final Configuration configuration = configurationOptional.get();
      for (final ResolvedArtifact resolvedArtifact : configuration.getResolvedConfiguration().getResolvedArtifacts()) {
        if (resolvedArtifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
          final ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier) resolvedArtifact
              .getId().getComponentIdentifier();
          final Project p2 = p.findProject(projectComponentIdentifier.getProjectPath());
          if (p2 != null) {
            collect(p2, configurationName);
          }
        }
      }
    }
  }
}
