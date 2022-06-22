package com.github.ngyewch.gradle.projectset;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class JavadocConfiguration {

  public abstract Property<String> getTitle();

  public abstract ListProperty<Project> getExcludeProjects();

  public abstract Property<Boolean> getUseJavadocIo();

  public abstract ListProperty<String> getExcludeLinksForDependencies();
}
