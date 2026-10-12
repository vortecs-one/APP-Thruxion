package com.thruxion.app.rt

import com.thruxion.app.rt.ws.CloseCodeAction
import com.thruxion.app.rt.ws.HttpStatusAction
import com.thruxion.app.rt.ws.RtDecisionTable
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionTableTest {

    @Test
    fun testHttpStatusDecisionTable() {
        assertEquals(HttpStatusAction.StopClientError, RtDecisionTable.handleHttpStatus(400))
        assertEquals(HttpStatusAction.RefreshTokenAndReconnect, RtDecisionTable.handleHttpStatus(401))
        assertEquals(HttpStatusAction.StopAndRequireSignIn, RtDecisionTable.handleHttpStatus(403))
        assertEquals(HttpStatusAction.Backoff(30_000L), RtDecisionTable.handleHttpStatus(429))
        assertEquals(HttpStatusAction.Backoff(null), RtDecisionTable.handleHttpStatus(503))
    }

    @Test
    fun testCloseCodeDecisionTable() {
        assertEquals(CloseCodeAction.ReconnectIfTracking, RtDecisionTable.handleCloseCode(1000))
        assertEquals(CloseCodeAction.Backoff(null), RtDecisionTable.handleCloseCode(1003))
        assertEquals(CloseCodeAction.Backoff(60_000L), RtDecisionTable.handleCloseCode(1008))
        assertEquals(CloseCodeAction.Backoff(null), RtDecisionTable.handleCloseCode(1009))
        assertEquals(CloseCodeAction.Backoff(null), RtDecisionTable.handleCloseCode(1012))
        assertEquals(CloseCodeAction.Backoff(null), RtDecisionTable.handleCloseCode(1013))
        assertEquals(CloseCodeAction.RefreshTokenAndReconnect, RtDecisionTable.handleCloseCode(4001))
        assertEquals(CloseCodeAction.StopAndRequireSignIn, RtDecisionTable.handleCloseCode(4003))
        assertEquals(CloseCodeAction.DoNotReconnect, RtDecisionTable.handleCloseCode(4009))
    }
}
