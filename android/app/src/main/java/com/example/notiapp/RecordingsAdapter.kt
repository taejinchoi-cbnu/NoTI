package com.example.notiapp

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class RecordingsAdapter(
    private var recordings: MutableList<RecordingItem>,
    private val onDeleteClickListener: (RecordingItem) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition = -1
    private val handler = Handler(Looper.getMainLooper())

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filenameTextView: TextView = itemView.findViewById(R.id.recordingFileName)
        val infoTextView: TextView = itemView.findViewById(R.id.recordingInfo)
        val playButton: ImageButton = itemView.findViewById(R.id.recordingPlayButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.recordingDeleteButton)
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

        // 재생 버튼 상태 설정
        holder.playButton.setImageResource(
            if (position == currentPlayingPosition && mediaPlayer?.isPlaying == true)
                android.R.drawable.ic_media_pause
            else
                android.R.drawable.ic_media_play
        )

        // 재생 버튼 클릭 리스너
        holder.playButton.setOnClickListener {
            if (position == currentPlayingPosition && mediaPlayer?.isPlaying == true) {
                // 현재 재생 중인 파일 일시정지
                pausePlayback(holder)
            } else if (position == currentPlayingPosition && mediaPlayer != null) {
                // 일시정지된 파일 다시 재생
                resumePlayback(holder)
            } else {
                // 새로운 파일 재생
                stopPlayback()
                playRecording(recording, holder, position)
            }
        }

        // 삭제 버튼 클릭 리스너
        holder.deleteButton.setOnClickListener {
            // 재생 중인 파일이면 중지
            if (position == currentPlayingPosition) {
                stopPlayback()
            }

            // 삭제 콜백 호출
            onDeleteClickListener(recording)
        }
    }

    override fun getItemCount() = recordings.size

    private fun playRecording(recording: RecordingItem, holder: RecordingViewHolder, position: Int) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recording.filePath)
                setOnCompletionListener {
                    stopPlayback()
                    notifyItemChanged(position)
                }
                prepare()
                start()
            }

            currentPlayingPosition = position
            holder.playButton.setImageResource(android.R.drawable.ic_media_pause)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                holder.itemView.context,
                "재생 중 오류가 발생했습니다: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun pausePlayback(holder: RecordingViewHolder) {
        mediaPlayer?.pause()
        holder.playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun resumePlayback(holder: RecordingViewHolder) {
        mediaPlayer?.start()
        holder.playButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        // 이전 재생 아이템 UI 업데이트
        if (currentPlayingPosition != -1) {
            val oldPosition = currentPlayingPosition
            currentPlayingPosition = -1
            notifyItemChanged(oldPosition)
        }
    }

    fun updateRecordings(newRecordings: List<RecordingItem>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    // 메모리 누수 방지를 위한 리소스 해제
    fun releaseMediaPlayer() {
        stopPlayback()
    }
}