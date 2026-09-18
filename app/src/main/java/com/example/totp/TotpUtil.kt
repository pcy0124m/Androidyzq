package com.example.totp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpUtil {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generate(secret: String, period: Int = 30, digits: Int = 6): String {
        val key = base32Decode(secret)
        if (key.isEmpty()) return "------"
        val counter = System.currentTimeMillis() / 1000 / period
        val msg = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            msg[i] = (c and 0xff).toByte()
            c = c ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(msg)
        val offset = hash[hash.size - 1].toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        val mod = Math.pow(10.0, digits.toDouble()).toInt()
        return String.format("%0${digits}d", binary % mod)
    }

    fun secondsRemaining(period: Int = 30): Int {
        return period - ((System.currentTimeMillis() / 1000) % period).toInt()
    }

    private fun base32Decode(input: String): ByteArray {
        val clean = input.uppercase().replace("=", "").replace(" ", "")
        val bits = StringBuilder()
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            if (v < 0) continue
            bits.append(v.toString(2).padStart(5, '0'))
        }
        val out = mutableListOf<Byte>()
        var i = 0
        while (i + 8 <= bits.length) {
            out.add(bits.substring(i, i + 8).toInt(2).toByte())
            i += 8
        }
        return out.toByteArray()
    }
}
