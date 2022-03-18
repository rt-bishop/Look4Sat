package com.rtbishop.look4sat.presentation.radarScreen

import android.Manifest
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.io.OutputStream
import java.util.*

class BTReporter(private val reporterScope: CoroutineScope) {

    private var rotationConnectBTJob: Job? = null
    private var rotationReportingBT: Job? = null
    private var satVisible=false
    private var CRchar:Char = '\r'
    private var NLchar:Char = '\n'
    private var TBchar:Char = '\t'
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var mmOutStream: OutputStream
    private val SPPID: UUID=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private var connected = false
    private var connectInProgress = false

    fun connectBTDevice(dev: String) {
        if (!connected) {

            rotationConnectBTJob = reporterScope.launch {
                runCatching {
                    connectInProgress = true
                    val rotationBTDevice = bluetoothAdapter.getRemoteDevice(dev)
                    val sock = rotationBTDevice.createInsecureRfcommSocketToServiceRecord(SPPID)
                    sock.connect()
                    mmOutStream = sock.outputStream
                    connected = true

                    connectInProgress = false
                    Log.i("look4satBT", "Connected!")
                }.onFailure { error: Throwable ->
                    Log.e("BT Error", "${error.message}")
                }
            }
        }

    }


    fun isBTConnected():Boolean
    {
        return connected
    }

    fun connectInProg():Boolean
    {
        return connectInProgress
    }


    fun reportRotationBT(dev: String, fmt: String, AZ: Int, EL: Int) {

        runCatching {
            var azStr:String
            var elStr:String
            satVisible=(EL>1)

            //Need to add leading zeros to string to ensure always 3 digits.
            //Ideally this could be done via the format string but this will do for now.
            if(AZ<100){
                if(AZ<10){
                    azStr="00"
                }
                else
                {
                    azStr="0"
                }
                azStr=azStr.plus(AZ.toString())
            }
            else
            {
                azStr=AZ.toString()
            }

            if(EL<100){
                if(EL<10){
                    elStr="00"
                }
                else
                {
                    elStr="0"
                }
                if(satVisible) {
                    elStr = elStr.plus(EL.toString())
                }
                else {
                    elStr="000"
                }

            }
            else
            {
                elStr=EL.toString()
            }

            var buffer = fmt.replace("\$AZ",azStr)
            buffer = buffer.replace("\$EL",elStr)
            buffer = buffer.replace("\\r",CRchar.toString())
            buffer = buffer.replace("\\n",NLchar.toString())
            buffer = buffer.replace("\\t",TBchar.toString())
            Log.i("Output is", buffer)
            if(connected) {
                Log.i("Sending BT", buffer)
                this.mmOutStream.write(buffer.toByteArray())
                Log.i("Sent", buffer)
            }
        }.onFailure { error: Throwable ->
            Log.e("BT Error","${error.message}")
            connected=false
        }
    }
}

