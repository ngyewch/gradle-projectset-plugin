package com.github.ngyewch.gradle.projectset;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;

public abstract class ProjectSetExtension {

  private final ObjectFactory objectFactory;

  @Inject
  public ProjectSetExtension(ObjectFactory objectFactory) {
    super();

    this.objectFactory = objectFactory;
  }

  public abstract ListProperty<ProjectSet> getProjectSets();

  public void projectSet(Action<ProjectSet> action) {
    final ProjectSet projectSet = objectFactory.newInstance(ProjectSet.class);
    action.execute(projectSet);
    getProjectSets().add(projectSet);
  }
}
