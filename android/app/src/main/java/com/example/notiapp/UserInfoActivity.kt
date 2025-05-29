package com.example.notiapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.app.ActivityOptions
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.ProgressBar
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class UserInfo(
    val userId: String,
    val nickname: String,
    val email: String
)

class UserInfoActivity : AppCompatActivity() {

    private val TAG = "UserInfoActivity"

    private lateinit var logoutButton: Button
    private lateinit var userIdText: TextView
    private lateinit var nicknameText: TextView
    private lateinit var emailText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var editToggleButton: Button
    private lateinit var saveButton: Button
    private lateinit var userIdEditText: EditText
    private lateinit var nicknameEditText: EditText
    private lateinit var emailEditText: EditText

    private var isEditMode = false
    private var currentUserInfo: UserInfo? = null

    // HTTP 클라이언트 (기존 프로젝트 패턴과 동일)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소 초기화
        initializeViews()

        // 사용자 정보 로드
        fetchUserInfo()

        // 로그아웃 버튼 초기화 및 클릭 리스너 설정
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            showLogoutConfirmDialog()
        }

        // 네비게이션 바의 하단 패딩을 제한
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val maxBottomPadding = resources.getDimensionPixelSize(R.dimen.max_bottom_padding) // 5dp
            val bottomPadding = minOf(systemInsets.bottom, maxBottomPadding)
            view.setPadding(0, 0, 0, bottomPadding)
            insets
        }

        // 하단 네비게이션 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.navigation_user // 현재 화면은 내정보 탭으로 설정

        // 네비게이션 아이템 클릭 리스너
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 홈 화면으로 이동 (왼쪽으로 이동)
                    val intent = Intent(this, DashBoardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent,
                        ActivityOptions.makeCustomAnimation(this,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        ).toBundle()
                    )
                    true
                }
                R.id.navigation_chatbot -> {
                    // 챗봇 화면으로 이동 (왼쪽으로 이동)
                    val intent = Intent(this, ChatbotActivity::class.java)
                    startActivity(intent,
                        ActivityOptions.makeCustomAnimation(this,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        ).toBundle()
                    )
                    true
                }
                R.id.navigation_user -> {
                    // 이미 내정보 화면이므로 아무 작업 없음
                    true
                }
                else -> false
            }
        }
    }

    // UI 요소들 초기화
    private fun initializeViews() {
        // 기존 요소들
        userIdText = findViewById(R.id.userIdText)
        nicknameText = findViewById(R.id.nicknameText)
        emailText = findViewById(R.id.emailText)
        progressBar = findViewById(R.id.progressBar)

        // 새로 추가된 요소들
        editToggleButton = findViewById(R.id.editToggleButton)
        saveButton = findViewById(R.id.saveButton)
        userIdEditText = findViewById(R.id.userIdEditText)
        nicknameEditText = findViewById(R.id.nicknameEditText)
        emailEditText = findViewById(R.id.emailEditText)

        // 편집 토글 버튼 클릭 리스너
        editToggleButton.setOnClickListener {
            toggleEditMode()
        }

        // 저장 버튼 클릭 리스너
        saveButton.setOnClickListener {
            saveUserInfo()
        }
    }

    // 로딩 상태 표시/숨김
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    // 서버에서 사용자 정보 가져오기 (기존 프로젝트 패턴과 동일)
    private fun fetchUserInfo() {
        // 로딩 표시
        showLoading(true)

        // JWT 토큰 가져오기
        val token = getJwtToken()
        if (token.isEmpty()) {
            showLoading(false)
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            navigateToSignIn()
            return
        }

        // 백그라운드 스레드에서 네트워크 요청
        thread {
            try {
                // POST 요청 생성 (빈 body) - 기존 프로젝트 패턴과 동일
                val requestBody = FormBody.Builder().build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/auth/get/user-information")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                Log.d(TAG, "사용자 정보 요청 URL: ${request.url}")
                Log.d(TAG, "Authorization 헤더: ${request.header("Authorization")?.substring(0, 20)}...")

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "사용자 정보 응답: ${response.code} - $responseBody")

                    // UI 스레드에서 결과 처리
                    runOnUiThread {
                        showLoading(false)

                        if (response.isSuccessful && responseBody != null) {
                            try {
                                parseAndDisplayUserInfo(responseBody)
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON 파싱 오류: ${e.message}", e)
                                showErrorMessage("사용자 정보 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            handleApiError(response.code)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    showLoading(false)
                    showErrorMessage("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    // JSON 파싱 및 UI 업데이트
    private fun parseAndDisplayUserInfo(responseBody: String) {
        try {
            val jsonObject = JSONObject(responseBody)

            val userInfo = UserInfo(
                userId = jsonObject.getString("userId"),
                nickname = jsonObject.getString("nickname"),
                email = jsonObject.getString("email")
            )

            // UI에 사용자 정보 표시
            displayUserInfo(userInfo)

            Log.d(TAG, "사용자 정보 로드 완료: $userInfo")

        } catch (e: Exception) {
            Log.e(TAG, "사용자 정보 파싱 실패: ${e.message}", e)
            showErrorMessage("사용자 정보를 불러올 수 없습니다.")
        }
    }

    // UI에 사용자 정보 표시
    private fun displayUserInfo(userInfo: UserInfo) {
        // 현재 사용자 정보 저장
        currentUserInfo = userInfo

        // TextView에 정보 표시
        userIdText.text = userInfo.userId
        nicknameText.text = userInfo.nickname
        emailText.text = userInfo.email

        // EditText에도 정보 설정 (편집 모드 대비)
        userIdEditText.setText(userInfo.userId)
        nicknameEditText.setText(userInfo.nickname)
        emailEditText.setText(userInfo.email)
    }

    // 편집 모드 토글
    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            // 편집 모드로 전환
            enterEditMode()
        } else {
            // 읽기 모드로 전환 (변경사항 취소)
            exitEditMode()
        }
    }

    // 편집 모드 진입
    private fun enterEditMode() {
        // TextView 숨기기
        userIdText.visibility = View.GONE
        nicknameText.visibility = View.GONE
        emailText.visibility = View.GONE

        // EditText 표시
        userIdEditText.visibility = View.VISIBLE
        nicknameEditText.visibility = View.VISIBLE
        emailEditText.visibility = View.VISIBLE

        // 저장 버튼 표시
        saveButton.visibility = View.VISIBLE

        // 편집 버튼 텍스트 변경
        editToggleButton.text = "취소"

        Log.d(TAG, "편집 모드 진입")
    }

    // 편집 모드 종료
    private fun exitEditMode() {
        // EditText 숨기기
        userIdEditText.visibility = View.GONE
        nicknameEditText.visibility = View.GONE
        emailEditText.visibility = View.GONE

        // TextView 표시
        userIdText.visibility = View.VISIBLE
        nicknameText.visibility = View.VISIBLE
        emailText.visibility = View.VISIBLE

        // 저장 버튼 숨기기
        saveButton.visibility = View.GONE

        // 편집 버튼 텍스트 변경
        editToggleButton.text = "편집"

        // 원래 값으로 복원 (변경사항 취소)
        currentUserInfo?.let { userInfo ->
            userIdEditText.setText(userInfo.userId)
            nicknameEditText.setText(userInfo.nickname)
            emailEditText.setText(userInfo.email)
        }

        Log.d(TAG, "편집 모드 종료")
    }

    // 입력 유효성 검사
    private fun validateUserInput(): Boolean {
        val userId = userIdEditText.text.toString().trim()
        val nickname = nicknameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        // 빈 필드 확인
        if (userId.isEmpty()) {
            userIdEditText.error = "아이디를 입력해주세요"
            userIdEditText.requestFocus()
            return false
        }

        if (nickname.isEmpty()) {
            nicknameEditText.error = "닉네임을 입력해주세요"
            nicknameEditText.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            emailEditText.error = "이메일을 입력해주세요"
            emailEditText.requestFocus()
            return false
        }

        // 이메일 형식 확인 (기존 SignUpActivity 패턴 사용)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "유효한 이메일 형식이 아닙니다"
            emailEditText.requestFocus()
            return false
        }

        Log.d(TAG, "입력 유효성 검사 통과")
        return true
    }

    // 사용자 정보 저장 (서버에 업데이트)
    private fun saveUserInfo() {
        // 입력 유효성 검사
        if (!validateUserInput()) {
            return
        }

        // 로딩 표시
        showLoading(true)

        // JWT 토큰 가져오기
        val token = getJwtToken()
        if (token.isEmpty()) {
            showLoading(false)
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            navigateToSignIn()
            return
        }

        // 입력된 정보 가져오기
        val userId = userIdEditText.text.toString().trim()
        val nickname = nicknameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        Log.d(TAG, "사용자 정보 업데이트 시작: userId=$userId, nickname=$nickname, email=$email")

        // 백그라운드 스레드에서 네트워크 요청
        thread {
            try {
                // POST 요청 본문 생성 (FormBody 사용 - 기존 프로젝트 패턴과 동일)
                val requestBody = FormBody.Builder()
                    .add("userId", userId)
                    .add("nickname", nickname)
                    .add("email", email)
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/auth/update/user-information")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                Log.d(TAG, "사용자 정보 업데이트 요청 URL: ${request.url}")
                Log.d(TAG, "Authorization 헤더: ${request.header("Authorization")?.substring(0, 20)}...")

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "사용자 정보 업데이트 응답: ${response.code} - $responseBody")

                    // UI 스레드에서 결과 처리
                    runOnUiThread {
                        showLoading(false)

                        if (response.isSuccessful) {
                            // 업데이트 성공
                            Toast.makeText(this@UserInfoActivity, "사용자 정보가 성공적으로 업데이트되었습니다!", Toast.LENGTH_SHORT).show()

                            // 현재 사용자 정보 업데이트
                            val updatedUserInfo = UserInfo(userId, nickname, email)
                            displayUserInfo(updatedUserInfo)

                            // 편집 모드 종료
                            isEditMode = false
                            exitEditMode()

                            Log.d(TAG, "사용자 정보 업데이트 완료")
                        } else {
                            // 업데이트 실패
                            handleUpdateError(response.code, responseBody)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 업데이트 네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@UserInfoActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 업데이트 에러 처리
    private fun handleUpdateError(code: Int, responseBody: String?) {
        val errorMessage = when (code) {
            400 -> "입력된 정보가 올바르지 않습니다."
            401 -> {
                clearJwtToken()
                "인증이 만료되었습니다. 다시 로그인해주세요."
            }
            403 -> "정보 수정 권한이 없습니다."
            409 -> "이미 사용 중인 아이디 또는 이메일입니다."
            500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
            else -> "정보 업데이트에 실패했습니다. (오류 코드: $code)"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

        // 401 오류 시 로그인 화면으로 이동
        if (code == 401) {
            navigateToSignIn()
        }

        Log.e(TAG, "사용자 정보 업데이트 실패: $code - $responseBody")
    }

    // 에러 메시지 표시
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // 오류 시 기본값 표시
        userIdText.text = "정보를 불러올 수 없음"
        nicknameText.text = "정보를 불러올 수 없음"
        emailText.text = "정보를 불러올 수 없음"
    }

    // API 에러 처리 (기존 프로젝트 패턴과 동일)
    private fun handleApiError(code: Int) {
        val errorMessage = when (code) {
            401 -> {
                // 토큰 만료 시 자동으로 로그인 화면으로 이동
                "인증이 만료되었습니다. 다시 로그인해주세요."
            }
            403 -> "접근 권한이 없습니다."
            404 -> "사용자 정보를 찾을 수 없습니다."
            500 -> "서버 오류가 발생했습니다."
            else -> "사용자 정보를 가져올 수 없습니다. (오류 코드: $code)"
        }

        showErrorMessage(errorMessage)

        // 401 또는 403 오류 시 토큰 삭제 후 로그인 화면으로 이동
        if (code == 401 || code == 403) {
            clearJwtToken() // 토큰 삭제
            navigateToSignIn() // 로그인 화면으로 이동
        }
    }

    // JWT 토큰 가져오기 (기존 프로젝트 패턴과 동일)
    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    // 로그아웃 확인 메세지
    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                // 확인 버튼 클릭 시 실제 로그아웃 수행
                performLogout()
            }
            .setNegativeButton("취소") { dialog, _ ->
                // 취소 버튼 클릭 시 다이얼로그만 닫기
                dialog.dismiss()
            }
            .setCancelable(true) // 다이얼로그 외부 터치로도 취소 가능
            .show()
    }

    // 실제로 로그아웃 수행하는 함수
    private fun performLogout() {
        // 1. JWT 토큰 삭제 (SharedPreferences에서 제거)
        clearJwtToken()

        // 2. 로그아웃 완료 메시지 표시
        Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()

        // 3. 로그인 화면으로 이동
        navigateToSignIn()
    }

    // JWT 토큰 삭제 (기존 프로젝트 패턴과 동일)
    private fun clearJwtToken() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("jwt_token") // "jwt_token" 키로 저장된 토큰 삭제
            .apply() // 비동기적으로 변경사항 적용
    }

    // 로그인 화면으로 이동 (기존 프로젝트 패턴과 동일)
    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
}