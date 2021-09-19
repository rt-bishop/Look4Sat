package com.rtbishop.look4sat.data

interface DataReporter {

    suspend fun setRotatorSocket(server: String, port: Int)

    suspend fun reportRotation(azim: Double, elev: Double)
}
