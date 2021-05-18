package com.r3.conclave.samples.tribuo.client

import com.r3.conclave.samples.tribuo.common.ClassEvaluationResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher

internal class ClassificationTestWatcher : TestWatcher {
    override fun testFailed(context: ExtensionContext?, cause: Throwable?) {
        ClassificationTest.classificationTeardown()
    }
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(ClassificationTestWatcher::class)
class ClassificationTest : TribuoTest() {
    companion object {
        private lateinit var classification: Classification
        private lateinit var enclaveIrisDataPath: String

        @BeforeAll
        @JvmStatic
        fun classificationSetup() {
            classification = Classification(client)
            enclaveIrisDataPath = classification.irisDataPath
        }

        @AfterAll
        @JvmStatic
        fun classificationTeardown() {
            classification.close()
        }
    }

    @Order(0)
    @Test
    fun trainAndEvaluate() {
        val evaluation = classification.trainAndEvaluate()
        val expectedResults = mapOf(
                "Iris-versicolor" to ClassEvaluationResult(16.0, 16.0, 0.0, 1.0, 1.0, 0.941, 0.97),
                "Iris-virginica" to ClassEvaluationResult(15.0, 14.0, 1.0, 0.0, 0.933, 1.0, 0.966),
                "Iris-setosa" to ClassEvaluationResult(14.0, 14.0, 0.0, 0.0, 1.0, 1.0, 1.0),
        )
        val offset = Offset.offset(0.001)
        assertThat(evaluation.classesEvaluationResults.size).isEqualTo(expectedResults.size)
        evaluation.classesEvaluationResults.forEach { (label, result) ->
            val expected = expectedResults[label]!!
            assertThat(result.n).isEqualTo(expected.n)
            assertThat(result.tp).isEqualTo(expected.tp)
            assertThat(result.fn).isEqualTo(expected.fn)
            assertThat(result.fp).isEqualTo(expected.fp)
            assertThat(result.recall).isEqualTo(expected.recall, offset)
            assertThat(result.precision).isEqualTo(expected.precision, offset)
            assertThat(result.f1).isEqualTo(expected.f1, offset)
        }
        assertThat(evaluation.accuracy).isEqualTo(0.978, offset)

        assertThat(evaluation.microAverageRecall).isEqualTo(0.978, offset)
        assertThat(evaluation.microAveragePrecision).isEqualTo(0.978, offset)
        assertThat(evaluation.microAverageF1).isEqualTo(0.978, offset)

        assertThat(evaluation.macroAverageRecall).isEqualTo(0.978, offset)
        assertThat(evaluation.macroAveragePrecision).isEqualTo(0.98, offset)
        assertThat(evaluation.macroAverageF1).isEqualTo(0.978, offset)

        assertThat(evaluation.balancedErrorRate).isEqualTo(0.022, offset)
    }

    @Order(1)
    @Test
    fun serializedModel() {
        val serializedModel = classification.serializedModel()
        val loadedModel = classification.loadModel(serializedModel)
        assertThat(loadedModel).isEqualTo("It's a Model<Label>!")
    }

}