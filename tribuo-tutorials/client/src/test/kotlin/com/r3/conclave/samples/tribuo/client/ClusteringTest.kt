package com.r3.conclave.samples.tribuo.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ClusteringTest : TribuoTest() {
    companion object {
        private lateinit var clustering: Clustering

        @BeforeAll
        @JvmStatic
        fun clusteringSetup() {
            clustering = Clustering(client)
        }
    }

    @Order(0)
    @Test
    fun train() {
        val train = clustering.train(Clustering.centroids, Clustering.iterations, Clustering.distanceType,
                Clustering.numThreads, Clustering.seed)
        assertThat(train.normalizedMI).isEqualTo(0.8128096132028937)
        assertThat(train.adjustedMI).isEqualTo(0.8113314999600718)
    }

    @Order(1)
    @Test
    fun evaluate() {
        val evaluate = clustering.evaluate()
        assertThat(evaluate.normalizedMI).isEqualTo(0.8154291916732408)
        assertThat(evaluate.adjustedMI).isEqualTo(0.8139169342020222)
    }
}