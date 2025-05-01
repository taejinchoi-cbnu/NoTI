package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        findViewById<Button>(R.id.signUpBtn).setOnClickListener {
            // 회원가입 페이지로 이동
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.signInBtn).setOnClickListener {
            // 메인 페이지로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.SignInmain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}