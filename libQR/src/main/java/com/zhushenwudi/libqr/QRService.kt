package com.zhushenwudi.libqr

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface.*
import com.kunminx.architecture.ui.callback.UnPeekLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException

class QRService: LifecycleService() {
    private lateinit var usbManager: UsbManager
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null
    private var scope: CoroutineScope? = null
    private var mHandler: UnPeekLiveData<String>? = null
    private var serialPortConnected = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, mIntent: Intent) {
            if (mIntent.action == ACTION_USB_PERMISSION) {
                val granted = mIntent.extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                granted?.let {
                    if (granted) {
                        val intent = Intent(ACTION_USB_PERMISSION_GRANTED)
                        arg0.sendBroadcast(intent)
                        connection = usbManager.openDevice(device)
                        scope = CoroutineScope(Dispatchers.IO)
                        scope?.launch {
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
                            serialPort?.run {
                                if (open()) {
                                    serialPortConnected = true
                                    setBaudRate(BAUD_RATE)
                                    setDataBits(DATA_BITS_8)
                                    setStopBits(STOP_BITS_1)
                                    setParity(PARITY_NONE)
                                    setFlowControl(FLOW_CONTROL_OFF)
                                    read {
                                        try {
                                            mHandler?.postValue(String(it, Charsets.UTF_8))
                                        } catch (e: UnsupportedEncodingException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } ?: run {
                                sendBroadcast(Intent(ACTION_USB_NOT_SUPPORTED))
                            }
                        }
                    } else {
                        val intent = Intent(ACTION_USB_PERMISSION_NOT_GRANTED)
                        arg0.sendBroadcast(intent)
                    }
                }
            } else if (mIntent.action == ACTION_USB_ATTACHED) {
                if (!serialPortConnected) {
                    findSerialPortDevice()
                }
            } else if (mIntent.action == ACTION_USB_DETACHED) {
                val intent = Intent(ACTION_USB_DISCONNECTED)
                arg0.sendBroadcast(intent)
                if (serialPortConnected) {
                    serialPort?.close()
                }
                serialPortConnected = false
            }
        }
    }

    init {
        serialPortConnected = false
        SERVICE_CONNECTED = true
    }

    /**
     * 向二维码模块下发字节数组指令
     */
    fun write(data: ByteArray?) {
        serialPort?.write(data)
    }

    /**
     * 扫描一次
     */
    fun readOnce() {
        serialPort?.write(HexUtil.decodeHex(READ_ONCE))
    }

    /**
     * 连续扫描
     */
    fun readContinuous() {
        serialPort?.write(HexUtil.decodeHex(READ_CONTINUOUS))
    }

    /**
     * 停止扫描
     */
    fun stopRead() {
        serialPort?.write(HexUtil.decodeHex(STOP_READ))
    }

    fun setHandler(handler: UnPeekLiveData<String>) {
        mHandler = handler
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
                sendBroadcast(intent)
            }
        } else {
            val intent = Intent(ACTION_NO_USB)
            sendBroadcast(intent)
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
        registerReceiver(usbReceiver, filter)
    }

    private fun requestUserPermission() {
        val mPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            0
        )
        usbManager.requestPermission(device, mPendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        SERVICE_CONNECTED = false
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        setFilter()
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        findSerialPortDevice()
        return QRBinder(this)
    }

    internal class QRBinder(private val service: QRService) : Binder() {
        fun getService(): QRService {
            return service
        }
    }

    companion object {
        const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        const val ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED"
        const val ACTION_NO_USB = "com.felhr.usbservice.NO_USB"
        const val ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        const val READ_ONCE = "020700535730303030309A0103"
        const val READ_CONTINUOUS = "020700535730303030319B0103"
        const val STOP_READ = "02070053574646464646080203"
        private const val BAUD_RATE = 9600
        var SERVICE_CONNECTED = false
    }
}