package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.app.ActivityOptions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
}