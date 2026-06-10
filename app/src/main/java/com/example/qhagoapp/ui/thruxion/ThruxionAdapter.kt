package com.example.qhagoapp.ui.thruxion
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qhagoapp.databinding.ItemThruxionBinding

class TransformAdapter(
    private val onItemClicked: (MapUser) -> Unit,
    private val onSaveClicked: (MapUser) -> Unit
) : ListAdapter<MapUser, TransformViewHolder>(DiffCallback())
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder
    {
        val binding = ItemThruxionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransformViewHolder, position: Int)
    {
        val user = getItem(position)
        holder.textView.text = user.name
        val resName = "avatar_${user.avatarIndex + 1}"
        val resId = holder.imageView.resources.getIdentifier(resName, "drawable", holder.imageView.context.packageName)
        if (resId != 0)
            holder.imageView.setImageResource(resId)
        
        holder.itemView.setOnClickListener { onItemClicked(user) }
        holder.saveButton?.setOnClickListener { onSaveClicked(user) }
    }
}

class TransformViewHolder(binding: ItemThruxionBinding) : RecyclerView.ViewHolder(binding.root)
{
    val imageView = binding.imageViewItemTransform
    val textView = binding.textViewItemTransform
    val saveButton = binding.btnItemSave
}

class DiffCallback : DiffUtil.ItemCallback<MapUser>()
{
    override fun areItemsTheSame(oldItem: MapUser, newItem: MapUser) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: MapUser, newItem: MapUser) = oldItem == newItem
}