package com.example.notiapp

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.app.ActivityOptions
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class DashBoardActivity : AppCompatActivity() {

    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var fileCountText: TextView
    private lateinit var emptyStateLayout: LinearLayout

    // 새로 추가할 변수들
    private lateinit var filterAllButton: Button
    private lateinit var filterLocalButton: Button
    private lateinit var filterServerButton: Button

    // 필터링 상태 관리
    private var currentFilter = FilterType.ALL
    private var allRecordings = mutableListOf<RecordingItem>() // 원본 데이터 저장용

    // 필터 타입 enum
    enum class FilterType {
        ALL,      // 전체
        LOCAL,    // 내 기기 (로컬 + 다운로드된 서버 파일)
        SERVER    // 서버만 (다운로드되지 않은 서버 파일)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 네비게이션 바의 하단 패딩 제한
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val maxBottomPadding = resources.getDimensionPixelSize(R.dimen.max_bottom_padding) // 5dp
            val bottomPadding = minOf(systemInsets.bottom, maxBottomPadding)
            view.setPadding(0, 0, 0, bottomPadding)
            insets
        }

        // UI 요소 초기화
        initializeViews()

        // 카드 클릭 리스너 설정
        setupDashboardCards()

        // 필터 버튼 설정
        setupFilterButtons()

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
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // 기존 요소들
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)
        fileCountText = findViewById(R.id.fileCountText)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // 새로 추가되는 필터 버튼들
        filterAllButton = findViewById(R.id.filterAllButton)
        filterLocalButton = findViewById(R.id.filterLocalButton)
        filterServerButton = findViewById(R.id.filterServerButton)
    }

    // TODO: Notion 링크로 연결해줘야함
    private fun setupDashboardCards() {
        // 카드 1 - Google 홈페이지
        findViewById<CardView>(R.id.dashboardCard1).setOnClickListener {
            openUrlInBrowser("https://www.google.com")
        }

        // 카드 2 - Google 홈페이지 (개발 문서 - 임시)
        findViewById<CardView>(R.id.dashboardCard2).setOnClickListener {
            openUrlInBrowser("https://www.google.com")
        }

        // 카드 3 - Google 홈페이지 (설정 가이드 - 임시)
        findViewById<CardView>(R.id.dashboardCard3).setOnClickListener {
            openUrlInBrowser("https://www.google.com")
        }

        // 카드 4 - Google 홈페이지 (지원 센터 - 임시)
        findViewById<CardView>(R.id.dashboardCard4).setOnClickListener {
            openUrlInBrowser("https://www.google.com")
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("DashBoardActivity", "URL 열기 실패: ${e.message}")
            Toast.makeText(this, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFilterButtons() {
        filterAllButton.setOnClickListener {
            setActiveFilter(FilterType.ALL)
        }

        filterLocalButton.setOnClickListener {
            setActiveFilter(FilterType.LOCAL)
        }

        filterServerButton.setOnClickListener {
            setActiveFilter(FilterType.SERVER)
        }
    }

    private fun setActiveFilter(filterType: FilterType) {
        // 이전 필터와 같으면 무시
        if (currentFilter == filterType) return

        currentFilter = filterType

        // 버튼 상태 업데이트
        updateFilterButtonStyles()

        // 필터링된 데이터 적용
        applyFilter()
    }

    private fun updateFilterButtonStyles() {
        // 모든 버튼 비활성 상태로 설정
        filterAllButton.setBackgroundResource(R.drawable.filter_button_inactive)
        filterAllButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        filterLocalButton.setBackgroundResource(R.drawable.filter_button_inactive)
        filterLocalButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        filterServerButton.setBackgroundResource(R.drawable.filter_button_inactive)
        filterServerButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        // 선택된 버튼만 활성 상태로 설정
        when (currentFilter) {
            FilterType.ALL -> {
                filterAllButton.setBackgroundResource(R.drawable.filter_button_active)
                filterAllButton.setTextColor(resources.getColor(R.color.white, null))
            }
            FilterType.LOCAL -> {
                filterLocalButton.setBackgroundResource(R.drawable.filter_button_active)
                filterLocalButton.setTextColor(resources.getColor(R.color.white, null))
            }
            FilterType.SERVER -> {
                filterServerButton.setBackgroundResource(R.drawable.filter_button_active)
                filterServerButton.setTextColor(resources.getColor(R.color.white, null))
            }
        }
    }

    private fun applyFilter() {
        val filteredRecordings = when (currentFilter) {
            FilterType.ALL -> allRecordings.toList()
            FilterType.LOCAL -> allRecordings.filter { recording ->
                // 로컬 파일이거나 다운로드된 서버 파일
                !recording.isServerFile || (recording.isServerFile && recording.isDownloaded)
            }
            FilterType.SERVER -> allRecordings.filter { recording ->
                // 서버 파일이면서 다운로드되지 않은 파일
                recording.isServerFile && !recording.isDownloaded
            }
        }

        // UI 업데이트
        updateUI(filteredRecordings)
    }

    private fun setupRecyclerView() {
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)

        // 스크롤바 표시 설정
        recordingsRecyclerView.isVerticalScrollBarEnabled = true

        recordingsAdapter = RecordingsAdapter(
            mutableListOf(),
            { recording -> deleteRecording(recording) }, // 삭제 콜백
            { recording -> downloadServerFile(recording) } // 다운로드 콜백
        )

        recordingsRecyclerView.adapter = recordingsAdapter
    }

    private fun setupBottomNavigation() {
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

    private fun loadRecordings() {
        // 먼저 로컬 파일들을 로드
        val localRecordings = loadLocalRecordings()

        // 서버에서 파일 정보를 가져와서 병합
        fetchServerRecordings { serverRecordings ->
            val combinedRecordings = combineRecordings(localRecordings, serverRecordings)

            // 원본 데이터 저장
            allRecordings.clear()
            allRecordings.addAll(combinedRecordings)

            // UI 스레드에서 어댑터 업데이트
            runOnUiThread {
                applyFilter() // 현재 필터에 맞게 데이터 적용
            }
        }
    }

    private fun updateUI(recordings: List<RecordingItem>) {
        recordingsAdapter.updateRecordings(recordings)

        // 파일 개수 업데이트 (필터별로)
        val totalCount = recordings.size
        val filterText = when (currentFilter) {
            FilterType.ALL -> {
                val localCount = allRecordings.count { !it.isServerFile || (it.isServerFile && it.isDownloaded) }
                val serverCount = allRecordings.count { it.isServerFile && !it.isDownloaded }
                when {
                    totalCount == 0 -> "파일 없음"
                    localCount > 0 && serverCount > 0 -> "총 ${totalCount}개 (로컬 ${localCount}개, 서버 ${serverCount}개)"
                    localCount > 0 -> "총 ${totalCount}개 (로컬 파일)"
                    serverCount > 0 -> "총 ${totalCount}개 (서버 파일)"
                    else -> "총 ${totalCount}개 파일"
                }
            }
            FilterType.LOCAL -> if (totalCount == 0) "내 기기에 파일 없음" else "내 기기: ${totalCount}개 파일"
            FilterType.SERVER -> if (totalCount == 0) "서버에 파일 없음" else "서버: ${totalCount}개 파일"
        }

        fileCountText.text = filterText

        // 빈 상태 처리
        if (recordings.isEmpty()) {
            recordingsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE

            // 빈 상태 메시지도 필터에 따라 변경
            val emptyMessageView = emptyStateLayout.getChildAt(1) as? TextView
            if (emptyMessageView != null) {
                emptyMessageView.text = when (currentFilter) {
                    FilterType.ALL -> "저장된 녹음 파일이 없습니다"
                    FilterType.LOCAL -> "내 기기에 저장된 파일이 없습니다"
                    FilterType.SERVER -> "서버에만 저장된 파일이 없습니다"
                }
            }
        } else {
            recordingsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun loadLocalRecordings(): MutableList<RecordingItem> {
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
                val durationSec = file.length() / 1024 / 16
                val duration = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

                recordings.add(
                    RecordingItem(
                        file = file,
                        filename = filename,
                        date = date,
                        duration = duration,
                        filePath = file.absolutePath,
                        isServerFile = false,
                        isDownloaded = true // 로컬 파일은 항상 다운로드된 상태
                    )
                )
            }
        }

        return recordings
    }

    private fun fetchServerRecordings(callback: (List<RecordingItem>) -> Unit) {
        // JWT 토큰 가져오기
        val token = getJwtToken()
        if (token.isEmpty()) {
            Log.w("DashBoardActivity", "JWT 토큰이 없어 서버 파일을 가져올 수 없습니다")
            callback(emptyList())
            return
        }

        // 백그라운드 스레드에서 서버 요청
        thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                // POST 요청 생성 (빈 body)
                val requestBody = FormBody.Builder().build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/file/get/file-information")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d("DashBoardActivity", "서버 파일 목록 응답: ${response.code} - $responseBody")

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val serverRecordings = parseServerResponse(responseBody)
                            callback(serverRecordings)
                        } catch (e: Exception) {
                            Log.e("DashBoardActivity", "서버 응답 파싱 오류: ${e.message}", e)
                            callback(emptyList())
                        }
                    } else {
                        Log.e("DashBoardActivity", "서버 파일 목록 가져오기 실패: ${response.code}")
                        callback(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e("DashBoardActivity", "서버 요청 오류: ${e.message}", e)
                callback(emptyList())
            }
        }
    }

    private fun parseServerResponse(responseBody: String): List<RecordingItem> {
        val serverRecordings = mutableListOf<RecordingItem>()

        try {
            val jsonArray = JSONArray(responseBody)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val originalName = jsonObject.getString("originalName")
                val savedName = jsonObject.getString("savedName")
                val uploadDate = jsonObject.getString("uploadDate")
                val duration = jsonObject.getInt("duration")
                val fileSize = jsonObject.getLong("fileSize")

                // 업로드 날짜를 파싱하여 표시용 날짜로 변환
                val displayDate = try {
                    val serverDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                        .parse(uploadDate)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(serverDate ?: Date())
                } catch (e: Exception) {
                    uploadDate.split("T")[0] // 간단히 날짜 부분만 추출
                }

                // 지속 시간을 포맷팅
                val formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60)

                // 로컬에 다운로드된 파일이 있는지 확인
                val downloadedFile = checkIfDownloaded(savedName)

                serverRecordings.add(
                    RecordingItem(
                        file = downloadedFile, // 다운로드된 파일이 있으면 File 객체, 없으면 null
                        filename = originalName,
                        date = displayDate,
                        duration = formattedDuration,
                        filePath = downloadedFile?.absolutePath ?: "", // 다운로드된 파일의 경로
                        isServerFile = true,
                        savedFileName = savedName,
                        fileSize = fileSize,
                        uploadDate = uploadDate,
                        isDownloaded = downloadedFile != null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("DashBoardActivity", "JSON 파싱 오류: ${e.message}", e)
        }

        return serverRecordings
    }

    private fun checkIfDownloaded(savedFileName: String): File? {
        val downloadsDir = File(getExternalFilesDir(null), "downloads")
        if (downloadsDir.exists()) {
            val downloadedFile = File(downloadsDir, savedFileName)
            return if (downloadedFile.exists()) downloadedFile else null
        }
        return null
    }

    private fun combineRecordings(
        localRecordings: List<RecordingItem>,
        serverRecordings: List<RecordingItem>
    ): List<RecordingItem> {
        val combinedList = mutableListOf<RecordingItem>()

        // 로컬 파일들을 먼저 추가
        combinedList.addAll(localRecordings)

        // 서버 파일들 중 로컬에 없는 것만 추가
        serverRecordings.forEach { serverFile ->
            val existsLocally = localRecordings.any { localFile ->
                localFile.filename == serverFile.filename
            }

            if (!existsLocally) {
                combinedList.add(serverFile)
            }
        }

        // 날짜 기준 정렬 (최신 순)
        return combinedList.sortedByDescending { recording ->
            if (recording.isServerFile) {
                // 서버 파일의 경우 uploadDate 사용
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                        .parse(recording.uploadDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else {
                // 로컬 파일의 경우 파일 수정 시간 사용
                recording.file?.lastModified() ?: 0L
            }
        }
    }

    private fun downloadServerFile(recording: RecordingItem) {
        if (!recording.isServerFile) return

        val progressDialog = ProgressDialog(this).apply {
            setMessage("파일 다운로드 중...")
            setCancelable(false)
            show()
        }

        // JWT 토큰 가져오기
        val token = getJwtToken()
        if (token.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                // 서버 파일 다운로드 API 호출 아직 url 없음
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/${recording.savedFileName}")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        if (responseBody != null) {
                            // 다운로드 디렉토리 생성
                            val downloadsDir = File(getExternalFilesDir(null), "downloads")
                            if (!downloadsDir.exists()) {
                                downloadsDir.mkdirs()
                            }

                            // 파일 저장
                            val downloadedFile = File(downloadsDir, recording.savedFileName)
                            saveFileFromResponse(responseBody, downloadedFile)

                            runOnUiThread {
                                progressDialog.dismiss()
                                Toast.makeText(this@DashBoardActivity, "다운로드 완료: ${recording.filename}", Toast.LENGTH_SHORT).show()

                                // 어댑터에 다운로드 상태 업데이트
                                recordingsAdapter.updateRecordingDownloadStatus(recording.savedFileName, downloadedFile.absolutePath)
                            }
                        } else {
                            runOnUiThread {
                                progressDialog.dismiss()
                                Toast.makeText(this@DashBoardActivity, "다운로드 실패: 응답 본문이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            val errorMessage = when (response.code) {
                                401 -> "인증이 만료되었습니다. 다시 로그인해주세요."
                                404 -> "서버에서 파일을 찾을 수 없습니다."
                                else -> "다운로드 실패: ${response.code}"
                            }
                            Toast.makeText(this@DashBoardActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashBoardActivity", "다운로드 오류: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@DashBoardActivity, "다운로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFileFromResponse(responseBody: ResponseBody, file: File) {
        try {
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            Log.e("DashBoardActivity", "파일 저장 오류: ${e.message}", e)
            throw e
        }
    }

    // JWT 토큰 가져오기 메서드
    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    private fun deleteRecording(recording: RecordingItem) {
        if (recording.isServerFile) {
            // 서버 파일인 경우: 목록에서만 제거 (실제 서버 삭제 API 필요시 추가)
            // 다운로드된 로컬 파일도 삭제
            if (recording.isDownloaded && recording.filePath.isNotEmpty()) {
                val downloadedFile = File(recording.filePath)
                if (downloadedFile.exists()) {
                    downloadedFile.delete()
                }
            }
            loadRecordings() // 목록 새로고침
            Toast.makeText(this, "목록에서 제거되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 로컬 파일인 경우: 실제 파일 삭제
            val file = File(recording.filePath)
            if (file.exists() && file.delete()) {
                loadRecordings() // 목록 갱신
                Toast.makeText(this, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "파일 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
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