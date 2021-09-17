package com.zhushenwudi.libqr

import android.util.Log
import java.lang.RuntimeException

object HexUtil {
    private const val TAG = "HexUtil"

    /**
     * 用于建立十六进制字符的输出的小写字符数组
     */
    private val DIGITS_LOWER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    private val DIGITS_UPPER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data byte[]
     * @return 十六进制char[]
     */
    fun encodeHex(data: ByteArray): CharArray? {
        return encodeHex(data, true)
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data        byte[]
     * @param toLowerCase `true` 传换成小写格式 ， `false` 传换成大写格式
     * @return 十六进制char[]
     */
    fun encodeHex(data: ByteArray, toLowerCase: Boolean): CharArray? {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制char[]
     */
    fun encodeHex(data: ByteArray, toDigits: CharArray): CharArray? {
        val l = data.size
        val out = CharArray(l shl 1)
        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[0xF0 and data[i].toInt() ushr 4]
            out[j++] = toDigits[0x0F and data[i].toInt()]
            i++
        }
        return out
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data byte[]
     * @return 十六进制String
     */
    fun encodeHexStr(data: ByteArray?): String? {
        return encodeHexStr(data, true)
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data        byte[]
     * @param toLowerCase `true` 传换成小写格式 ， `false` 传换成大写格式
     * @return 十六进制String
     */
    fun encodeHexStr(data: ByteArray?, toLowerCase: Boolean): String? {
        return encodeHexStr(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制String
     */
    fun encodeHexStr(data: ByteArray?, toDigits: CharArray): String? {
        if (data == null) {
            Log.e(TAG, "this data is null.")
            return ""
        }
        return String(encodeHex(data, toDigits)!!)
    }

    /**
     * 将十六进制字符串转换为字节数组
     *
     * @param data
     * @return
     */
    fun decodeHex(data: String?): ByteArray? {
        if (data == null) {
            Log.e(TAG, "this data is null.")
            return ByteArray(0)
        }
        return decodeHex(data.toCharArray())
    }

    /**
     * 将十六进制字符数组转换为字节数组
     *
     * @param data 十六进制char[]
     * @return byte[]
     * @throws RuntimeException 如果源十六进制字符数组是一个奇怪的长度，将抛出运行时异常
     */
    fun decodeHex(data: CharArray): ByteArray? {
        val len = data.size
        if (len and 0x01 != 0) {
            throw RuntimeException("Odd number of characters.")
        }
        val out = ByteArray(len shr 1)

        // two characters form the hex value.
        var i = 0
        var j = 0
        while (j < len) {
            var f = toDigit(data[j], j) shl 4
            j++
            f = f or toDigit(data[j], j)
            j++
            out[i] = (f and 0xFF).toByte()
            i++
        }
        return out
    }

    /**
     * 将十六进制字符转换成一个整数
     *
     * @param ch    十六进制char
     * @param index 十六进制字符在字符数组中的位置
     * @return 一个整数
     * @throws RuntimeException 当ch不是一个合法的十六进制字符时，抛出运行时异常
     */
    private fun toDigit(ch: Char, index: Int): Int {
        val digit = Character.digit(ch, 16)
        if (digit == -1) {
            throw RuntimeException("Illegal hexadecimal character $ch at index $index")
        }
        return digit
    }

    /**
     * 截取字节数组
     *
     * @param src   byte []  数组源  这里填16进制的 数组
     * @param begin 起始位置 源数组的起始位置。0位置有效
     * @param count 截取长度
     * @return
     */
    fun subBytes(src: ByteArray?, begin: Int, count: Int): ByteArray? {
        val bs = ByteArray(count)
        System.arraycopy(src, begin, bs, 0, count) // bs 目的数组  0 截取后存放的数值起始位置。0位置有效
        return bs
    }

    /**
     * int转byte数组
     *
     * @param bb
     * @param x
     * @param index 第几位开始
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     */
    fun intToByte(bb: ByteArray, x: Int, index: Int, flag: Boolean) {
        if (flag) {
            bb[index + 0] = (x shr 24).toByte()
            bb[index + 1] = (x shr 16).toByte()
            bb[index + 2] = (x shr 8).toByte()
            bb[index + 3] = (x shr 0).toByte()
        } else {
            bb[index + 3] = (x shr 24).toByte()
            bb[index + 2] = (x shr 16).toByte()
            bb[index + 1] = (x shr 8).toByte()
            bb[index + 0] = (x shr 0).toByte()
        }
    }

    /**
     * byte数组转int
     *
     * @param bb
     * @param index 第几位开始
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     * @return
     */
    fun byteToInt(bb: ByteArray, index: Int, flag: Boolean): Int {
        return if (flag) {
            ((bb[index + 0].toLong() and 0xff) shl 24
                    or (bb[index + 1].toLong() and 0xff shl 16)
                    or (bb[index + 2].toLong() and 0xff shl 8)
                    or (bb[index + 3].toLong() and 0xff shl 0)).toInt()
        } else {
            (bb[index + 3].toLong() and 0xff shl 24
                    or (bb[index + 2].toLong() and 0xff shl 16)
                    or (bb[index + 1].toLong() and 0xff shl 8)
                    or (bb[index + 0].toLong() and 0xff shl 0)).toInt()
        }
    }


    /**
     * 字节数组逆序
     *
     * @param data
     * @return
     */
    fun reverse(data: ByteArray): ByteArray? {
        val reverseData = ByteArray(data.size)
        for (i in data.indices) {
            reverseData[i] = data[data.size - 1 - i]
        }
        return reverseData
    }

    /**
     * 蓝牙传输 16进制 高低位 读数的 转换
     *
     * @param data 截取数据源，字节数组
     * @param index 截取数据开始位置
     * @param count 截取数据长度，只能为2、4、8个字节
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     * @return
     */
    fun byteToLong(data: ByteArray, index: Int, count: Int, flag: Boolean): Long {
        var lg: Long = 0
        return if (flag) {
            when (count) {
                2 -> lg = (data[index + 0].toLong() and 0xff shl 8
                        or (data[index + 1].toLong() and 0xff shl 0))
                4 -> lg = (data[index + 0].toLong() and 0xff shl 24
                        or (data[index + 1].toLong() and 0xff shl 16)
                        or (data[index + 2].toLong() and 0xff shl 8)
                        or (data[index + 3].toLong() and 0xff shl 0))
                8 -> lg = (data[index + 0].toLong() and 0xff shl 56
                        or (data[index + 1].toLong() and 0xff shl 48)
                        or (data[index + 2].toLong() and 0xff shl 40)
                        or (data[index + 3].toLong() and 0xff shl 32)
                        or (data[index + 4].toLong() and 0xff shl 24)
                        or (data[index + 5].toLong() and 0xff shl 16)
                        or (data[index + 6].toLong() and 0xff shl 8)
                        or (data[index + 7].toLong() and 0xff shl 0))
            }
            lg
        } else {
            when (count) {
                2 -> lg = (data[index + 1].toLong() and 0xff shl 8
                        or (data[index + 0].toLong() and 0xff shl 0))
                4 -> lg = (data[index + 3].toLong() and 0xff shl 24
                        or (data[index + 2].toLong() and 0xff shl 16)
                        or (data[index + 1].toLong() and 0xff shl 8)
                        or (data[index + 0].toLong() and 0xff shl 0))
                8 -> lg = (data[index + 7].toLong() and 0xff shl 56
                        or (data[index + 6].toLong() and 0xff shl 48)
                        or (data[index + 5].toLong() and 0xff shl 40)
                        or (data[index + 4].toLong() and 0xff shl 32)
                        or (data[index + 3].toLong() and 0xff shl 24)
                        or (data[index + 2].toLong() and 0xff shl 16)
                        or (data[index + 1].toLong() and 0xff shl 8)
                        or (data[index + 0].toLong() and 0xff shl 0))
            }
            lg
        }
    }
}