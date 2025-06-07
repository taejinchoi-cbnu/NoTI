package com.example.notiapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import android.content.Context

class ChatbotActivity : AppCompatActivity() {

    private val TAG = "ChatbotActivity"
    val serverIp = AddressAdmin.MY_SERVER_IP

    // UI 요소들
    private lateinit var chatTitleText: TextView
    private lateinit var backButton: ImageButton

    // 1. 파일 목록 화면
    private lateinit var fileListLayout: LinearLayout
    private lateinit var chatFileCountText: TextView
    private lateinit var chatFileRecyclerView: RecyclerView
    private lateinit var chatEmptyStateLayout: LinearLayout
    private lateinit var chatFileAdapter: ChatFileAdapter

    // 필터 버튼들
    private lateinit var chatFilterAllButton: Button
    private lateinit var chatFilterLocalButton: Button
    private lateinit var chatFilterServerButton: Button

    // 2. 세션 준비 화면
    private lateinit var sessionSetupLayout: LinearLayout
    private lateinit var selectedFileNameText: TextView
    private lateinit var setupStatusText: TextView
    private lateinit var createChatbotButton: Button
    private lateinit var sessionLoadingProgress: ProgressBar
    private lateinit var loadingStatusText: TextView

    // 3. 채팅 화면
    private lateinit var chatLayout: LinearLayout
    private lateinit var currentChatFileNameText: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    // 데이터
    private var allRecordings = mutableListOf<RecordingItem>()
    private var filteredRecordings = mutableListOf<RecordingItem>()
    private var currentSelectedFile: RecordingItem? = null
    private var currentSessionId: Long? = null
    private var chatMessages = mutableListOf<ChatMessage>()

    // 필터링 상태 관리
    private var currentFilter = FilterType.ALL

    // 필터 타입 enum
    enum class FilterType {
        ALL,      // 전체
        LOCAL,    // 내 기기 (로컬 + 다운로드된 서버 파일)
        SERVER    // 서버만 (다운로드되지 않은 서버 파일)
    }

    // 상태 관리
    private enum class ScreenState {
        FILE_LIST,      // 파일 목록
        SESSION_SETUP,  // 세션 준비
        CHATTING       // 채팅 중
    }
    private var currentState = ScreenState.FILE_LIST
    private lateinit var sessionManager: ChatSessionManager

    // HTTP 클라이언트
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val imm: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소 초기화
        initializeViews()
        sessionManager = ChatSessionManager(this)

        // 어댑터 설정
        setupAdapters()

        // 버튼 리스너 설정
        setupButtonListeners()

        // 하단 네비게이션 설정
        setupBottomNavigation()

