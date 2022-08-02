package com.zhushenwudi.libqr

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.*
import java.io.UnsupportedEncodingException

class QRHelper(private val context: Context, private val callback: (code: String) -> Unit) {
    private lateinit var usbManager: UsbManager
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null
    private var scope: CoroutineScope? = null
    private var isConnected = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, mIntent: Intent) {
            if (mIntent.action == ACTION_USB_PERMISSION) {
                val granted = mIntent.extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                granted?.let {
                    if (granted) {
                        val intent = Intent(ACTION_USB_PERMISSION_GRANTED)
                        context.sendBroadcast(intent)
                        connection = usbManager.openDevice(device)
                        if (connection == null) {
                            return@let
                        }
                        scope = CoroutineScope(Dispatchers.IO)
                        scope?.launch {
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
                            serialPort?.run {
                                if (open()) {
                                    isConnected = true
                                    setBaudRate(BAUD_RATE)
                                    setDataBits(UsbSerialInterface.DATA_BITS_8)
                                    setStopBits(UsbSerialInterface.STOP_BITS_1)
                                    setParity(UsbSerialInterface.PARITY_NONE)
                                    setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                                    read {
                                        try {
                                            val str = String(it, Charsets.UTF_8).trim()
                                            if (str.isNotEmpty()) {
                                                callback(str)
                                            }
                                        } catch (e: UnsupportedEncodingException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } ?: run {
                                context.sendBroadcast(Intent(ACTION_USB_NOT_SUPPORTED))
                            }
                        }
                    } else {
                        val intent = Intent(ACTION_USB_PERMISSION_NOT_GRANTED)
                        context.sendBroadcast(intent)
                    }
                }
            } else if (mIntent.action == ACTION_USB_ATTACHED) {
                if (!isConnected) {
                    findSerialPortDevice()
                }
            } else if (mIntent.action == ACTION_USB_DETACHED) {
                val intent = Intent(ACTION_USB_DISCONNECTED)
                context.sendBroadcast(intent)
                if (isConnected) {
                    serialPort?.close()
                }
                isConnected = false
            }
        }
    }

    private fun init() {
        setFilter()
        usbManager = context.getSystemService(USB_SERVICE) as UsbManager
        findSerialPortDevice()
    }

    /**
     * 寻找二维码设备
     */
    private fun findSerialPortDevice() {
        val usbDevices = usbManager.deviceList
        if (usbDevices.isNotEmpty()) {
            var keep = true
            for ((_, value) in usbDevices) {
                device = value
                val deviceVID = device?.vendorId
                val devicePID = device?.productId
                if (deviceVID == null || devicePID == null) {
                    continue
                }
                Log.d("aaa", "deviceVID: $deviceVID - devicePID: $devicePID")
                if (deviceVID == 0x27DD && devicePID == 0x0002 || deviceVID == 0x0103 && devicePID == 0x6061 || deviceVID == 0x26F1 && devicePID == 0x5650) {
                    requestUserPermission()
                    keep = false
                } else {
                    connection = null
                    device = null
                }
                if (!keep) break
            }
            if (keep) {
                val intent = Intent(ACTION_NO_USB)
                context.sendBroadcast(intent)
            }
        } else {
            val intent = Intent(ACTION_NO_USB)
            context.sendBroadcast(intent)
        }
    }

    /**
     * 设置 USB事件 广播过滤器
     */
    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(usbReceiver, filter)
    }

    private fun requestUserPermission() {
        val mPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            0
        )
        usbManager.requestPermission(device, mPendingIntent)
    }

    /**
     * 向二维码模块下发字节数组指令
     */
    suspend fun write(data: ByteArray?) {
        checkConnected()
        serialPort?.write(data)
    }

    /**
     * 扫描一次
     */
    suspend fun readOnce() {
        checkConnected()
        serialPort?.write(HexUtil.decodeHex(READ_ONCE))
    }

    /**
     * 连续扫描
     */
    suspend fun readContinuous() {
        checkConnected()
        serialPort?.write(HexUtil.decodeHex(READ_CONTINUOUS))
    }

    /**
     * 停止扫描
     */
    fun stopRead() {
        serialPort?.write(HexUtil.decodeHex(STOP_READ))
    }

    private suspend fun checkConnected() {
        if (!isConnected) {
            init()
            delay(300)
        }
    }

    fun release() {
        serialPort?.close()
        serialPort = null
        if (scope?.isActive == true) {
            scope?.cancel()
        }
        scope = null
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
        }
        isConnected = false
    }

    fun isRunning(): Boolean {
        return isConnected
    }

    companion object {
        const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        const val ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED"
        const val ACTION_NO_USB = "com.felhr.usbservice.NO_USB"
        const val ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED =
            "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        const val READ_ONCE = "020700535730303030309A0103"
        const val READ_CONTINUOUS = "020700535730303030319B0103"
        const val STOP_READ = "02070053574646464646080203"
        private const val BAUD_RATE = 9600
    }
}