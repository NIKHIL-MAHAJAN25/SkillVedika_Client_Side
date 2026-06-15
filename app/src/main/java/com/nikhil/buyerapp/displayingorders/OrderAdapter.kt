package com.nikhil.buyerapp.displayingorders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.widget.PopupMenu
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.OrderItemBinding
import com.nikhil.buyerapp.dataclasses.Project
import com.nikhil.buyerapp.dataclasses.ProjectStatus

class OrderAdapter(
    private val onClicked: (Project) -> Unit,
    private val onAssignFreelancer: (Project) -> Unit,
    private val onMarkCompleted: (Project) -> Unit,
    private val onCancelProject: (Project) -> Unit,
    private val onLeaveReview: (Project) -> Unit

) : ListAdapter<Project, OrderAdapter.ViewHolder>(ServiceDiffCallback()) {

    inner class ViewHolder(private val binding: OrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.tvProjectTitle.text = project.title
            binding.tvDescription.text = project.description
            binding.tvFreelancerName.text = project.freename?.ifEmpty { "Not Assigned" }
            binding.tvBudget.text = "₹${project.budget}"
            binding.chipStatus.text = project.status

            // chip color based on status
            val chipColor = when (project.status) {
                ProjectStatus.OPEN.name -> 0xFFE8F5E9.toInt()
                ProjectStatus.ASSIGNED.name -> 0xFFE3F2FD.toInt()
                ProjectStatus.COMPLETED.name -> 0xFFEDE7F6.toInt()
                ProjectStatus.CANCELLED.name -> 0xFFFFEBEE.toInt()
                else -> 0xFFE8F5E9.toInt()
            }
            binding.chipStatus.setChipBackgroundColorResource(android.R.color.transparent)
            binding.chipStatus.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(chipColor)

            binding.root.setOnClickListener { onClicked(project) }

            binding.edit.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)

                // show options based on current status
                when (project.status) {
                    ProjectStatus.OPEN.name -> {
                        popup.menu.add(0, 1, 0, "Assign Freelancer")
                        popup.menu.add(0, 3, 1, "Cancel Project")
                    }
                    ProjectStatus.ASSIGNED.name -> {
                        popup.menu.add(0, 2, 0, "Mark as Completed")
                        popup.menu.add(0, 3, 1, "Cancel Project")
                    }
                    ProjectStatus.COMPLETED.name-> {
                        popup.menu.add(0,0,0,"Leave a Review")
                    }
                    ProjectStatus.CANCELLED.name -> {
                        popup.menu.add(0, 0, 0, "No actions available")
                    }
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        0 -> onLeaveReview(project)
                        1 -> onAssignFreelancer(project)
                        2 -> onMarkCompleted(project)
                        3 -> onCancelProject(project)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project) =
            oldItem.projectid == newItem.projectid

        override fun areContentsTheSame(oldItem: Project, newItem: Project) =
            oldItem == newItem
    }
}