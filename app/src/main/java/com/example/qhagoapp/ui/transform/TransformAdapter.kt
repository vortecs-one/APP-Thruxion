package com.example.qhagoapp.ui.transform

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qhagoapp.R
import com.example.qhagoapp.databinding.ItemTransformBinding

class TransformAdapter : ListAdapter<String, TransformViewHolder>(DiffCallback())
{
    private val drawables = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4,
        R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8,
        R.drawable.avatar_9, R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
        R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder
    {
        val binding = ItemTransformBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransformViewHolder, position: Int)
    {
        val item = getItem(position)
        holder.textView.text = item
        holder.imageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                holder.imageView.resources,
                drawables[position % drawables.size],
                null
            )
        )
    }

}

// -----------------------------------------------------
// VIEW HOLDER
// -----------------------------------------------------

class TransformViewHolder(binding: ItemTransformBinding) :
    RecyclerView.ViewHolder(binding.root) {

    val imageView = binding.imageViewItemTransform
    val textView = binding.textViewItemTransform
}

// -----------------------------------------------------
// DIFF UTIL
// -----------------------------------------------------

class DiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
}