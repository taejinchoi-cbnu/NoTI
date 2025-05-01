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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SignUpActivity : AppCompatActivity() {
    private val TAG = "SignUpActivity"

    // UI 요소 변수 선언
    private lateinit var nameEditText: EditText
    private lateinit var nicknameEditText: EditText  // 닉네임 필드 추가
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
        nicknameEditText = findViewById(R.id.signUpNickname)  // 닉네임 필드 초기화
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
        val nickname = nicknameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = checkPasswordEditText.text.toString()

        Log.d(TAG, "입력된 데이터 확인:")
        Log.d(TAG, "이름: $name")
        Log.d(TAG, "닉네임: $nickname")
        Log.d(TAG, "이메일: $email")
        Log.d(TAG, "비밀번호: $password")
        Log.d(TAG, "비밀번호 확인: $confirmPassword")

        // 모든 필드가 채워져 있는지 확인
        if (name.isEmpty() || nickname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "빈 필드가 있습니다")

            // 구체적인 오류 메시지 표시
            if (name.isEmpty()) {
                nameEditText.error = "이름을 입력해주세요"
            }
            if (nickname.isEmpty()) {
                nicknameEditText.error = "닉네임을 입력해주세요"  // 닉네임 필드 오류 메시지
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
            Log.e(TAG, "유효하지 않은 이메일 형식: $email")
            return
        }

        // 비밀번호 길이 확인 (8자 이상)
        if (password.length < 8) {
            passwordEditText.error = "비밀번호는 8자 이상이어야 합니다"
            Toast.makeText(this, "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "비밀번호가 너무 짧습니다: ${password.length}자")
            return
        }

        // 비밀번호 일치 확인
        if (password != confirmPassword) {
            checkPasswordEditText.error = "비밀번호가 일치하지 않습니다"
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "비밀번호 불일치: $password vs $confirmPassword")
            return
        }

        // 모든 유효성 검사를 통과하면 서버로 데이터 전송
        Log.d(TAG, "유효성 검사 통과! 회원가입 데이터를 서버로 전송합니다.")
        signUp(name, nickname, email, password)
    }

    private fun isValidEmail(email: String): Boolean {
        val result = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        Log.d(TAG, "이메일 유효성 검사 결과: $result (이메일: $email)")
        return result
    }

    private fun signUp(name: String, nickname: String, email: String, password: String) {
        // 로딩 표시 등 UI 업데이트
        signUpButton.isEnabled = false
        Toast.makeText(this, "회원가입 처리 중...", Toast.LENGTH_SHORT).show()

        // 백그라운드 스레드에서 네트워크 요청 수행
        thread {
            try {
                // JSON 데이터 생성
                val jsonObject = JSONObject().apply {
                    put("userId", name) // Postman에서 사용하는 필드명 맞춤
                    put("password", password)
                    put("nickname", nickname)
                    put("email", email)
                }
                val jsonData = jsonObject.toString()
                Log.d(TAG, "서버 요청 데이터: $jsonData")

                // OkHttp 클라이언트 생성
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                // 요청 생성
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonData.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/auth/register") // 에뮬레이터에서 localhost 대신 10.0.2.2 사용
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .build()

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "서버 응답: ${response.code} - $responseBody")

                    // UI 스레드에서 결과 처리
                    runOnUiThread {
                        signUpButton.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this, "회원가입이 완료되었습니다!", Toast.LENGTH_LONG).show()
                            // 로그인 화면으로 이동
                            val intent = Intent(this, SignInActivity::class.java)
                            startActivity(intent)
                            finish() // 현재 화면 종료
                        } else {
                            // 서버 오류 처리
                            val errorMessage = when (response.code) {
                                409 -> "이미 등록된 이메일입니다."
                                400 -> "요청 데이터가 올바르지 않습니다."
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                403 -> "접근이 거부되었습니다. 서버 설정을 확인하세요."
                                else -> "회원가입 실패: ${response.code}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)

                // UI 스레드에서 오류 처리
                runOnUiThread {
                    signUpButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "서버 연결에 실패했습니다. 인터넷 연결을 확인하세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "예외 발생: ${e.message}", e)

                // UI 스레드에서 오류 처리
                runOnUiThread {
                    signUpButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}