package org.openrewrite.java.dropwizard.annotation.security;

import org.openrewrite.config.CompositeRecipe;

import java.util.Arrays;

public class SecurityAnnotationTransformer extends CompositeRecipe {

  public SecurityAnnotationTransformer() {
    super(Arrays.asList(new RolesToPreAuthorize(), new PermitAllToPreAuthorizeTransformer()));
  }
}
