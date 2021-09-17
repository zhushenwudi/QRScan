package com.zhushenwudi.libqr

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

class QRConnection(private var listener: ((service: QRService) -> Unit)? = {}) : ServiceConnection {
    private lateinit var qrService: QRService

    // 是否绑定服务
    private var isBind = false

    override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
        try {
            qrService = (service as QRService.QRBinder).getService()
            listener?.invoke(qrService)
            isBind = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        listener = null
        isBind = false
    }

    fun getIsBind(): Boolean {
        return isBind
    }
}