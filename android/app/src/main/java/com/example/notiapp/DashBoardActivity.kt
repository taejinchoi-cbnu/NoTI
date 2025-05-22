package com.example.notiapp

import android.app.ProgressDialog
import android.content.Intent
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

// TODO: server에만 있는 파일 다운로드 가능하게 만들기
class DashBoardActivity : AppCompatActivity() {

    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var fileCountText: TextView
    private lateinit var emptyStateLayout: LinearLayout

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
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)
        fileCountText = findViewById(R.id.fileCountText)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
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

            // UI 스레드에서 어댑터 업데이트
            runOnUiThread {
                updateUI(combinedRecordings)
            }
        }
    }

    private fun updateUI(recordings: List<RecordingItem>) {
        recordingsAdapter.updateRecordings(recordings)

        // 파일 개수 업데이트
        val localCount = recordings.count { !it.isServerFile }
        val serverCount = recordings.count { it.isServerFile }
        val totalCount = recordings.size

        fileCountText.text = when {
            totalCount == 0 -> "파일 없음"
            localCount > 0 && serverCount > 0 -> "총 ${totalCount}개 (로컬 ${localCount}개, 서버 ${serverCount}개)"
            localCount > 0 -> "총 ${totalCount}개 (로컬 파일)"
            serverCount > 0 -> "총 ${totalCount}개 (서버 파일)"
            else -> "총 ${totalCount}개 파일"
        }

        // 빈 상태 처리
        if (recordings.isEmpty()) {
            recordingsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
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