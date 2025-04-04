package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // 이미 계정이 있는 경우 로그인 페이지로 이동
        findViewById<Button>(R.id.signInBtn).setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val nameEditText = findViewById<EditText>(R.id.signUpName)
        val emailEditText = findViewById<EditText>(R.id.signUpEmail)
        val passwordEditText = findViewById<EditText>(R.id.signUpPassword)
        val checkPasswordEditText = findViewById<EditText>(R.id.signUpCheckPassword)
        val signUpButton = findViewById<Button>(R.id.signUpBtn)

        signUpButton.setOnClickListener {
            val name = nameEditText.text.toString();
            val email = emailEditText.text.toString();
            val password = passwordEditText.text.toString();
            val checkPassword = checkPasswordEditText.text.toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || checkPassword.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != checkPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버로 넘겨서 회원가입 처리 로직

            // 회원가입 완료 메세지
            Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            // 회원가입 완료 후 로그인 페이지로 이동
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signUpmain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
