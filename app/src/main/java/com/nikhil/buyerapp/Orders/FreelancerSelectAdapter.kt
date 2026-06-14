package com.nikhil.buyerapp.displayingorders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.ItemFreelancerSelectBinding

data class FreelancerItem(
    val uid: String,
    val name: String,
    val email: String,
    val imageUrl: String
)

class FreelancerSelectAdapter(
    private val items: List<FreelancerItem>,
    private val onSelect: (FreelancerItem) -> Unit
) : RecyclerView.Adapter<FreelancerSelectAdapter.VH>() {

    inner class VH(val binding: ItemFreelancerSelectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemFreelancerSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvEmail.text = item.email
        Glide.with(holder.binding.ivAvatar)
            .load(item.imageUrl.ifEmpty { null })
            .placeholder(R.drawable.untitleddesign)
            .circleCrop()
            .into(holder.binding.ivAvatar)

        holder.binding.root.setOnClickListener { onSelect(item) }
    }
}