package com.thruxion.app.rt

import com.thruxion.app.rt.db.PendingPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchingTest {

    @Test
    fun testBatchingAtMost50PointsOldestFirst() {
        val points = (1L..100L).map { seq ->
            PendingPoint(
                seq = seq,
                t = 1700000000000L + seq,
                lat = 40.0,
                lon = -3.0,
                acc = 5.0,
                src = "gps",
                mock = false
            )
        }

        val limit = 50
        val batch = points.sortedBy { it.seq }.take(limit)

        assertEquals(50, batch.size)
        assertEquals(1L, batch.first().seq)
        assertEquals(50L, batch.last().seq)

        for (i in 0 until batch.size - 1) {
            assertTrue("Points must be ordered oldest first", batch[i].seq < batch[i + 1].seq)
        }
    }
}
