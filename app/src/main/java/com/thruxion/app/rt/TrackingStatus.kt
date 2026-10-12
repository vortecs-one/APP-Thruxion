package com.thruxion.app.rt

sealed interface TrackingStatus {
    object Stopped : TrackingStatus {
        override fun toString(): String = "Stopped"
    }

    object Connecting : TrackingStatus {
        override fun toString(): String = "Connecting"
    }

    object Live : TrackingStatus {
        override fun toString(): String = "Live"
    }

    data class Offline(val pendingCount: Int) : TrackingStatus {
        override fun toString(): String = if (pendingCount > 0) "Offline ($pendingCount points waiting)" else "Offline"
    }

    object SignInRequired : TrackingStatus {
        override fun toString(): String = "Stopped: sign in again"
    }
}
