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
    private val onFileClickListener: (RecordingItem) -> Unit,
    private val sessionManager: ChatSessionManager
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

        // 채팅 상태 아이콘 설정 with animation
        if (sessionManager.hasSession(recording.savedFileName)) {
            // 기존 세션이 있는 경우 - 채팅 아이콘으로 변경
            if (holder.chatStatusIcon.tag != "chat") {
                // 아이콘 변경 애니메이션
                holder.chatStatusIcon.animate()
                    .scaleX(0f).scaleY(0f)
                    .setDuration(100)
                    .withEndAction {
                        holder.chatStatusIcon.setImageResource(R.drawable.ic_chat)
                        holder.chatStatusIcon.setColorFilter(
                            holder.itemView.context.getColor(R.color.naverGreen),
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                        holder.chatStatusIcon.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
                holder.chatStatusIcon.tag = "chat"
            } else {
                // 이미 채팅 아이콘인 경우 애니메이션 없이 설정
                holder.chatStatusIcon.setImageResource(R.drawable.ic_chat)
                holder.chatStatusIcon.setColorFilter(
                    holder.itemView.context.getColor(R.color.naverGreen),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            holder.chatStatusIcon.alpha = 1.0f
        } else {
            // 새로운 파일 - 기본 녹음 아이콘
            if (holder.chatStatusIcon.tag != "recording") {
                holder.chatStatusIcon.setImageResource(R.drawable.ic_recording)
                holder.chatStatusIcon.clearColorFilter()
                holder.chatStatusIcon.tag = "recording"
            }
            holder.chatStatusIcon.alpha = 0.7f
        }

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

    // 특정 아이템의 세션 상태가 변경되었을 때 해당 아이템만 업데이트
    fun updateSessionStatus(savedFileName: String) {
        val index = recordings.indexOfFirst { it.savedFileName == savedFileName }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    // 모든 아이템의 세션 상태를 다시 확인하여 업데이트
    fun refreshSessionStatuses() {
        notifyDataSetChanged()
    }
}