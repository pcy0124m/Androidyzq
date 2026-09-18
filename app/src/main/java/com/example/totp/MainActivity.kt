package com.example.totp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.totp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AccountAdapter
    private val store by lazy { AccountStore(this) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AccountAdapter(store.load(),
            onDelete = { pos ->
                store.delete(pos)
                refresh()
            },
            onCopy = { code ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("totp", code))
                Toast.makeText(this, "已复制 $code", Toast.LENGTH_SHORT).show()
            })
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fabManual.setOnClickListener { showManualDialog() }
        binding.fabScan.setOnClickListener { checkCamera() }

        tick()
    }

    private fun tick() {
        adapter.notifyDataSetChanged()
        handler.postDelayed({ tick() }, 1000)
    }

    private fun refresh() {
        adapter.items = store.load()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun checkCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, QRScanActivity::class.java))
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, QRScanActivity::class.java))
        } else {
            Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showManualDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add, null)
        AlertDialog.Builder(this)
            .setTitle("手动添加")
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val name = view.findViewById<EditText>(R.id.etName).text.toString()
                val secret = view.findViewById<EditText>(R.id.etSecret).text.toString()
                if (name.isNotBlank() && secret.isNotBlank()) {
                    store.add(name, secret)
                    refresh()
                } else {
                    Toast.makeText(this, "名称和密钥都要填", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
