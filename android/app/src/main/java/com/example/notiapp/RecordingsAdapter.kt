package com.example.notiapp

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class RecordingsAdapter(
    private var recordings: MutableList<RecordingItem>,
    private val onDeleteClickListener: (RecordingItem) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    private val TAG = "RecordingsAdapter"

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filenameTextView: TextView = itemView.findViewById(R.id.recordingFileName)
        val infoTextView: TextView = itemView.findViewById(R.id.recordingInfo)
        val playButton: ImageButton = itemView.findViewById(R.id.recordingPlayButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.recordingDeleteButton)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.recordingItemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]

        holder.filenameTextView.text = recording.filename
        holder.infoTextView.text = "${recording.date} | ${recording.duration}"

        // 재생 버튼도 상세 화면으로 이동하도록 수정
        holder.playButton.setImageResource(android.R.drawable.ic_media_play)

        // 아이템 클릭 리스너 - 전체 아이템 컨테이너 클릭 시 상세 화면으로 이동
        holder.itemContainer.setOnClickListener {
            navigateToDetail(holder, recording)
        }

        // 재생 버튼 클릭 리스너 - 상세 화면으로 이동
        holder.playButton.setOnClickListener {
            navigateToDetail(holder, recording)
        }

        // 삭제 버튼 클릭 리스너
        holder.deleteButton.setOnClickListener {
            // 삭제 콜백 호출
            onDeleteClickListener(recording)
        }
    }

    private fun navigateToDetail(holder: RecordingViewHolder, recording: RecordingItem) {
        val context = holder.itemView.context
        val intent = Intent(context, RecordingDetailActivity::class.java).apply {
            putExtra("filePath", recording.filePath)
            putExtra("fileName", recording.filename)
            putExtra("recordingDate", recording.date)
            putExtra("duration", recording.duration)
        }
        Log.d(TAG, "상세 화면으로 이동: ${recording.filename}, ${recording.filePath}")
        context.startActivity(intent)
    }

    override fun getItemCount() = recordings.size

    fun updateRecordings(newRecordings: List<RecordingItem>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    fun releaseMediaPlayer() {
        // 아무 동작 없음
    }

}