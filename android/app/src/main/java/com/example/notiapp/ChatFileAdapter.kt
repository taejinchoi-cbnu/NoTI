package com.example.notiapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class ChatFileAdapter(
    private var recordings: MutableList<RecordingItem>,
    private val onFileClickListener: (RecordingItem) -> Unit
) : RecyclerView.Adapter<ChatFileAdapter.ChatFileViewHolder>() {

    inner class ChatFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filenameTextView: TextView = itemView.findViewById(R.id.recordingFileName)
        val infoTextView: TextView = itemView.findViewById(R.id.recordingInfo)
        val chatStatusIcon: ImageView = itemView.findViewById(R.id.chatStatusIcon)
        val itemContainer: ConstraintLayout = itemView.findViewById(R.id.recordingItemContainer)
        val cardView: CardView = itemView.findViewById(R.id.recordingCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatFileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_file, parent, false)
        return ChatFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatFileViewHolder, position: Int) {
        val recording = recordings[position]

        holder.filenameTextView.text = recording.filename
        holder.infoTextView.text = "${recording.date} | ${recording.duration}"

        // 채팅 상태 아이콘 설정 (나중에 구현)
        // TODO: 기존 채팅 세션이 있는지 확인해서 아이콘 변경
        holder.chatStatusIcon.setImageResource(R.drawable.ic_recording)

        // 클릭 리스너
        holder.itemContainer.setOnClickListener {
            onFileClickListener(recording)
        }

        holder.cardView.setOnClickListener {
            onFileClickListener(recording)
        }
    }

    override fun getItemCount() = recordings.size

    fun updateRecordings(newRecordings: List<RecordingItem>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }
}