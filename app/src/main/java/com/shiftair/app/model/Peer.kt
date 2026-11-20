package com.shiftair.app.model

data class Peer(
    val deviceName: String,
    val deviceAddress: String,
    val isGroupOwner: Boolean = false
)
