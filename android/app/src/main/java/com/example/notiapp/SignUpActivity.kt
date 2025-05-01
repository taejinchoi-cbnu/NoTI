package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUpActivity : AppCompatActivity() {
    // 로그 태그 private val TAG = "SignUpActivity"

    // UI 요소 변수 선언
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var checkPasswordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var signInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // UI 요소 초기화
        nameEditText = findViewById(R.id.signUpName)
        emailEditText = findViewById(R.id.signUpEmail)
        passwordEditText = findViewById(R.id.signUpPassword)
        checkPasswordEditText = findViewById(R.id.signUpCheckPassword)
        signUpButton = findViewById(R.id.signUpBtn)
        signInButton = findViewById(R.id.signInBtn)

        // 회원가입 버튼 클릭 리스너 설정
        signUpButton.setOnClickListener {
            validateAndSignUp()
        }

        // 로그인 버튼 클릭 리스너 설정
        signInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // Edge-to-Edge 설정 유지
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signUpmain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateAndSignUp() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = checkPasswordEditText.text.toString()

       /* Log.d(TAG, "입력된 데이터 확인:")
        Log.d(TAG, "이름: $name")
        Log.d(TAG, "이메일: $email")
        Log.d(TAG, "비밀번호: $password")
        Log.d(TAG, "비밀번호 확인: $confirmPassword") */

        // 모든 필드가 채워져 있는지 확인
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            // Log.e(TAG, "빈 필드가 있습니다")

            // 구체적인 오류 메시지 표시
            if (name.isEmpty()) {
                nameEditText.error = "이름을 입력해주세요"
            }
            if (email.isEmpty()) {
                emailEditText.error = "이메일을 입력해주세요"
            }
            if (password.isEmpty()) {
                passwordEditText.error = "비밀번호를 입력해주세요"
            }
            if (confirmPassword.isEmpty()) {
                checkPasswordEditText.error = "비밀번호 확인을 입력해주세요"
            }
            return
        }

        // 이메일 형식 확인
        if (!isValidEmail(email)) {
            emailEditText.error = "유효한 이메일 형식이 아닙니다"
            Toast.makeText(this, "유효한 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
            // Log.e(TAG, "유효하지 않은 이메일 형식: $email")
            return
        }

        // 비밀번호 길이 확인 (8자 이상)
        if (password.length < 8) {
            passwordEditText.error = "비밀번호는 8자 이상이어야 합니다"
            Toast.makeText(this, "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            // Log.e(TAG, "비밀번호가 너무 짧습니다: ${password.length}자")
            return
        }

        // 비밀번호 일치 확인
        if (password != confirmPassword) {
            checkPasswordEditText.error = "비밀번호가 일치하지 않습니다"
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            // Log.e(TAG, "비밀번호 불일치: $password vs $confirmPassword")
            return
        }

        /* 모든 유효성 검사를 통과하면 로그로 확인
        Log.d(TAG, "유효성 검사 통과! 회원가입 데이터:")
        Log.d(TAG, "이름: $name")
        Log.d(TAG, "이메일: $email")
        Log.d(TAG, "비밀번호: $password") */

        // 사용자에게 입력 데이터 확인 메시지 표시
        val message = "입력 데이터 확인:\n" +
                "이름: $name\n" +
                "이메일: $email\n" +
                "유효성 검사 통과!"

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // 이제 서버 연동 코드를 구현할 수 있습니다
        // signUp(name, email, password)
    }

    private fun isValidEmail(email: String): Boolean {
        val result = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        // Log.d(TAG, "이메일 유효성 검사 결과: $result (이메일: $email)")
        return result
    }
}