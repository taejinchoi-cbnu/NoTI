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

class UserInfoActivity : AppCompatActivity() {

    // TODO: DB에서 userdata 가져오는 API 필요. data 수정도 구현..?

    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 로그아웃 버튼 초기화 및 클릭 리스너 설정
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            showLogoutConfirmDialog()
        }

        // 네비게이션 바의 하단 패딩을 제한
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 하단 패딩을 5dp로 제한
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


    // JWT 토큰 삭제
    private fun clearJwtToken() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("jwt_token") // "jwt_token" 키로 저장된 토큰 삭제
            .apply() // 비동기적으로 변경사항 적용
    }
    
    // 로그인 화면으로 이동
    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
}