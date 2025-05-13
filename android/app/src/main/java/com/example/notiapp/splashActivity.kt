package com.example.notiapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class splashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 12 이상에서는 시스템 스플래시 화면만 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 약간의 지연 추가 (시스템 스플래시 화면이 표시된 후)
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToSignIn()
            }, 100) // 최소한의 지연
        } else {
            // Android 12 미만에서는 커스텀 스플래시 화면 사용
            setContentView(R.layout.activity_splash)

            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // 1.5초 후 로그인 화면으로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToSignIn()
            }, 1500)
        }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}