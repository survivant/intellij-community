// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.maven.indices;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenRepositoryInfo;
import org.jetbrains.idea.maven.model.RepositoryKind;

public interface MavenRepositoryIndex {
  @ApiStatus.Internal
  @NotNull
  RepositoryKind getKind();

  @NotNull
  @ApiStatus.Internal
  MavenRepositoryInfo getRepository();
  void close(boolean releaseIndexContext);
}
