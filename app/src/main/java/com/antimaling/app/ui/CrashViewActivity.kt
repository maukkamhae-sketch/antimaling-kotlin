package com.antimaling.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class CrashViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = try { File(filesDir, "crash.txt").readText() } catch (e: Exception) { "Tidak ada data crash." }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24) }
        val btn = Button(this).apply {
            text = "Salin error"
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("crash", trace))
                Toast.makeText(this@CrashViewActivity, "Tersalin", Toast.LENGTH_SHORT).show()
            }
        }
        val tv = TextView(this).apply { text = trace; textSize = 11f; setTextIsSelectable(true) }
        root.addView(btn)
        root.addView(ScrollView(this).apply { addView(tv) })
        setContentView(root)
    }
}
