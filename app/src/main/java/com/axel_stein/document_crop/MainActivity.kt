package com.axel_stein.document_crop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.axel_stein.document_crop.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }
}