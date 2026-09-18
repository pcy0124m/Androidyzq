package com.example.totp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.totp.databinding.ActivityScanBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

class QRScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
            )
        )
    }
    private var processed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startCamera()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy -> analyze(proxy) }

            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(proxy: ImageProxy) {
        if (processed) {
            proxy.close()
            return
        }

        val rotation = proxy.imageInfo.rotationDegrees
        val plane = proxy.planes[0]
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = proxy.width
        val height = proxy.height

        val yData = ByteArray(plane.buffer.remaining())
        plane.buffer.get(yData)

        // 去掉每行 rowStride 对齐产生的 padding，得到紧密排列的亮度数组
        val tight = ByteArray(width * height)
        var src = 0
        var dst = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                tight[dst++] = yData[src + col * pixelStride]
            }
            src += rowStride
        }
        proxy.close()

        // 按相机旋转角把画面转正，否则竖屏时 ZXing 解不出码
        val (lum, w, h) = rotateLuminance(tight, width, height, rotation)

        try {
            val source = PlanarYUVLuminanceSource(lum, w, h, 0, 0, w, h, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decode(bitmap)
            processed = true
            val text = result.text ?: ""
            runOnUiThread {
                val (name, secret) = parse(text)
                if (secret.isNotBlank()) {
                    AccountStore(this).add(name, secret)
                    Toast.makeText(this, "已添加: $name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "二维码里没有有效的密钥", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        } catch (e: Exception) {
            // 这一帧没扫到，继续等下一帧
        }
    }

    private fun rotateLuminance(
        src: ByteArray,
        w: Int,
        h: Int,
        rotation: Int
    ): Triple<ByteArray, Int, Int> {
        return when (rotation) {
            90 -> {
                val out = ByteArray(w * h)
                for (y in 0 until h) for (x in 0 until w) {
                    out[x * h + (h - 1 - y)] = src[y * w + x]
                }
                Triple(out, h, w)
            }
            180 -> {
                val out = ByteArray(w * h)
                for (y in 0 until h) for (x in 0 until w) {
                    out[(h - 1 - y) * w + (w - 1 - x)] = src[y * w + x]
                }
                Triple(out, w, h)
            }
            270 -> {
                val out = ByteArray(w * h)
                for (y in 0 until h) for (x in 0 until w) {
                    out[(w - 1 - x) * h + y] = src[y * w + x]
                }
                Triple(out, h, w)
            }
            else -> Triple(src, w, h)
        }
    }

    private fun parse(text: String): Pair<String, String> {
        if (text.startsWith("otpauth://", ignoreCase = true)) {
            val uri = android.net.Uri.parse(text)
            val secret = uri.getQueryParameter("secret") ?: ""
            val label = uri.path?.removePrefix("/totp/")?.removePrefix("/") ?: ""
            val issuer = uri.getQueryParameter("issuer")
            val name = if (!issuer.isNullOrBlank()) "$issuer:$label" else label
            return Pair(if (name.isBlank()) "账号" else name, secret)
        }
        return Pair("账号", text.trim())
    }
}
