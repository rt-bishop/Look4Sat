/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.data.framework

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.UUID
import java.io.IOException
import com.rtbishop.look4sat.domain.model.SatRadio
import android.content.Context
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import java.io.InputStream

enum class Mode(val value: Int) {
    LSB(0),
    USB(1),
    AM(2),
    CW(3),
    FMN(5),
    FM(5),
	DSTAR(17);

    companion object {
        fun fromString(name: String): Int {
            return values().find { it.name.equals(name, ignoreCase = true) }?.value ?: 1
        }
    }
}

fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    return bluetoothManager?.adapter
}

public object BluetoothCIV {

    // UUID for Serial Port Service
    private val SERIAL_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    // Bluetooth adapter
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    // CI-V Address of the ICOM IC-705 (verify for your radio)
    private val CIV_ADDRESS = 0xA4
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    public fun init(context: Context) {
		bluetoothAdapter = getBluetoothAdapter(context)
    }

    private fun ensureConnected():Boolean {
        if( bluetoothSocket?.isConnected != true) {
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket?.close()
                } catch (e: IOException) {
                    Log.e("BluetoothCivManager", "Fehler beim Schließen des alten Sockets", e)
                }
                bluetoothSocket = null
            }
            val dev = bluetoothAdapter?.bondedDevices?.firstOrNull { device -> device.name == "ICOM BT(IC-705)"}
            if(dev==null) {
                Log.e("BluetoothCivManager", "Gerät nicht gefunden")
                return false
            } else {
              try{
                  val localSocket = dev.createRfcommSocketToServiceRecord(SERIAL_UUID)
                  localSocket.connect()
                  outputStream = localSocket.outputStream
                  inputStream = localSocket.inputStream
                  bluetoothSocket = localSocket
                 // delay(100);
                  Thread.sleep(100);
                  Log.d("BluetoothCivManager", "Verbindung aufgebaut")
              } catch (e: IOException) {
                  Log.e("BluetoothCivManager", "Verbindung nicht aufgebaut")
                  bluetoothSocket = null // Falls fehlgeschlagen, Socket auf null setzen
                  return false
              }
            }
        }
        return true
    }

    private fun sendCommand(cmd: Int, sub: Int? = null, data: ByteArray? = null) {
        outputStream!!.write(0xfe);
        outputStream!!.write(0xfe);
        outputStream!!.write(CIV_ADDRESS);
        outputStream!!.write(0xe0);
        outputStream!!.write(cmd);
        if (sub != null) {
            outputStream!!.write(sub)
        }
        if (data != null) {
            outputStream!!.write(data)
        }
        outputStream!!.write(0xfd)
        outputStream!!.flush();
    }

    data class RMsg(val broadcast: Boolean, val cmd: Int, val payload: ByteArray)
    private fun receiveCommand(): RMsg? {
        if (inputStream!!.read()!=0xFE) {
            return null;
        }
        if (inputStream!!.read()!=0xFE) {
            return null;
        }
        val to = inputStream!!.read();
        if (inputStream!!.read()!= CIV_ADDRESS) {
            return null;
        }
        val cmd = inputStream!!.read();

        val buffer = mutableListOf<Byte>()
        var byte: Int

        while (inputStream!!.read().also { byte = it } != -1) {
            if (byte == 0xFD) break
            buffer.add(byte.toByte())
        }

        return RMsg(to==0x00, cmd, buffer.toByteArray())
    }

    private fun receiveSpecificCommand(): RMsg? {
        while (true) {
            val msg = receiveCommand() ?: return null
            if (!msg.broadcast) {
                return msg
            }
        }
    }

    private fun callProcedure(cmd: Int, sub: Int? = null, data: ByteArray? = null):Boolean {
        if(ensureConnected()){
            sendCommand(cmd, sub, data)
            val res = receiveSpecificCommand();
            return res?.cmd == 0xFB;
        } else {
            return false;
        }
    }

    data class Result(val subcmd: Int?, val data: ByteArray)
    private fun callFunction(cmd: Int, sub: Int? = null, retsub: Boolean): Result {
        if(ensureConnected()){
            sendCommand(cmd, sub)
            val res = receiveSpecificCommand();
            if (res!=null){
            if (cmd!=res.cmd) {
                Log.d("BluetoothCivManager", "Wrong command $cmd != $res.cmd")
                return Result(null, byteArrayOf());
            }
            val subcmd = if (retsub) res.payload[0].toInt() else null;
            val data = res.payload.copyOfRange((if (retsub) 1 else 0), res.payload.size);
            return Result(subcmd, data)
            }else {
                Log.d("BluetoothCivManager", "No Reply")
                return Result(null, byteArrayOf());
            }
        } else {
            Log.d("BluetoothCivManager", "NotSure")
            return Result(null, byteArrayOf());
        }
    }

    public var selected: String = "NONE";
    public var isTransponder: Boolean = false;
    public var lastDownlinkLow: Long? = null;
    public var lastDownlinkHigh: Long? = null;


    private fun frequencyToBCD(frequency: Long): ByteArray {
        var n = frequency
        val bcd = ByteArray(5)
        // Process each two-digit group, starting from the rightmost group.
        for (i in 4 downTo 0) {
            // Extract the last two digits (a value between 0 and 99)
            val twoDigits = (n % 100).toInt()
            // Pack the tens digit into the high nibble and the ones digit into the low nibble
            bcd[i] = (((twoDigits / 10) shl 4) or (twoDigits % 10)).toByte()
            // Remove the two digits we just processed
            n /= 100
        }
        return bcd
    }

    // Function to convert BCD format back to a frequency (Long)
    private fun bcdToFrequency(bcd: ByteArray): Long {
        var frequency = 0L
        for (byte in bcd) {
            // Extract the high nibble (first decimal digit)
            val high = (byte.toInt() shr 4) and 0x0F
            // Extract the low nibble (second decimal digit)
            val low = byte.toInt() and 0x0F
            // Combine them into the frequency
            frequency = frequency * 100 + high * 10 + low
        }
        return frequency
    }

    private fun setVFO(A: Boolean): Boolean {
        return callProcedure(0x07, if (A) 0x00 else 0x01);
    }

    private fun setSplit(On: Boolean): Boolean {
        return callProcedure(0x0F, if (On) 0x01 else 0x00);
    }

    private fun setFrequency(Main: Boolean, Frequency: Long): Boolean {
        return callProcedure(0x25, if (Main) 0x00 else 0x01, frequencyToBCD(Frequency).reversedArray());
    }

    private fun getFrequency(Main: Boolean): Long {
        val res = callFunction(0x25, if (Main) 0x00 else 0x01, true)
        return if (res.data.isNotEmpty()){
            bcdToFrequency(res.data.reversedArray());
        } else {
            -1
        }
    }

	private fun setMode(Main: Boolean, mode: String): Boolean {
        return callProcedure(0x26, if (Main) 0x00 else 0x01, byteArrayOf(Mode.fromString(mode).toByte(), 0x00.toByte(), 0x01.toByte()));
    }

	@SuppressLint("MissingPermission")
	public fun connect(radio: SatRadio) {
   		setVFO(true)
        setSplit(true)
        setMode(true, radio.downlinkMode?:"USB")
        setMode(false, radio.uplinkMode?:"USB")
        selected = radio.uuid;
        isTransponder = radio.downlinkHigh != null;
        lastDownlinkLow = null;
        lastDownlinkHigh = null;
		// Info on tone?
	}

    fun Double.clamp(min: Double, max: Double): Double {
        return when {
            this < min -> min
            this > max -> max
            else -> this
        }
    }

    // Function to send the CI-V command to the ICOM radio over Bluetooth
    public fun updateOnce(radio: SatRadio) {
        if(radio.uuid != selected) return;

        if(isTransponder) {
            if (lastDownlinkLow != null && lastDownlinkHigh != null) {
                val actual = getFrequency(true)
                val pos = ((actual - lastDownlinkLow!!).toDouble() / (lastDownlinkHigh!! - lastDownlinkLow!!)).clamp(0.0,1.0);
                Log.d("BluetoothCivManager", "Pos: $pos");
                radio.downlinkLow?.let{
                    setFrequency(true, (radio.downlinkLow!! + pos * (radio.downlinkHigh!!-radio.downlinkLow!!)).toLong());
                }
                radio.uplinkLow?.let {
                    setFrequency(false, (radio.uplinkLow!! + pos * (radio.uplinkHigh!!-radio.uplinkLow!!)).toLong())
                }
            } else {
                radio.downlinkLow?.let{
                    setFrequency(true, it);
                }
                radio.uplinkLow?.let {
                    setFrequency(false, it)
                }
            }
            lastDownlinkLow = radio.downlinkLow;
            lastDownlinkHigh = radio.downlinkHigh;
        }
        else {
            radio.downlinkLow?.let{
                setFrequency(true, it);
            }
            radio.uplinkLow?.let {
                setFrequency(false, it)
            }
        }
    }
}
