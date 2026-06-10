package com.nikhil.buyerapp.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.NewsArticleBinding

class NewsAdapter(private val onArticleClick: (String) -> Unit):ListAdapter<Result,NewsAdapter.ViewHolder> (articleDiffCallback()) {
    inner class ViewHolder(private val binding: NewsArticleBinding ):RecyclerView.ViewHolder(binding.root)
    {
        fun bind(article:Result)
        {
            binding.apply{
                tvNewsTitle.text=article.title
                tvSource.text=article.source_name
                tvNewsDesc.text=article.description
                Glide.with(root.context)
                    .load(article.image_url)
                    .placeholder(R.drawable.newsplacee) // Change to your placeholder
                    .error(R.drawable.newsplacee) // Change to your error image
                    .fitCenter()
                    .into(ivNewsImage)
                root.setOnClickListener {
                    onArticleClick(article.link)
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsAdapter.ViewHolder {
        val binding= NewsArticleBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsAdapter.ViewHolder, position: Int) {
        val article=getItem(position)
        holder.bind(article)

    }
    class articleDiffCallback:DiffUtil.ItemCallback<Result>() {
        override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem.article_id==newItem.article_id
        }

        override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
           return oldItem==newItem
        }
    }

}