// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.findUsages

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesTest.Companion.FindUsageTestType
import org.jetbrains.kotlin.idea.base.test.InTextDirectivesUtils
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractFindUsagesMultiModuleTest : AbstractMultiModuleTest() {
    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("multiModuleFindUsages")

    protected val mainFile: KtFile
        get() = project.allKotlinFiles().single { file ->
            file.text.contains("// ")
        }

    protected open fun doFindUsagesTest() {
        val virtualFile = mainFile.virtualFile!!
        configureByExistingFile(virtualFile)

        val mainFileName = mainFile.name
        val mainFileText = mainFile.text
        val prefix = mainFileName.substringBefore(".") + "."

        val caretElementClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, "// PSI_ELEMENT: ")

        @Suppress("UNCHECKED_CAST")
        val caretElementClass = Class.forName(caretElementClassNames.single()) as Class<out KtDeclaration>

        val parser = OptionsParser.getParserByPsiElementClass(caretElementClass)

        val rootPath = virtualFile.path.substringBeforeLast("/") + "/"

        val caretElement = executeOnPooledThreadInReadAction {
            TargetElementUtil.findTargetElement(myEditor, TargetElementUtil.ELEMENT_NAME_ACCEPTED)
        }
        UsefulTestCase.assertInstanceOf(caretElement!!, caretElementClass)

        val options = parser?.parse(mainFileText, project)
        findUsagesAndCheckResults(
            mainFileText,
            prefix,
            rootPath,
            caretElement,
            options,
            project,
            alwaysAppendFileName = true,
            testType = if (isFirPlugin()) FindUsageTestType.FIR else FindUsageTestType.DEFAULT,
        )
    }
}