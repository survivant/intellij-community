// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("j2k/new/tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("testData/partialConverter")
public abstract class PartialConverterTestGenerated extends AbstractPartialConverterTest {
    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/partialConverter/field")
    public static class Field extends AbstractPartialConverterTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("classChildExtendsBase.java")
        public void testClassChildExtendsBase() throws Exception {
            runTest("testData/partialConverter/field/classChildExtendsBase.java");
        }

        @TestMetadata("conversion.java")
        public void testConversion() throws Exception {
            runTest("testData/partialConverter/field/conversion.java");
        }

        @TestMetadata("internalField.java")
        public void testInternalField() throws Exception {
            runTest("testData/partialConverter/field/internalField.java");
        }

        @TestMetadata("needInitializer.java")
        public void testNeedInitializer() throws Exception {
            runTest("testData/partialConverter/field/needInitializer.java");
        }

        @TestMetadata("privateField.java")
        public void testPrivateField() throws Exception {
            runTest("testData/partialConverter/field/privateField.java");
        }

        @TestMetadata("protectedField.java")
        public void testProtectedField() throws Exception {
            runTest("testData/partialConverter/field/protectedField.java");
        }

        @TestMetadata("publicField.java")
        public void testPublicField() throws Exception {
            runTest("testData/partialConverter/field/publicField.java");
        }

        @TestMetadata("specifyType.java")
        public void testSpecifyType() throws Exception {
            runTest("testData/partialConverter/field/specifyType.java");
        }

        @TestMetadata("valOrVar.java")
        public void testValOrVar() throws Exception {
            runTest("testData/partialConverter/field/valOrVar.java");
        }

        @TestMetadata("valWithInit.java")
        public void testValWithInit() throws Exception {
            runTest("testData/partialConverter/field/valWithInit.java");
        }

        @TestMetadata("varWithInit.java")
        public void testVarWithInit() throws Exception {
            runTest("testData/partialConverter/field/varWithInit.java");
        }

        @TestMetadata("varWithoutInit.java")
        public void testVarWithoutInit() throws Exception {
            runTest("testData/partialConverter/field/varWithoutInit.java");
        }

