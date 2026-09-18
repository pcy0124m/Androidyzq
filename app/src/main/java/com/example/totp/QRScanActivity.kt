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
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

class QRScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private val reader = MultiFormatReader()
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
        }, ContextCompat.mainExecutor(this))
    }

    private fun analyze(proxy: ImageProxy) {
        if (processed) {
            proxy.close()
            return
        }
        val buffer = proxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val width = proxy.width
        val height = proxy.height
        proxy.close()

        try {
            val source = PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decode(bitmap)
            processed = true
            runOnUiThread {
                val text = result.text ?: ""
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
            // no QR found in this frame, keep scanning
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
        // 直接是 Base32 密钥
        return Pair("账号", text.trim())
    }
}
