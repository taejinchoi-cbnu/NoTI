package com.example.notiapp

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RecordActivity : AppCompatActivity() {

    private val TAG = "recordActivity"
    private val RECORD_AUDIO_PERMISSION_CODE = 200
    val serverIp = AddressAdmin.MY_SERVER_IP

    // 녹음 관련 변수들
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var isPaused = false
    private var outputFile: String = ""
    private var recordingDuration: Int = 0
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var totalPausedTime: Long = 0
    private var tempFileName: String = ""

    // UI 요소들
    private lateinit var recordingTitleText: TextView
    private lateinit var recordingStatusText: TextView
    private lateinit var recordingTimeText: TextView
    private lateinit var recordingHintText: TextView
    private lateinit var currentFileNameText: TextView

    private lateinit var recordButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var recordingActiveButtonsLayout: LinearLayout

    // 녹음 상태 카드 - 테두리 적용 대상
    private lateinit var recordingStatusCard: CardView

    // GIF 애니메이션 뷰
    private lateinit var recordingAnimationView: ImageView

    // 시간 업데이트용 핸들러
    private var timeUpdateHandler: Handler? = null
    private var timeUpdateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.record)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소 초기화
        initializeViews()

        // 버튼 클릭 리스너 설정
        setupButtonListeners()

        // 초기 UI 상태 설정
        updateUIForRecordingState(RecordingState.READY)
    }

    private fun initializeViews() {
        recordingTitleText = findViewById(R.id.recordingTitleText)
        recordingStatusText = findViewById(R.id.recordingStatusText)
        recordingTimeText = findViewById(R.id.recordingTimeText)
        recordingHintText = findViewById(R.id.recordingHintText)
        currentFileNameText = findViewById(R.id.currentFileNameText)

        recordButton = findViewById(R.id.recordButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopButton = findViewById(R.id.stopButton)
        recordingActiveButtonsLayout = findViewById(R.id.recordingActiveButtonsLayout)

        // 상태 카드 초기화 (테두리 적용 대상)
        recordingStatusCard = findViewById(R.id.recordingStatusCard)

        // 애니메이션 뷰 초기화
        recordingAnimationView = findViewById(R.id.recordingAnimationView)

        // 초기 상태에서는 숨김 처리
        recordingAnimationView.visibility = View.GONE
    }

    // 녹음 상태에 따라 카드 테두리 설정
    private fun updateCardBorder(state: RecordingState) {
        val drawable = GradientDrawable().apply {
            // 기본 배경색 설정 (카드 배경색)
            setColor(ContextCompat.getColor(this@RecordActivity, R.color.cardBackground))

            when (state) {
                RecordingState.RECORDING -> {
                    // 녹음 중 테두리
                    setStroke(
                        resources.getDimensionPixelSize(R.dimen.recording_border_width), // 2dp
                        ContextCompat.getColor(this@RecordActivity, R.color.accentGreen)
                    )
                }
                RecordingState.PAUSED -> {
                    // 일시정지 중일 때: 주황색 테두리 적용
                    setStroke(
                        resources.getDimensionPixelSize(R.dimen.recording_border_width), // 2dp
                        ContextCompat.getColor(this@RecordActivity, R.color.warningOrange)
                    )
                }
                else -> {
                    // 준비 상태나 완료 상태: 테두리 없음
                    setStroke(0, 0)
                }
            }

            // 둥근 모서리 설정
            cornerRadius = resources.getDimensionPixelSize(R.dimen.button_corner_radius).toFloat()
        }

        recordingStatusCard.background = drawable
    }

    // 카드 상태 변화 애니메이션
    private fun animateCardStateChange(state: RecordingState) {
        when (state) {
            RecordingState.RECORDING -> {
                // 녹음 시작 애니메이션
                val scaleInAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in)
                recordingStatusCard.startAnimation(scaleInAnimation)
            }
            RecordingState.PAUSED -> {
                // 일시정지 애니메이션
                val fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                recordingStatusCard.startAnimation(fadeAnimation)
            }
            RecordingState.COMPLETED -> {
                // 완료 시: fade_out 효과
                val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                recordingStatusCard.startAnimation(fadeOutAnimation)
            }
            else -> {
                // 준비 상태: 특별한 애니메이션 없음
            }
        }
    }

    private fun setupButtonListeners() {
        // 녹음 시작 버튼
        recordButton.setOnClickListener {
            if (checkPermission()) {
                startRecording()
            } else {
                requestPermission()
            }
        }

        // 일시정지 버튼
        pauseButton.setOnClickListener {
            if (isRecording && !isPaused) {
                pauseRecording()
            } else if (isPaused) {
                resumeRecording()
            }
        }

        // 종료 버튼
        stopButton.setOnClickListener {
            if (isRecording || isPaused) {
                stopRecording()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    private fun startRecording() {
        try {
            // 녹음 파일 저장 디렉토리 생성
            val recordingsDir = File(getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }

            // 임시 파일명 생성
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            tempFileName = "TEMP_REC_$timestamp.mp3"
            outputFile = File(recordingsDir, tempFileName).absolutePath

            // 녹음 시작 시간 기록
            startTime = System.currentTimeMillis()
            totalPausedTime = 0

            // MediaRecorder 초기화
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            // 상태 업데이트
            isRecording = true
            isPaused = false

            // UI 업데이트
            updateUIForRecordingState(RecordingState.RECORDING)

            // 시간 업데이트 시작
            startTimeUpdate()

            Log.d(TAG, "녹음 시작: $tempFileName")

        } catch (e: IOException) {
            Log.e(TAG, "녹음 시작 실패: ${e.message}", e)
            Toast.makeText(this, "녹음을 시작할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            updateUIForRecordingState(RecordingState.READY)
        }
    }

    private fun pauseRecording() {
        try {
            mediaRecorder?.pause()
            isPaused = true
            pausedTime = System.currentTimeMillis()

            // UI 업데이트
            updateUIForRecordingState(RecordingState.PAUSED)

            // 시간 업데이트 중지
            stopTimeUpdate()

            Log.d(TAG, "녹음 일시정지")

        } catch (e: Exception) {
            Log.e(TAG, "녹음 일시정지 실패: ${e.message}", e)
            Toast.makeText(this, "일시정지에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resumeRecording() {
        try {
            mediaRecorder?.resume()
            isPaused = false

            // 일시정지된 시간 누적
            totalPausedTime += System.currentTimeMillis() - pausedTime

            // UI 업데이트
            updateUIForRecordingState(RecordingState.RECORDING)

            // 시간 업데이트 재시작
            startTimeUpdate()

            Log.d(TAG, "녹음 재시작")

        } catch (e: Exception) {
            Log.e(TAG, "녹음 재시작 실패: ${e.message}", e)
            Toast.makeText(this, "재시작에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            // 녹음 종료 시간 기록 및 기간 계산
            val endTime = System.currentTimeMillis()
            recordingDuration = ((endTime - startTime - totalPausedTime) / 1000).toInt()

            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null

            // 상태 리셋
            isRecording = false
            isPaused = false

            // 시간 업데이트 중지
            stopTimeUpdate()

            // UI 업데이트
            updateUIForRecordingState(RecordingState.COMPLETED)

            Log.d(TAG, "녹음 종료: ${recordingDuration}초")

            // 파일명 입력 다이얼로그 표시
            showFileNameInputDialog()

        } catch (e: Exception) {
            Log.e(TAG, "녹음 종료 실패: ${e.message}", e)
            Toast.makeText(this, "녹음을 중지할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimeUpdate() {
        timeUpdateHandler = Handler(Looper.getMainLooper())
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                if (isRecording && !isPaused) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = ((currentTime - startTime - totalPausedTime) / 1000).toInt()
                    updateTimeDisplay(elapsedTime)
                    timeUpdateHandler?.postDelayed(this, 1000) // 1초마다 업데이트
                }
            }
        }
        timeUpdateHandler?.post(timeUpdateRunnable!!)
    }

    private fun stopTimeUpdate() {
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable!!)
        timeUpdateHandler = null
        timeUpdateRunnable = null
    }

    private fun updateTimeDisplay(seconds: Int) {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val timeString = String.format("%02d:%02d", minutes, remainingSeconds)
        recordingTimeText.text = timeString
    }

    private fun updateUIForRecordingState(state: RecordingState) {
        // 상태 변경 시 카드 업데이트
        updateCardBorder(state)
        animateCardStateChange(state)

        when (state) {
            RecordingState.READY -> {
                recordingStatusText.text = "녹음 준비"
                recordingTimeText.text = "00:00"
                recordingHintText.text = "버튼을 눌러 녹음을 시작하세요"
                currentFileNameText.visibility = View.GONE

                recordButton.visibility = View.VISIBLE
                recordingActiveButtonsLayout.visibility = View.GONE

                // 대기 상태에서는 애니메이션 숨김
                recordingAnimationView.visibility = View.GONE
            }

            RecordingState.RECORDING -> {
                recordingTitleText.text = "녹음 중"
                recordingStatusText.text = "녹음 중..."
                recordingHintText.text = "일시정지하거나 녹음을 종료하세요"
                currentFileNameText.text = "파일명: $tempFileName"
                currentFileNameText.visibility = View.VISIBLE

                recordButton.visibility = View.GONE
                recordingActiveButtonsLayout.visibility = View.VISIBLE

                // 일시정지 버튼 활성화
                pauseButton.setImageResource(R.drawable.ic_pause)
                pauseButton.contentDescription = "일시정지"

                // 녹음 중 GIF 애니메이션 시작
                recordingAnimationView.visibility = View.VISIBLE
                Glide.with(this)
                    .asGif()
                    .load(R.drawable.recording_animation)
                    .into(recordingAnimationView)
            }

            RecordingState.PAUSED -> {
                recordingTitleText.text = "일시정지"
                recordingStatusText.text = "일시정지 중"
                recordingHintText.text = "녹음을 계속하거나 종료하세요"

                // 재시작 버튼으로 변경
                pauseButton.setImageResource(R.drawable.ic_play)
                pauseButton.contentDescription = "재시작"

                // 일시정지 시 애니메이션 정지
                recordingAnimationView.visibility = View.GONE
            }

            RecordingState.COMPLETED -> {
                recordingTitleText.text = "녹음 완료"
                recordingStatusText.text = "녹음이 완료되었습니다"
                recordingHintText.text = "파일명을 입력해주세요"

                recordButton.visibility = View.VISIBLE
                recordingActiveButtonsLayout.visibility = View.GONE

                // 완료 시 애니메이션 숨김
                recordingAnimationView.visibility = View.GONE
            }
        }
    }

    enum class RecordingState {
        READY, RECORDING, PAUSED, COMPLETED
    }

    private fun showFileNameInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filename_input, null)
        val fileNameEditText = dialogView.findViewById<EditText>(R.id.fileNameEditText)

        // 기본 파일명 제안
        val defaultName = SimpleDateFormat("yyyy-MM-dd HH_mm", Locale.getDefault()).format(Date())
        fileNameEditText.setText("녹음 $defaultName")
        fileNameEditText.selectAll()

        val dialog = AlertDialog.Builder(this)
            .setTitle("파일명 입력")
            .setMessage("녹음 파일의 이름을 입력해주세요.")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val userFileName = fileNameEditText.text.toString().trim()
                if (userFileName.isNotEmpty()) {
                    processRecordingWithCustomName(userFileName)
                } else {
                    Toast.makeText(this, "파일명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    showFileNameInputDialog()
                }
            }
            .setNegativeButton("취소") { _, _ ->
                deleteTempFile()
                navigateToDashboard()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun processRecordingWithCustomName(userFileName: String) {
        // 파일명에 확장자가 없으면 추가
        val finalFileName = if (userFileName.endsWith(".mp3", true)) {
            userFileName
        } else {
            "$userFileName.mp3"
        }

        // 임시 파일을 사용자 지정 파일명으로 변경
        val tempFile = File(outputFile)
        val newFile = File(tempFile.parent, finalFileName)

        try {
            if (tempFile.renameTo(newFile)) {
                outputFile = newFile.absolutePath
                Log.d(TAG, "파일명 변경 성공: ${tempFile.name} -> ${newFile.name}")
                uploadRecordingAndNavigate()
            } else {
                Log.e(TAG, "파일명 변경 실패")
                Toast.makeText(this, "파일명 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                uploadRecordingAndNavigate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "파일명 변경 중 오류: ${e.message}", e)
            Toast.makeText(this, "파일 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            uploadRecordingAndNavigate()
        }
    }

    private fun deleteTempFile() {
        try {
            val tempFile = File(outputFile)
            if (tempFile.exists()) {
                tempFile.delete()
                Log.d(TAG, "임시 파일 삭제: $tempFileName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "임시 파일 삭제 실패: ${e.message}", e)
        }
    }

    private fun uploadRecordingAndNavigate() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("녹음 파일 업로드 중...")
            setCancelable(false)
            show()
        }

        thread {
            try {
                val file = File(outputFile)
                if (!file.exists()) {
                    Log.e(TAG, "파일이 존재하지 않습니다: $outputFile")
                    showErrorAndDismissDialog(progressDialog, "파일을 찾을 수 없습니다.")
                    return@thread
                }

                Log.d(TAG, "파일 업로드 시작: ${file.name}, 크기: ${file.length()} bytes, 녹음 길이: ${recordingDuration}초")

                // JWT 토큰 가져오기
                val token = getJwtToken()
                if (token.isEmpty()) {
                    showErrorAndDismissDialog(progressDialog, "로그인이 필요합니다. 다시 로그인해주세요.")
                    navigateToLogin()
                    return@thread
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("duration", recordingDuration.toString())
                    .build()

                val request = Request.Builder()
                    .url("http://${serverIp}/file/upload/audio")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "서버 응답: ${response.code} - $responseBody")

                    runOnUiThread {
                        progressDialog.dismiss()

                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val prefix = "파일 저장 완료: "
                                if (responseBody.startsWith(prefix)) {
                                    val serverSavedFileName = responseBody.substring(prefix.length).trim()

                                    val sharedPreferences = getSharedPreferences("recording_files", MODE_PRIVATE)
                                    val editor = sharedPreferences.edit()
                                    editor.putString(file.name, serverSavedFileName)
                                    editor.apply()

                                    Log.d(TAG, "파일명 매핑 저장: ${file.name} -> $serverSavedFileName")
                                    Toast.makeText(this, "녹음 파일이 성공적으로 저장되었습니다!\n파일명: ${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Log.w(TAG, "예상치 못한 응답 형식: $responseBody")
                                    Toast.makeText(this, "녹음 파일이 업로드되었지만 파일명을 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "응답 파싱 오류: ${e.message}", e)
                                Toast.makeText(this, "녹음 파일이 업로드되었지만 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }

                            navigateToDashboard()
                        } else {
                            val errorMessage = when (response.code) {
                                400 -> "요청 데이터가 올바르지 않습니다."
                                401, 403 -> {
                                    "토큰 에러."
                                }
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                else -> "업로드 실패: ${response.code}"
                            }

                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                            if (response.code == 401 || response.code == 403) {
                                navigateToLogin()
                            } else {
                                navigateToDashboard()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                showErrorAndDismissDialog(progressDialog, "서버 연결에 실패했습니다. 인터넷 연결을 확인하세요.")
                navigateToDashboard()
            } catch (e: Exception) {
                Log.e(TAG, "예외 발생: ${e.message}", e)
                showErrorAndDismissDialog(progressDialog, "오류가 발생했습니다: ${e.message}")
                navigateToDashboard()
            }
        }
    }

    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    private fun clearJwtToken() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("jwt_token").apply()
    }

    private fun showErrorAndDismissDialog(dialog: ProgressDialog, message: String) {
        runOnUiThread {
            dialog.dismiss()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToDashboard() {
        runOnUiThread {
            val intent = Intent(this, DashBoardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToLogin() {
        runOnUiThread {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "녹음 권한이 승인되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 액티비티가 중지될 때 녹음 중이면 중지
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 시간 업데이트 핸들러 정리
        stopTimeUpdate()
    }
}