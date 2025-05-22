package com.example.notiapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class RecordingsAdapter(
    private var recordings: MutableList<RecordingItem>,
    private val onDeleteClickListener: (RecordingItem) -> Unit,
    private val onDownloadClickListener: (RecordingItem) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    private val TAG = "RecordingsAdapter"

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filenameTextView: TextView = itemView.findViewById(R.id.recordingFileName)
        val infoTextView: TextView = itemView.findViewById(R.id.recordingInfo)
        val playButton: ImageButton = itemView.findViewById(R.id.recordingPlayButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.recordingDeleteButton)
        val downloadButton: ImageButton = itemView.findViewById(R.id.recordingDownloadButton)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.recordingItemContainer)
        val cardView: CardView = itemView.findViewById(R.id.recordingCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]

        holder.filenameTextView.text = recording.filename

        // 서버 파일과 로컬 파일에 따라 정보 표시 방식 및 색상 변경
        if (recording.isServerFile) {
            // 서버 파일: 파일 크기 정보도 함께 표시
            val fileSizeKB = recording.fileSize / 1024
            holder.infoTextView.text = "${recording.date} | ${recording.duration} | ${fileSizeKB}KB (서버)"

            // 서버 파일 색상 설정: #85a8ff
            holder.cardView.setCardBackgroundColor(Color.parseColor("#85a8ff"))

            // 다운로드 버튼 표시
            holder.downloadButton.visibility = View.VISIBLE

            // 다운로드 상태에 따른 재생 버튼 처리
            if (recording.isDownloaded) {
                holder.playButton.alpha = 1.0f
                holder.playButton.isEnabled = true
            } else {
                holder.playButton.alpha = 0.5f
                holder.playButton.isEnabled = false
            }
        } else {
            // 로컬 파일: 기존 방식 유지
            holder.infoTextView.text = "${recording.date} | ${recording.duration}"

            // 로컬 파일 색상 설정: #68ff7f
            holder.cardView.setCardBackgroundColor(Color.parseColor("#68ff7f"))

            // 다운로드 버튼 숨김
            holder.downloadButton.visibility = View.GONE

            // 재생 버튼 활성화
            holder.playButton.alpha = 1.0f
            holder.playButton.isEnabled = true
        }

        // 재생 버튼 아이콘 설정
        holder.playButton.setImageResource(android.R.drawable.ic_media_play)

        // 아이템 클릭 리스너
        holder.itemContainer.setOnClickListener {
            navigateToDetail(holder, recording)
        }

        // 재생 버튼 클릭 리스너
        holder.playButton.setOnClickListener {
            if (recording.isServerFile && !recording.isDownloaded) {
                Toast.makeText(
                    holder.itemView.context,
                    "먼저 파일을 다운로드해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                navigateToDetail(holder, recording)
            }
        }

        // 다운로드 버튼 클릭 리스너
        holder.downloadButton.setOnClickListener {
            if (recording.isServerFile) {
                onDownloadClickListener(recording)
            }
        }

        // 삭제 버튼 클릭 리스너
        holder.deleteButton.setOnClickListener {
            if (recording.isServerFile) {
                showServerFileDeleteDialog(holder, recording)
            } else {
                // 로컬 파일 삭제 확인
                showLocalFileDeleteDialog(holder, recording)
            }
        }
    }

    private fun showServerFileDeleteDialog(holder: RecordingViewHolder, recording: RecordingItem) {
        val context = holder.itemView.context

        AlertDialog.Builder(context)
            .setTitle("서버 파일 삭제")
            .setMessage("서버에 저장된 파일은 현재 삭제할 수 없습니다.\n목록에서만 제거하시겠습니까?")
            .setPositiveButton("목록에서 제거") { _, _ ->
                onDeleteClickListener(recording)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showLocalFileDeleteDialog(holder: RecordingViewHolder, recording: RecordingItem) {
        val context = holder.itemView.context

        AlertDialog.Builder(context)
            .setTitle("로컬 파일 삭제")
            .setMessage("'${recording.filename}' 파일을 삭제하시겠습니까?\n삭제된 파일은 복구할 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                onDeleteClickListener(recording)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun navigateToDetail(holder: RecordingViewHolder, recording: RecordingItem) {
        val context = holder.itemView.context

        val intent = Intent(context, RecordingDetailActivity::class.java).apply {
            if (recording.isServerFile) {
                // 서버 파일인 경우
                putExtra("filePath", if (recording.isDownloaded) recording.filePath else "")
                putExtra("fileName", recording.filename)
                putExtra("recordingDate", recording.date)
                putExtra("duration", recording.duration)
                putExtra("serverSavedFileName", recording.savedFileName)
                putExtra("isServerFile", true)
                putExtra("isDownloaded", recording.isDownloaded)
                putExtra("fileSize", recording.fileSize)
                putExtra("uploadDate", recording.uploadDate)
            } else {
                // 로컬 파일인 경우
                putExtra("filePath", recording.filePath)
                putExtra("fileName", recording.filename)
                putExtra("recordingDate", recording.date)
                putExtra("duration", recording.duration)
                putExtra("isServerFile", false)
                putExtra("isDownloaded", true) // 로컬 파일은 항상 다운로드된 상태

                // SharedPreferences에서 서버 저장 파일명 확인
                val sharedPreferences = context.getSharedPreferences("recording_files", Context.MODE_PRIVATE)
                val serverSavedFileName = sharedPreferences.getString(recording.filename, "")
                if (!serverSavedFileName.isNullOrEmpty()) {
                    putExtra("serverSavedFileName", serverSavedFileName)
                }
            }
        }

        Log.d(TAG, "상세 화면으로 이동: ${recording.filename}, 서버파일: ${recording.isServerFile}, 다운로드됨: ${recording.isDownloaded}")
        context.startActivity(intent)
    }

    override fun getItemCount() = recordings.size

    fun updateRecordings(newRecordings: List<RecordingItem>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    fun updateRecordingDownloadStatus(savedFileName: String, localPath: String) {
        val index = recordings.indexOfFirst { it.savedFileName == savedFileName }
        if (index != -1) {
            recordings[index] = recordings[index].copy(
                isDownloaded = true,
                filePath = localPath
            )
            notifyItemChanged(index)
        }
    }
}