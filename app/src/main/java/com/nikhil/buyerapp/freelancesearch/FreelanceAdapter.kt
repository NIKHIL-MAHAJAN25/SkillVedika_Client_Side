package com.nikhil.buyerapp.freelancesearch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nikhil.buyerapp.dataclasses.FreelancerItem

import com.nikhil.buyerapp.databinding.FreelanceritemBinding


class FreelanceAdapter(private val onclicked:(FreelancerItem)->Unit):ListAdapter<FreelancerItem, FreelanceAdapter.ViewHolder>
    (ServiceDiffCallback())
{
    inner class ViewHolder(private val binding: FreelanceritemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(service:FreelancerItem)
        {
            binding.tvName.text=service.name
            Glide.with(itemView.context)
                .load(service.profileImageUrl)

                .centerCrop()
                .into(binding.ivProfileImage)
            binding.tvPrimarySkill.text=service.primaryskill
            binding.tvratings.text=service.rating.toString()
            binding.tvProjectRate.text=service?.projectRate.toString()?: "Negotiable"
            binding.root.setOnClickListener {
                onclicked(service)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreelanceAdapter.ViewHolder {
        val binding = FreelanceritemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FreelanceAdapter.ViewHolder, position: Int) {
        val service=getItem(position)
        holder.bind(service)
    }

    class ServiceDiffCallback:DiffUtil.ItemCallback<FreelancerItem>()
    {
        override fun areItemsTheSame(oldItem: FreelancerItem, newItem: FreelancerItem): Boolean {
            return oldItem.uid==newItem.uid
        }

        override fun areContentsTheSame(oldItem: FreelancerItem, newItem: FreelancerItem): Boolean {
            return oldItem==newItem
        }

    }
}