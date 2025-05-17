package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.app.ActivityOptions
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashBoardActivity : AppCompatActivity() {

    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recordingsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 녹음하러가기 버튼 설정
        val goToRecButton: Button = findViewById(R.id.goToRec)
        goToRecButton.setOnClickListener {
            val intent = Intent(this, RecordActivity::class.java)
            startActivity(intent)
        }

        // RecyclerView 설정
        setupRecyclerView()

        // 저장된 녹음 파일 로드
        loadRecordings()

        // 하단 네비게이션 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.navigation_home

        // 네비게이션 아이템 클릭 리스너
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 이미 홈 화면이므로 아무 작업 없음
                    true
                }
                R.id.navigation_chatbot -> {
                    // 나만의 비서 화면으로 이동 (오른쪽으로 이동)
                    val intent = Intent(this, ChatbotActivity::class.java)
                    startActivity(intent,
                        ActivityOptions.makeCustomAnimation(this,
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        ).toBundle()
                    )
                    true
                }
                R.id.navigation_user -> {
                    // 내정보 화면으로 이동 (오른쪽으로 이동)
                    val intent = Intent(this, UserInfoActivity::class.java)
                    startActivity(intent,
                        ActivityOptions.makeCustomAnimation(this,
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        ).toBundle()
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)

        recordingsAdapter = RecordingsAdapter(mutableListOf()) { recording ->
            // 삭제 처리
            deleteRecording(recording)
        }

        recordingsRecyclerView.adapter = recordingsAdapter
    }

    private fun loadRecordings() {
        val recordings = mutableListOf<RecordingItem>()

        // 앱 내부 저장소 내 녹음 파일 디렉토리
        val recordingsDir = File(getExternalFilesDir(null), "recordings")
        if (recordingsDir.exists()) {
            val recordingFiles = recordingsDir.listFiles()
            recordingFiles?.forEach { file ->
                // 파일 정보 추출
                val filename = file.name
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(file.lastModified()))

                // 녹음 길이 추출 (간단히 파일 크기 기반으로 추정)
                // 근데 MediaMetadataRetriever를 사용하여 정확한 길이를 가져오는 것이 좋음
                val durationSec = file.length() / 1024 / 16
                val duration = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

                recordings.add(
                    RecordingItem(
                        file = file,
                        filename = filename,
                        date = date,
                        duration = duration,
                        filePath = file.absolutePath
                    )
                )
            }
        }

        // 날짜 기준 정렬 (최신 순)
        recordings.sortByDescending { it.file.lastModified() }

        // 어댑터 업데이트
        recordingsAdapter.updateRecordings(recordings)
    }

    private fun deleteRecording(recording: RecordingItem) {
        val file = File(recording.filePath)
        if (file.exists() && file.delete()) {
            loadRecordings() // 목록 갱신
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 표시될 때마다 녹음 목록 새로고침
        loadRecordings()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}