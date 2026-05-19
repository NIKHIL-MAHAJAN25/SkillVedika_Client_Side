package com.nikhil.buyerapp.freelanceprofileview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.buyerapp.databinding.ItemReviewCompactBinding
import com.nikhil.buyerapp.dataclasses.Review


class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ViewHolder>(ReviewDiffCallback()) {

    inner class ViewHolder(private val binding: ItemReviewCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.tvReviewerName.text = review.reviewerName
            binding.tvReviewText.text = review.reviewText
            binding.ratingBar.rating = review.rating.toFloat()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewCompactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review) =
            oldItem.reviewerUid == newItem.reviewerUid &&
                    oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(oldItem: Review, newItem: Review) =
            oldItem == newItem
    }
}