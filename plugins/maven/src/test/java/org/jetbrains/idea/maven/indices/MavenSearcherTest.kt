/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.idea.maven.indices

import org.junit.Test
import java.util.*

class MavenSearcherTest : MavenIndicesTestCase() {
  override fun runInDispatchThread() = true

  private val JUNIT_VERSIONS = arrayOf("junit:junit:4.0", "junit:junit:3.8.2", "junit:junit:3.8.1")
  private val JMOCK_VERSIONS = arrayOf("jmock:jmock:1.2.0", "jmock:jmock:1.1.0", "jmock:jmock:1.0.0")
  private val COMMONS_IO_VERSIONS = arrayOf("commons-io:commons-io:2.4")

  private lateinit var myIndicesFixture: MavenIndicesTestFixture

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myIndicesFixture = MavenIndicesTestFixture(dir.toPath(), project)
    myIndicesFixture.setUp()
    myIndicesFixture.indicesManager.scheduleUpdateContentLocalClassIndex(true)
    myIndicesFixture.indicesManager.waitForLuceneUpdateCompleted()
  }

  @Throws(Exception::class)
  override fun tearDown() {
    try {
      myIndicesFixture.tearDown()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun testClassSearch() {
    assertClassSearchResults("TestCas",
                             "TestCase(junit.framework) junit:junit:4.0 junit:junit:3.8.2 junit:junit:3.8.1",
                             "TestCaseClassLoader(junit.runner) junit:junit:3.8.2 junit:junit:3.8.1")
    assertClassSearchResults("TESTcase",
                             "TestCase(junit.framework) junit:junit:4.0 junit:junit:3.8.2 junit:junit:3.8.1",
                             "TestCaseClassLoader(junit.runner) junit:junit:3.8.2 junit:junit:3.8.1")

    assertClassSearchResults("After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0")
    assertClassSearchResults("After ",
                             "After(org.junit) junit:junit:4.0")
    assertClassSearchResults("*After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0",
                             "BeforeAndAfterRunner(org.junit.internal.runners) junit:junit:4.0",
                             "InvokedAfterMatcher(org.jmock.core.matcher) jmock:jmock:1.2.0 jmock:jmock:1.1.0 jmock:jmock:1.0.0")

    // do not include package hits 
    assertClassSearchResults("JUnit",
                             "JUnit4TestAdapter(junit.framework) junit:junit:4.0",
                             "JUnit4TestAdapterCache(junit.framework) junit:junit:4.0",
                             "JUnit4TestCaseFacade(junit.framework) junit:junit:4.0",
                             "JUnitCore(org.junit.runner) junit:junit:4.0")

    assertClassSearchResults("org.junit.After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0")

    assertClassSearchResults("org.After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0")

    assertClassSearchResults("junit.After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0")

    assertClassSearchResults("or.jun.After",
                             "After(org.junit) junit:junit:4.0",
                             "AfterClass(org.junit) junit:junit:4.0")

    // do not include other packages
    assertClassSearchResults("junit.framework.Test ",
                             "Test(junit.framework) junit:junit:4.0 junit:junit:3.8.2 junit:junit:3.8.1")

    assertClassSearchResults("!@][#$%)(^&*()_") // shouldn't throw
  }

  @Test
  fun testArtifactSearch() {
    if (ignore()) return
    assertArtifactSearchResults("")
    assertArtifactSearchResults("j:j", *(JMOCK_VERSIONS + JUNIT_VERSIONS))
    assertArtifactSearchResults("junit", *JUNIT_VERSIONS)
    assertArtifactSearchResults("junit 3.", *JUNIT_VERSIONS)
    assertArtifactSearchResults("uni 3.")
    assertArtifactSearchResults("juni juni 3.")
    assertArtifactSearchResults("junit foo", *JUNIT_VERSIONS)
    assertArtifactSearchResults("juni:juni:3.", *JUNIT_VERSIONS)
    assertArtifactSearchResults("junit:", *JUNIT_VERSIONS)
    assertArtifactSearchResults("junit:junit", *JUNIT_VERSIONS)
    assertArtifactSearchResults("junit:junit:3.", *JUNIT_VERSIONS)
    assertArtifactSearchResults("junit:junit:4.0", *JUNIT_VERSIONS)
  }

  @Test
  fun testArtifactSearchDash() {
    if (ignore()) return
    assertArtifactSearchResults("commons", *COMMONS_IO_VERSIONS)
    assertArtifactSearchResults("commons-", *COMMONS_IO_VERSIONS)
    assertArtifactSearchResults("commons-io", *COMMONS_IO_VERSIONS)
  }

  private fun assertClassSearchResults(pattern: String, vararg expected: String) {
    assertOrderedElementsAreEqual(getClassSearchResults(pattern), *expected)
  }

  private fun getClassSearchResults(pattern: String): List<String> {
    val actualArtifacts: MutableList<String> = ArrayList()
    for (eachResult in MavenClassSearcher().search(project, pattern, 100)) {
      val s = StringBuilder(eachResult.className + "(" + eachResult.packageName + ")")
      for (eachVersion in eachResult.searchResults.items) {
        if (s.length > 0) s.append(" ")
        s.append(eachVersion.groupId).append(":").append(eachVersion.artifactId).append(":").append(eachVersion.version)
      }
      actualArtifacts.add(s.toString())
    }
    return actualArtifacts
  }

  private fun assertArtifactSearchResults(pattern: String, vararg expected: String) {
    val actual: MutableList<String> = ArrayList()
    var s: StringBuilder
    for (eachResult in MavenArtifactSearcher().search(project, pattern, 100)) {
      for (eachVersion in eachResult.searchResults.items) {
        s = StringBuilder()
        s.append(eachVersion.groupId).append(":").append(eachVersion.artifactId).append(":").append(eachVersion.version)
        actual.add(s.toString())
      }
    }
    assertUnorderedElementsAreEqual(actual, *expected)
  }
}
