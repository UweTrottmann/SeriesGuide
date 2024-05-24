// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.comments

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemCommentBinding
import com.battlelancer.seriesguide.util.CircleTransformation
import com.battlelancer.seriesguide.util.ImageTools
import com.uwetrottmann.trakt5.entities.Comment

/**
 * Binds a list of [Comment]s. Use [update] to submit new data and user name of current Trakt user
 * to show edit and delete buttons for that user's comments.
 */
class TraktCommentsAdapter(
    val context: Context,
    val onItemClickListener: OnItemClickListener
) : ListAdapter<Comment, CommentViewHolder>(CommentDiffCallback()) {

    /**
     * If not null, show edit and delete button for comments with this user name.
     */
    private var traktUserName: String? = null

    interface OnItemClickListener {
        fun onOpenWebsite(commentId: Int)
        fun onEdit(commentId: Int, comment: String, isSpoiler: Boolean)
        fun onDelete(commentId: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder.create(parent, onItemClickListener)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bindTo(context, traktUserName, getItem(position))
    }

    fun update(userName: String?, results: List<Comment>?) {
        traktUserName = userName
        submitList(results)
    }
}

class CommentViewHolder(
    private val binding: ItemCommentBinding,
    onItemClickListener: TraktCommentsAdapter.OnItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var comment: Comment? = null

    init {
        binding.cardComment.setOnClickListener {
            val comment = comment ?: return@setOnClickListener
            if (comment.spoiler == true) {
                // If comment is a spoiler it is hidden, first click should reveal it.
                comment.spoiler = false
                binding.textViewComment.text = comment.comment
            } else {
                // Open comment website
                comment.id?.let { onItemClickListener.onOpenWebsite(it) }
            }
        }
        binding.buttonEditComment.setOnClickListener {
            val commentId = comment?.id
            val message = comment?.comment
            val isSpoiler = comment?.spoiler
            if (commentId != null && message != null && isSpoiler != null) {
                onItemClickListener.onEdit(commentId, message, isSpoiler)
            }
        }
        binding.buttonDeleteComment.setOnClickListener {
            comment?.id?.let { onItemClickListener.onDelete(it) }
        }
    }

    fun bindTo(context: Context, traktUsername: String?, comment: Comment?) {
        this.comment = comment
        if (comment == null) {
            binding.textViewCommentUsername.text = null
            binding.imageViewCommentAvatar.setImageDrawable(null)
            binding.textViewComment.text = null
            binding.textViewCommentTimestamp.text = null
            binding.textViewCommentReplies.text = null
            return
        }

        val user = comment.user
        val username = user?.username
        binding.textViewCommentUsername.text = username

        val isCommentOfCurrentUser = traktUsername != null && traktUsername == username
        binding.buttonEditComment.isGone = !isCommentOfCurrentUser
        // Do not enforce deletion rules, let only Trakt check, as they may change without notice
        binding.buttonDeleteComment.isGone = !isCommentOfCurrentUser

        ImageTools.loadWithPicasso(context, user?.images?.avatar?.full)
            .transform(avatarTransform)
            .into(binding.imageViewCommentAvatar)

        if (comment.spoiler == true) {
            binding.textViewComment.setText(R.string.isspoiler)
            TextViewCompat.setTextAppearance(
                binding.textViewComment,
                R.style.TextAppearance_SeriesGuide_Body2_Error
            )
        } else {
            binding.textViewComment.text = comment.comment
            TextViewCompat.setTextAppearance(
                binding.textViewComment,
                R.style.TextAppearance_SeriesGuide_Body2
            )
        }

        val timestamp = comment.created_at?.toInstant()?.toEpochMilli()
            ?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL
                )
            } ?: context.getString(R.string.unknown)
        binding.textViewCommentTimestamp.text = timestamp

        val replies = comment.replies
        if (replies == null || replies <= 0) {
            // no replies
            binding.textViewCommentReplies.visibility = View.GONE
        } else {
            binding.textViewCommentReplies.visibility = View.VISIBLE
            binding.textViewCommentReplies.text = context.resources
                .getQuantityString(R.plurals.replies_plural, replies, replies)
        }
    }

    companion object {
        private val avatarTransform = CircleTransformation()

        fun create(
            parent: ViewGroup,
            onItemClickListener: TraktCommentsAdapter.OnItemClickListener
        ): CommentViewHolder {
            return CommentViewHolder(
                ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onItemClickListener
            )
        }
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean =
        oldItem.comment == newItem.comment
                && oldItem.spoiler == newItem.spoiler
}