        @TestMetadata("volatileTransientAndStrictFp.java")
        public void testVolatileTransientAndStrictFp() throws Exception {
            runTest("testData/partialConverter/field/volatileTransientAndStrictFp.java");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/partialConverter/function")
    public static class Function extends AbstractPartialConverterTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("extendsBaseWhichExtendsObject.java")
        public void testExtendsBaseWhichExtendsObject() throws Exception {
            runTest("testData/partialConverter/function/extendsBaseWhichExtendsObject.java");
        }

        @TestMetadata("externalFunctionalInterface.java")
        public void testExternalFunctionalInterface() throws Exception {
            runTest("testData/partialConverter/function/externalFunctionalInterface.java");
        }

        @TestMetadata("externalKtFunctionalInterface.java")
        public void testExternalKtFunctionalInterface() throws Exception {
            runTest("testData/partialConverter/function/externalKtFunctionalInterface.java");
        }

        @TestMetadata("final.java")
        public void testFinal() throws Exception {
            runTest("testData/partialConverter/function/final.java");
        }

        @TestMetadata("functionInFinalClass.java")
        public void testFunctionInFinalClass() throws Exception {
            runTest("testData/partialConverter/function/functionInFinalClass.java");
        }

        @TestMetadata("internal.java")
        public void testInternal() throws Exception {
            runTest("testData/partialConverter/function/internal.java");
        }

        @TestMetadata("internalFunctionalInterface.java")
        public void testInternalFunctionalInterface() throws Exception {
            runTest("testData/partialConverter/function/internalFunctionalInterface.java");
        }

        @TestMetadata("java8Lambdas.java")
        public void testJava8Lambdas() throws Exception {
            runTest("testData/partialConverter/function/java8Lambdas.java");
        }

        @TestMetadata("java8MRKFunctionExpectedType.java")
        public void testJava8MRKFunctionExpectedType() throws Exception {
            runTest("testData/partialConverter/function/java8MRKFunctionExpectedType.java");
        }

        @TestMetadata("java8MRSamConstructor.java")
        public void testJava8MRSamConstructor() throws Exception {
            runTest("testData/partialConverter/function/java8MRSamConstructor.java");
        }

        @TestMetadata("lineBreaksBetweenParameters.java")
        public void testLineBreaksBetweenParameters() throws Exception {
            runTest("testData/partialConverter/function/lineBreaksBetweenParameters.java");
        }

        @TestMetadata("main.java")
        public void testMain() throws Exception {
            runTest("testData/partialConverter/function/main.java");
        }

        @TestMetadata("main2.java")
        public void testMain2() throws Exception {
            runTest("testData/partialConverter/function/main2.java");
        }

        @TestMetadata("mainAndNullabilitySetting.java")
        public void testMainAndNullabilitySetting() throws Exception {
            runTest("testData/partialConverter/function/mainAndNullabilitySetting.java");
        }

        @TestMetadata("mainVararg.java")
        public void testMainVararg() throws Exception {
            runTest("testData/partialConverter/function/mainVararg.java");
        }

        @TestMetadata("methodClassType.java")
        public void testMethodClassType() throws Exception {
            runTest("testData/partialConverter/function/methodClassType.java");
        }

        @TestMetadata("methodWithReturnStatement.java")
        public void testMethodWithReturnStatement() throws Exception {
            runTest("testData/partialConverter/function/methodWithReturnStatement.java");
        }

        @TestMetadata("nativeMethods.java")
        public void testNativeMethods() throws Exception {
            runTest("testData/partialConverter/function/nativeMethods.java");
        }

        @TestMetadata("open.java")
        public void testOpen() throws Exception {
            runTest("testData/partialConverter/function/open.java");
        }

        @TestMetadata("override.java")
        public void testOverride() throws Exception {
            runTest("testData/partialConverter/function/override.java");
        }

        @TestMetadata("overrideAndOpen.java")
        public void testOverrideAndOpen() throws Exception {
            runTest("testData/partialConverter/function/overrideAndOpen.java");
        }

        @TestMetadata("overrideObject.java")
        public void testOverrideObject() throws Exception {
            runTest("testData/partialConverter/function/overrideObject.java");
        }

        @TestMetadata("overrideObject2.java")
        public void testOverrideObject2() throws Exception {
            runTest("testData/partialConverter/function/overrideObject2.java");
        }

        @TestMetadata("overrideObject3.java")
        public void testOverrideObject3() throws Exception {
            runTest("testData/partialConverter/function/overrideObject3.java");
        }

        @TestMetadata("overrideWithHigherVisibility.java")
        public void testOverrideWithHigherVisibility() throws Exception {
            runTest("testData/partialConverter/function/overrideWithHigherVisibility.java");
        }

        @TestMetadata("ownGenericParam.java")
        public void testOwnGenericParam() throws Exception {
            runTest("testData/partialConverter/function/ownGenericParam.java");
        }

        @TestMetadata("ownSeveralGenericParams.java")
        public void testOwnSeveralGenericParams() throws Exception {
            runTest("testData/partialConverter/function/ownSeveralGenericParams.java");
        }

        @TestMetadata("parameterModification.java")
        public void testParameterModification() throws Exception {
            runTest("testData/partialConverter/function/parameterModification.java");
        }

        @TestMetadata("private.java")
        public void testPrivate() throws Exception {
            runTest("testData/partialConverter/function/private.java");
        }

        @TestMetadata("protected.java")
        public void testProtected() throws Exception {
            runTest("testData/partialConverter/function/protected.java");
        }

        @TestMetadata("public.java")
        public void testPublic() throws Exception {
            runTest("testData/partialConverter/function/public.java");
        }

        @TestMetadata("referenceToConstructor.java")
        public void testReferenceToConstructor() throws Exception {
            runTest("testData/partialConverter/function/referenceToConstructor.java");
        }

        @TestMetadata("synchronizedMethod.java")
        public void testSynchronizedMethod() throws Exception {
            runTest("testData/partialConverter/function/synchronizedMethod.java");
        }

        @TestMetadata("varVararg.java")
        public void testVarVararg() throws Exception {
            runTest("testData/partialConverter/function/varVararg.java");
        }
    }
}
