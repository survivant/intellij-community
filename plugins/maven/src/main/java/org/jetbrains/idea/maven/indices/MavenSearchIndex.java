/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.maven.indices;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenRepositoryInfo;
import org.jetbrains.idea.maven.model.RepositoryKind;

import java.io.File;
import java.util.Set;

public interface MavenSearchIndex extends MavenRepositoryIndex {

  @Topic.AppLevel
  Topic<IndexListener> INDEX_IS_BROKEN =
    new Topic<>("Maven Index Broken Listener", IndexListener.class);

  @NlsSafe
  String getRepositoryId();

  File getRepositoryFile();

  @NotNull
  MavenRepositoryInfo getRepository();

  @NlsSafe
  String getRepositoryUrl();

  @NlsSafe
  String getRepositoryPathOrUrl();

  RepositoryKind getKind();

  long getUpdateTimestamp();

  @NlsSafe
  String getFailureMessage();

  Set<MavenArtifactInfo> search(String pattern, int maxResult);

  interface IndexListener {
    void indexIsBroken(@NotNull MavenSearchIndex index);
  }
}
