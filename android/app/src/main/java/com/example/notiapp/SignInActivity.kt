package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class SignInActivity : AppCompatActivity() {

    private val TAG = "SignInActivity"

    // UI 요소 변수 선언
    private lateinit var userIdEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button
    private lateinit var findPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        // UI 요소 초기화
        userIdEditText = findViewById(R.id.signInUserId)
        passwordEditText = findViewById(R.id.signInPassword)
        signInButton = findViewById(R.id.signInBtn)
        signUpButton = findViewById(R.id.signUpBtn)
        findPasswordButton = findViewById(R.id.findPasswordBtn)

        // 회원가입에서 넘어온 userId가 있으면 자동 입력
        val userIdFromSignUp = intent.getStringExtra("userId")
        if (!userIdFromSignUp.isNullOrEmpty()) {
            userIdEditText.setText(userIdFromSignUp)
        }

        // 회원가입 버튼 클릭 리스너
        signUpButton.setOnClickListener {
            // 회원가입 페이지로 이동
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // 로그인 버튼 클릭 리스너
        signInButton.setOnClickListener {
            validateAndSignIn()
        }

        // 비밀번호 찾기 버튼 클릭 리스너
        findPasswordButton.setOnClickListener {
            // 비밀번호 찾기 기능은 아직 구현하지 않음
            Toast.makeText(this, "비밀번호 찾기 기능은 추후 구현 예정입니다.", Toast.LENGTH_SHORT).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.SignInmain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateAndSignIn() {
        val userId = userIdEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        Log.d(TAG, "로그인 시도: userId=$userId")

        // 빈 필드 확인
        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()

            if (userId.isEmpty()) {
                userIdEditText.error = "아이디를 입력해주세요"
            }
            if (password.isEmpty()) {
                passwordEditText.error = "비밀번호를 입력해주세요"
            }
            return
        }

        // 로그인 시도
        signIn(userId, password)
    }

    private fun signIn(userId: String, password: String) {
        // 로딩 표시 등 UI 업데이트
        signInButton.isEnabled = false
        Toast.makeText(this, "로그인 중...", Toast.LENGTH_SHORT).show()

        // 백그라운드 스레드에서 네트워크 요청 수행
        thread {
            try {
                // JSON 데이터 생성 - 서버 API 스펙에 맞게 필드명 설정
                val jsonObject = JSONObject().apply {
                    put("userId", userId)
                    put("password", password)
                }
                val jsonData = jsonObject.toString()
                Log.d(TAG, "서버 요청 데이터: $jsonData")

                // OkHttp 클라이언트 생성
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                // 요청 생성
                // JSON으로 요청을 보내는 경우 (현재 코드와 유사)
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonData.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/auth/login")
                    .post(requestBody)
                    .header("Content-Type", "application/json") // JSON 형식으로 일치
                    .build()

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "서버 응답: ${response.code} - $responseBody")

                    // UI 스레드에서 결과 처리
                    runOnUiThread {
                        signInButton.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                            // 로그인 성공 시 메인 화면으로 이동
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // 현재 화면 종료
                        } else {
                            // 서버 오류 처리
                            val errorMessage = when (response.code) {
                                401 -> "아이디 또는 비밀번호가 일치하지 않습니다."
                                404 -> "서버를 찾을 수 없습니다."
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                else -> "로그인 실패: ${response.code}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)

                // UI 스레드에서 오류 처리
                runOnUiThread {
                    signInButton.isEnabled = true
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
                    signInButton.isEnabled = true
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