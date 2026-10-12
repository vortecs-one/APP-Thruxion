package com.thruxion.app.rt

import com.thruxion.app.rt.ws.AckData
import com.thruxion.app.rt.ws.RejectedSeq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AckHandlingTest {

    @Test
    fun testAckDeletesBothAcceptedAndRejectedSeqs() {
        val ackData = AckData(
            seqs = listOf(1L, 2L, 3L),
            rejected = listOf(
                RejectedSeq(seq = 4L, reason = "too_old"),
                RejectedSeq(seq = 5L, reason = "in_future")
            )
        )

        val acceptedSeqs = ackData.seqs
        val rejectedSeqs = ackData.rejected.map { it.seq }
        val allSeqsToDelete = (acceptedSeqs + rejectedSeqs).distinct()

        assertEquals(5, allSeqsToDelete.size)
        assertTrue(allSeqsToDelete.containsAll(listOf(1L, 2L, 3L, 4L, 5L)))
    }
}