        // 파일 목록 로드
        loadRecordingFiles()
    }

    private fun initializeViews() {
        // 공통 UI
        chatTitleText = findViewById(R.id.chatTitleText)
        backButton = findViewById(R.id.backButton)

        // 1. 파일 목록 화면
        fileListLayout = findViewById(R.id.fileListLayout)
        chatFileCountText = findViewById(R.id.chatFileCountText)
        chatFileRecyclerView = findViewById(R.id.chatFileRecyclerView)
        chatEmptyStateLayout = findViewById(R.id.chatEmptyStateLayout)

        // 필터 버튼들
        chatFilterAllButton = findViewById(R.id.chatFilterAllButton)
        chatFilterLocalButton = findViewById(R.id.chatFilterLocalButton)
        chatFilterServerButton = findViewById(R.id.chatFilterServerButton)

        // 2. 세션 준비 화면
        sessionSetupLayout = findViewById(R.id.sessionSetupLayout)
        selectedFileNameText = findViewById(R.id.selectedFileNameText)
        setupStatusText = findViewById(R.id.setupStatusText)
        createChatbotButton = findViewById(R.id.createChatbotButton)
        sessionLoadingProgress = findViewById(R.id.sessionLoadingProgress)
        loadingStatusText = findViewById(R.id.loadingStatusText)

        // 3. 채팅 화면
        chatLayout = findViewById(R.id.chatLayout)
        currentChatFileNameText = findViewById(R.id.currentChatFileNameText)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupAdapters() {
        // 파일 목록 어댑터
        chatFileAdapter = ChatFileAdapter(filteredRecordings, { recordingItem ->
            onFileSelected(recordingItem)
        }, sessionManager)

        chatFileRecyclerView.layoutManager = LinearLayoutManager(this)
        chatFileRecyclerView.adapter = chatFileAdapter

        // 채팅 어댑터
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupButtonListeners() {
        // 뒤로가기 버튼
        backButton.setOnClickListener {
            when (currentState) {
                ScreenState.SESSION_SETUP -> {
                    showFileListScreen()
                }
                ScreenState.CHATTING -> {
                    showFileListScreen()
                }
                else -> {
                    // 파일 목록에서는 뒤로가기 없음
                }
            }
        }

        // 챗봇 생성 버튼
        createChatbotButton.setOnClickListener {
            currentSelectedFile?.let { file ->
                createChatbotSession(file)
            }
        }

        // 메시지 전송 버튼
        sendButton.setOnClickListener {
            sendMessage()
        }

        // 엔터키로 메시지 전송
        messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // 필터 버튼 설정
        setupFilterButtons()
    }

    private fun setupFilterButtons() {
        chatFilterAllButton.setOnClickListener {
            setActiveFilter(FilterType.ALL)
        }

        chatFilterLocalButton.setOnClickListener {
            setActiveFilter(FilterType.LOCAL)
        }

        chatFilterServerButton.setOnClickListener {
            setActiveFilter(FilterType.SERVER)
        }
    }

    private fun setActiveFilter(filterType: FilterType) {
        if (currentFilter == filterType) return

        currentFilter = filterType
        updateFilterButtonStyles()
        applyFilter()
    }

    private fun updateFilterButtonStyles() {
        // 모든 버튼 비활성 상태로
        chatFilterAllButton.setBackgroundResource(R.drawable.filter_button_inactive)
        chatFilterAllButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        chatFilterLocalButton.setBackgroundResource(R.drawable.filter_button_inactive)
        chatFilterLocalButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        chatFilterServerButton.setBackgroundResource(R.drawable.filter_button_inactive)
        chatFilterServerButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        // 선택된 버튼만 활성 상태로
        when (currentFilter) {
            FilterType.ALL -> {
                chatFilterAllButton.setBackgroundResource(R.drawable.filter_button_active)
                chatFilterAllButton.setTextColor(resources.getColor(R.color.white, null))
            }
            FilterType.LOCAL -> {
                chatFilterLocalButton.setBackgroundResource(R.drawable.filter_button_active)
                chatFilterLocalButton.setTextColor(resources.getColor(R.color.white, null))
            }
            FilterType.SERVER -> {
                chatFilterServerButton.setBackgroundResource(R.drawable.filter_button_active)
                chatFilterServerButton.setTextColor(resources.getColor(R.color.white, null))
            }
        }
    }

    private fun applyFilter() {
        filteredRecordings = when (currentFilter) {
            FilterType.ALL -> allRecordings.toMutableList()
            FilterType.LOCAL -> allRecordings.filter { recording ->
                !recording.isServerFile || (recording.isServerFile && recording.isDownloaded)
            }.toMutableList()
            FilterType.SERVER -> allRecordings.filter { recording ->
                recording.isServerFile && !recording.isDownloaded
            }.toMutableList()
        }

        updateFilteredUI()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.navigation_chatbot

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DashBoardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.navigation_user -> {
                    val intent = Intent(this, UserInfoActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_chatbot -> {
                    // 이미 챗봇 화면
                    true
                }
                else -> false
            }
        }
    }

    // ========== 화면 전환 메서드들 ==========

    private fun showFileListScreen() {
        currentState = ScreenState.FILE_LIST

        fileListLayout.visibility = View.VISIBLE
        sessionSetupLayout.visibility = View.GONE
        chatLayout.visibility = View.GONE

        backButton.visibility = View.GONE
        chatTitleText.text = "AI 비서"

        // 파일 목록 새로고침하여 아이콘 업데이트
        chatFileAdapter.refreshSessionStatuses()

        Log.d(TAG, "파일 목록 화면 표시")
    }

    private fun showSessionSetupScreen(recordingItem: RecordingItem) {
        currentState = ScreenState.SESSION_SETUP
        currentSelectedFile = recordingItem

        fileListLayout.visibility = View.GONE
        sessionSetupLayout.visibility = View.VISIBLE
        chatLayout.visibility = View.GONE

        backButton.visibility = View.VISIBLE
        chatTitleText.text = "AI 비서 생성"
        selectedFileNameText.text = recordingItem.filename

        // 로딩 상태 초기화
        createChatbotButton.visibility = View.VISIBLE
        sessionLoadingProgress.visibility = View.GONE
        loadingStatusText.visibility = View.GONE

        Log.d(TAG, "세션 준비 화면 표시: ${recordingItem.filename}")
    }

    private fun showChatScreen(recordingItem: RecordingItem, sessionId: Long) {
        currentState = ScreenState.CHATTING
        currentSelectedFile = recordingItem
        currentSessionId = sessionId

        fileListLayout.visibility = View.GONE
        sessionSetupLayout.visibility = View.GONE
        chatLayout.visibility = View.VISIBLE

        backButton.visibility = View.VISIBLE
        chatTitleText.text = "AI 비서와 대화"
        currentChatFileNameText.text = recordingItem.filename

        Log.d(TAG, "채팅 화면 표시: ${recordingItem.filename}")
    }

    // ========== 데이터 로딩 메서드들 ==========

    private fun loadRecordingFiles() {
        // 먼저 로컬 파일들을 로드
        val localRecordings = loadLocalRecordings()

        // 서버에서 파일 정보를 가져와서 병합
        fetchServerRecordings { serverRecordings ->
            val combinedRecordings = combineRecordings(localRecordings, serverRecordings)

            runOnUiThread {
                updateFileListUI(combinedRecordings)
            }
        }
    }

    private fun loadLocalRecordings(): List<RecordingItem> {
        val recordings = mutableListOf<RecordingItem>()

        // SharedPreferences 인스턴스 생성
        val sharedPreferences = getSharedPreferences("recording_files", MODE_PRIVATE)

        // 앱 내부 저장소 내 녹음 파일 디렉토리
        val recordingsDir = File(getExternalFilesDir(null), "recordings")
        if (recordingsDir.exists()) {
            val recordingFiles = recordingsDir.listFiles()
            recordingFiles?.forEach { file ->
                val filename = file.name
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(file.lastModified()))

                // 녹음 길이 추출 (간단히 파일 크기 기반으로 추정)
                val durationSec = file.length() / 1024 / 16
                val duration = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

                // SharedPreferences에서 서버 저장 파일명(UUID 포함) 가져오기
                val serverSavedFileName = sharedPreferences.getString(filename, "") ?: ""

                // savedFileName이 비어있으면 원본 파일명 사용 (fallback)
                val finalSavedFileName = if (serverSavedFileName.isNotEmpty()) {
                    serverSavedFileName
                } else {
                    filename
                }

                recordings.add(
                    RecordingItem(
                        file = file,
                        filename = filename,
                        date = date,
                        duration = duration,
                        filePath = file.absolutePath,
                        isServerFile = false,
                        savedFileName = finalSavedFileName, // UUID가 포함된 서버 파일명
                        isDownloaded = true
                    )
                )

                Log.d(TAG, "로컬 파일 로드: $filename -> savedFileName: $finalSavedFileName")
            }
        }

        return recordings
    }

    private fun fetchServerRecordings(callback: (List<RecordingItem>) -> Unit) {
        val token = getJwtToken()
        if (token.isEmpty()) {
            Log.w(TAG, "JWT 토큰이 없어 서버 파일을 가져올 수 없습니다")
            callback(emptyList())
            return
        }

        thread {
            try {
                val requestBody = FormBody.Builder().build()
                val request = Request.Builder()
                    .url("http://${serverIp}/file/get/file-information")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "서버 파일 목록 응답: ${response.code}")

                    if (response.isSuccessful && responseBody != null) {
                        val recordings = parseServerFileResponse(responseBody)
                        callback(recordings)
                    } else {
                        callback(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "서버 파일 목록 로드 오류: ${e.message}", e)
                callback(emptyList())
            }
        }
    }

    private fun parseServerFileResponse(responseBody: String): List<RecordingItem> {
        val recordings = mutableListOf<RecordingItem>()

        try {
            val jsonArray = JSONArray(responseBody)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val originalName = jsonObject.getString("originalName")
                val savedName = jsonObject.getString("savedName")
                val uploadDate = jsonObject.getString("uploadDate")
                val duration = jsonObject.getInt("duration")
                val fileSize = jsonObject.getLong("fileSize")

                // 날짜 포맷팅
                val displayDate = try {
                    val serverDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                        .parse(uploadDate)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(serverDate ?: Date())
                } catch (e: Exception) {
                    uploadDate.split("T")[0]
                }

                val formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60)

                // 로컬에 다운로드된 파일이 있는지 확인
                val downloadedFile = checkIfDownloaded(savedName)

                recordings.add(
                    RecordingItem(
                        file = downloadedFile,
                        filename = originalName,
                        date = displayDate,
                        duration = formattedDuration,
                        filePath = downloadedFile?.absolutePath ?: "",
                        isServerFile = true,
                        savedFileName = savedName,
                        fileSize = fileSize,
                        uploadDate = uploadDate,
                        isDownloaded = downloadedFile != null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "파일 목록 파싱 오류: ${e.message}", e)
        }

        return recordings
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
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                        .parse(recording.uploadDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else {
                recording.file?.lastModified() ?: 0L
            }
        }
    }

    private fun updateFileListUI(recordings: List<RecordingItem>) {
        allRecordings.clear()
        allRecordings.addAll(recordings)

        // 필터 적용
        applyFilter()
    }

    private fun updateFilteredUI() {
        chatFileAdapter.updateRecordings(filteredRecordings)

        // 파일 개수 업데이트 (필터별로)
        val totalCount = filteredRecordings.size
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

        chatFileCountText.text = filterText

        // 빈 상태 처리
        if (filteredRecordings.isEmpty()) {
            chatFileRecyclerView.visibility = View.GONE
            chatEmptyStateLayout.visibility = View.VISIBLE

            // 빈 상태 메시지도 필터에 따라 변경
            val emptyMessageView = chatEmptyStateLayout.getChildAt(1) as? TextView
            emptyMessageView?.text = when (currentFilter) {
                FilterType.ALL -> "녹음 파일이 없습니다"
                FilterType.LOCAL -> "내 기기에 저장된 파일이 없습니다"
                FilterType.SERVER -> "서버에만 저장된 파일이 없습니다"
            }
        } else {
            chatFileRecyclerView.visibility = View.VISIBLE
            chatEmptyStateLayout.visibility = View.GONE
        }
    }

    // ========== 파일 선택 및 세션 관리 ==========

    private fun onFileSelected(recordingItem: RecordingItem) {
        Log.d(TAG, "파일 선택됨: ${recordingItem.filename}")

        // 기존 세션이 있는지 확인
        val existingSessionId = sessionManager.getSessionId(recordingItem.savedFileName)

        if (existingSessionId != null) {
            Log.d(TAG, "기존 세션 발견: $existingSessionId")
            // 바로 채팅 화면으로 이동
            loadChatHistory(existingSessionId) {
                showChatScreen(recordingItem, existingSessionId)
            }
        } else {
            Log.d(TAG, "새로운 세션 필요")
            // 세션 준비 화면으로 이동
            showSessionSetupScreen(recordingItem)
        }
    }

    private fun createChatbotSession(recordingItem: RecordingItem) {
        // 로딩 상태 표시
        createChatbotButton.visibility = View.GONE
        sessionLoadingProgress.visibility = View.VISIBLE
        loadingStatusText.visibility = View.VISIBLE
        loadingStatusText.text = "녹음 내용을 분석하고 있습니다..."

        val token = getJwtToken()
        if (token.isEmpty()) {
            showSessionError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("savedFileName", recordingItem.savedFileName)
                    .build()

                val request = Request.Builder()
                    .url("http://${serverIp}/chatbot/session")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val sessionId = jsonResponse.getLong("sessionId")
                                val status = jsonResponse.getString("sessionStatus")

                                // 세션 ID 저장
                                sessionManager.saveSession(recordingItem.savedFileName, sessionId)

                                when (status) {
                                    "READY" -> {
                                        loadChatHistory(sessionId) {
                                            showChatScreen(recordingItem, sessionId)
                                        }
                                    }
                                    "INITIALIZING" -> {
                                        checkSessionStatus(sessionId, recordingItem)
                                    }
                                    else -> {
                                        showSessionError("세션 생성에 실패했습니다.")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "세션 생성 응답 파싱 오류: ${e.message}", e)
                                showSessionError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showSessionError("세션 생성에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "세션 생성 오류: ${e.message}", e)
                runOnUiThread {
                    showSessionError("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    private fun checkSessionStatus(sessionId: Long, recordingItem: RecordingItem) {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            thread {
                try {
                    val request = Request.Builder()
                        .url("http://${serverIp}/chatbot/session-status/$sessionId")
                        .header("Authorization", "Bearer ${getJwtToken()}")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()

                        runOnUiThread {
                            if (response.isSuccessful && responseBody != null) {
                                try {
                                    val jsonResponse = JSONObject(responseBody)
                                    val status = jsonResponse.getString("status")

                                    when (status) {
                                        "READY" -> {
                                            showChatScreen(recordingItem, sessionId)
                                        }
                                        "INITIALIZING" -> {
                                            checkSessionStatus(sessionId, recordingItem)
                                        }
                                        "ERROR" -> {
                                            showSessionError("AI 분석 중 오류가 발생했습니다.")
                                        }
                                    }
                                } catch (e: Exception) {
                                    showSessionError("상태 확인 중 오류가 발생했습니다.")
                                }
                            } else {
                                showSessionError("상태 확인에 실패했습니다.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        showSessionError("서버 연결에 실패했습니다.")
                    }
                }
            }
        }, 3000) // 3초마다 상태 확인
    }

    private fun showSessionError(message: String) {
        sessionLoadingProgress.visibility = View.GONE
        loadingStatusText.visibility = View.GONE
        createChatbotButton.visibility = View.VISIBLE

        // 세션 생성 실패 시 저장된 정보 제거
        currentSelectedFile?.let { file ->
            sessionManager.removeSession(file.savedFileName)
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "세션 오류: $message")
    }

    // ========== 채팅 메시지 처리 ==========

    private fun loadChatHistory(sessionId: Long, onComplete: () -> Unit = {}) {
        thread {
            try {
                val request = Request.Builder()
                    .url("http://${serverIp}/chatbot/history/$sessionId")
                    .get()
                    .header("Authorization", "Bearer ${getJwtToken()}")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val conversations = parseChatHistory(responseBody)

                            runOnUiThread {
                                chatMessages.clear()
                                chatMessages.addAll(conversations)
                                chatAdapter.updateMessages(conversations)
                                scrollToBottom()
                                onComplete()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "채팅 기록 파싱 오류: ${e.message}", e)
                            runOnUiThread { onComplete() }
                        }
                    } else {
                        Log.d(TAG, "채팅 기록이 없거나 로드 실패 - 빈 채팅 화면 표시")
                        runOnUiThread { onComplete() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "채팅 기록 로드 오류: ${e.message}", e)
                runOnUiThread { onComplete() }
            }
        }
    }

    private fun parseChatHistory(responseBody: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        try {
            val jsonArray = JSONArray(responseBody)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val question = jsonObject.getString("question")
                val isUser = jsonObject.getBoolean("isUser")
                val timestamp = jsonObject.getString("timestamp")

                // 시간 파싱
                val timestampLong = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(timestamp)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                messages.add(ChatMessage(question, isUser, timestampLong))
            }
        } catch (e: Exception) {
            Log.e(TAG, "채팅 기록 파싱 오류: ${e.message}", e)
        }

        return messages
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isEmpty() || currentSessionId == null) {
            return
        }

        // 사용자 메시지 즉시 표시
        val userMessage = ChatMessage(messageText, isUser = true)
        chatAdapter.addMessage(userMessage)
        messageEditText.text.clear()
        scrollToBottom()

        // 서버로 메시지 전송
        val token = getJwtToken()
        if (token.isEmpty()) {
            showAIError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                // Multipart 형식으로 요청 생성
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("sessionId", currentSessionId.toString())
                    .addFormDataPart("message", messageText)
                    .build()

                val request = Request.Builder()
                    .url("http://${serverIp}/chatbot/message")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val aiResponse = jsonResponse.getString("aiMessage")

                                // AI 응답 표시
                                val aiMessage = ChatMessage(aiResponse, isUser = false)
                                chatAdapter.addMessage(aiMessage)
                                scrollToBottom()

                            } catch (e: Exception) {
                                Log.e(TAG, "AI 응답 파싱 오류: ${e.message}", e)
                                showAIError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showAIError("메시지 전송에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 전송 오류: ${e.message}", e)
                runOnUiThread {
                    showAIError("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    private fun showAIError(message: String) {
        val errorMessage = ChatMessage("죄송합니다. $message", isUser = false)
        chatAdapter.addMessage(errorMessage)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (chatMessages.isNotEmpty()) {
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
        }
    }

    // ========== 유틸리티 메서드들 ==========

    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면에서 돌아왔을 때 파일 목록 새로고침
        if (currentState == ScreenState.FILE_LIST) {
            loadRecordingFiles()
        }
    }

    override fun onBackPressed() {
        when (currentState) {
            ScreenState.SESSION_SETUP, ScreenState.CHATTING -> {
                showFileListScreen()
            }
            ScreenState.FILE_LIST -> {
                super.onBackPressed()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onDestroy() {

        super.onDestroy()
        Log.d(TAG, "ChatbotActivity 종료")
    }
}