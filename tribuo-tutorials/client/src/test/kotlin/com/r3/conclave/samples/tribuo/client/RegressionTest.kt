package com.r3.conclave.samples.tribuo.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import java.util.*

internal class RegressionTestWatcher : TestWatcher {
    override fun testDisabled(context: ExtensionContext?, reason: Optional<String>?) {
        RegressionTest.regressionTearDown()
    }
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(RegressionTestWatcher::class)
class RegressionTest : TribuoTest() {
    companion object {
        private val OFFSET = Offset.offset(0.000001)

        private lateinit var regression: Regression

        @BeforeAll
        @JvmStatic
        fun regressionSetup() {
            regression = Regression(client)
        }

        @AfterAll
        @JvmStatic
        fun regressionTearDown() {
            regression.close()
        }
    }

    @Order(0)
    @Test
    fun trainSGD() {
        val trainSGD = regression.trainSGD(Regression.epochs, Regression.seed)
        assertThat(trainSGD.rmse).isEqualTo(0.96745, OFFSET)
        assertThat(trainSGD.mae).isEqualTo(0.720619, OFFSET)
        assertThat(trainSGD.r2).isEqualTo(-0.439255, OFFSET)
    }

    @Order(1)
    @Test
    fun trainAdaGrad() {
        val trainAdaGrad = regression.trainAdaGrad(Regression.epochs, Regression.seed)
        assertThat(trainAdaGrad.rmse).isEqualTo(0.737994, OFFSET)
        assertThat(trainAdaGrad.mae).isEqualTo(0.585709, OFFSET)
        assertThat(trainAdaGrad.r2).isEqualTo(0.162497, OFFSET)
    }

    @Order(2)
    @Test
    fun trainCART() {
        val trainCART = regression.trainCART(Regression.maxDepth)
        assertThat(trainCART.rmse).isEqualTo(0.657900, OFFSET)
        assertThat(trainCART.mae).isEqualTo(0.494812, OFFSET)
        assertThat(trainCART.r2).isEqualTo(0.334420, OFFSET)
    }
}