package com.nikhil.buyerapp.displayingorders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.buyerapp.databinding.OrderItemBinding
import com.nikhil.buyerapp.dataclasses.Project


class OrderAdapter(private val onClicked:(Project)->Unit): ListAdapter<Project, OrderAdapter.ViewHolder>(
    ServiceDiffCallback()
){

    inner class ViewHolder(private val binding: OrderItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(project: Project)
        {
            binding.tvProjectTitle.text=project.title
            binding.tvDescription.text=project.description
            binding.tvFreelancerName.text=project.freename
            binding.tvBudget.text=project.budget.toString()
            binding.chipStatus.text=project.status
            binding.root.setOnClickListener {
                onClicked(project)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderAdapter.ViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderAdapter.ViewHolder, position: Int) {
        val project=getItem(position)
        holder.bind(project)
    }
    class ServiceDiffCallback: DiffUtil.ItemCallback<Project>(){
        override fun areItemsTheSame(
            oldItem: Project,
            newItem: Project
        ): Boolean {
            return oldItem.projectid==newItem.projectid
        }

        override fun areContentsTheSame(
            oldItem: Project,
            newItem: Project
        ): Boolean {
            return oldItem==newItem
        }

    }

}