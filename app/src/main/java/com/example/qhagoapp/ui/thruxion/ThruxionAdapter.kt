package com.example.qhagoapp.ui.thruxion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qhagoapp.R
import com.example.qhagoapp.data.model.Contact
import com.example.qhagoapp.data.model.Folder
import com.example.qhagoapp.data.model.SavedPlace
import com.example.qhagoapp.databinding.ItemThruxionBinding

sealed class ThruxionItem {
    data class NearbyUser(val user: MapUser, val isSaved: Boolean) : ThruxionItem()
    data class SearchResultItem(val result: SearchResult, val isSaved: Boolean) : ThruxionItem()
    data class MainCategory(val title: String, val type: String) : ThruxionItem() // CONTACT or PLACE
    data class FolderItem(val folder: Folder, val count: Int) : ThruxionItem()
    data class ContactItem(val contact: Contact) : ThruxionItem()
    data class PlaceItem(val place: SavedPlace) : ThruxionItem()
    data class SaveTargetOption(val originalItem: Any, val targetType: String) : ThruxionItem()
    data class SaveFolderOption(val originalItem: Any, val targetType: String, val folder: Folder) : ThruxionItem()
    object NewFolderOption : ThruxionItem()
    object BackAction : ThruxionItem()
}

class TransformAdapter(
    private val onItemClicked: (ThruxionItem) -> Unit,
    private val onSaveClicked: (ThruxionItem) -> Unit
) : ListAdapter<ThruxionItem, TransformViewHolder>(ThruxionDiffCallback())
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder
    {
        val binding = ItemThruxionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransformViewHolder, position: Int)
    {
        val item = getItem(position)
        holder.bind(item, onItemClicked, onSaveClicked)
    }
}

class TransformViewHolder(private val binding: ItemThruxionBinding) : RecyclerView.ViewHolder(binding.root)
{
    fun bind(
        item: ThruxionItem,
        onItemClicked: (ThruxionItem) -> Unit,
        onSaveClicked: (ThruxionItem) -> Unit
    ) {
        val context = binding.root.context
        val yellowColor = ContextCompat.getColor(context, R.color.purple_500)
        
        binding.btnItemSave?.visibility = View.GONE
        binding.imageViewItemTransform.clearColorFilter()

        when (item) {
            is ThruxionItem.NearbyUser -> {
                binding.textViewItemTransform.text = item.user.name
                val resName = "avatar_${item.user.avatarIndex + 1}"
                val resId = binding.imageViewItemTransform.resources.getIdentifier(resName, "drawable", binding.imageViewItemTransform.context.packageName)
                if (resId != 0) {
                    binding.imageViewItemTransform.setImageResource(resId)
                } else {
                    binding.imageViewItemTransform.setImageResource(R.drawable.ic_profile)
                    binding.imageViewItemTransform.setColorFilter(yellowColor)
                }
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(if (item.isSaved) R.drawable.ic_edit else R.drawable.ic_add)
            }
            is ThruxionItem.SearchResultItem -> {
                binding.textViewItemTransform.text = item.result.shortName
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(if (item.isSaved) R.drawable.ic_edit else R.drawable.ic_add)
            }
            is ThruxionItem.MainCategory -> {
                binding.textViewItemTransform.text = item.title
                binding.imageViewItemTransform.setImageResource(if (item.type == "CONTACT") R.drawable.ic_profile else R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.FolderItem -> {
                binding.textViewItemTransform.text = "${item.folder.name} (${item.count})"
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_justice)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.ContactItem -> {
                binding.textViewItemTransform.text = item.contact.name
                val resName = "avatar_${item.contact.avatarIndex + 1}"
                val resId = binding.imageViewItemTransform.resources.getIdentifier(resName, "drawable", binding.imageViewItemTransform.context.packageName)
                if (resId != 0) {
                    binding.imageViewItemTransform.setImageResource(resId)
                } else {
                    binding.imageViewItemTransform.setImageResource(R.drawable.ic_profile)
                    binding.imageViewItemTransform.setColorFilter(yellowColor)
                }
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(R.drawable.ic_edit)
            }
            is ThruxionItem.PlaceItem -> {
                binding.textViewItemTransform.text = item.place.name
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(R.drawable.ic_edit)
            }
            is ThruxionItem.SaveTargetOption -> {
                binding.textViewItemTransform.text = "Save as ${item.targetType}"
                binding.imageViewItemTransform.setImageResource(if (item.targetType == "Contact") R.drawable.ic_profile else R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.SaveFolderOption -> {
                binding.textViewItemTransform.text = "Save to: ${item.folder.name}"
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_justice)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.NewFolderOption -> {
                binding.textViewItemTransform.text = "+ Create New Group/Folder"
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_add)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.BackAction -> {
                binding.textViewItemTransform.text = "Go Back"
                binding.imageViewItemTransform.setImageResource(android.R.drawable.ic_menu_revert)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
        }

        binding.root.setOnClickListener { onItemClicked(item) }
        binding.btnItemSave?.setOnClickListener { onSaveClicked(item) }
    }
}

class ThruxionDiffCallback : DiffUtil.ItemCallback<ThruxionItem>()
{
    override fun areItemsTheSame(oldItem: ThruxionItem, newItem: ThruxionItem): Boolean {
        return when {
            oldItem is ThruxionItem.NearbyUser && newItem is ThruxionItem.NearbyUser -> oldItem.user.id == newItem.user.id
            oldItem is ThruxionItem.SearchResultItem && newItem is ThruxionItem.SearchResultItem -> oldItem.result.id == newItem.result.id
            oldItem is ThruxionItem.ContactItem && newItem is ThruxionItem.ContactItem -> oldItem.contact.id == newItem.contact.id
            oldItem is ThruxionItem.PlaceItem && newItem is ThruxionItem.PlaceItem -> oldItem.place.id == newItem.place.id
            oldItem is ThruxionItem.FolderItem && newItem is ThruxionItem.FolderItem -> oldItem.folder.id == newItem.folder.id
            else -> oldItem == newItem
        }
    }
    override fun areContentsTheSame(oldItem: ThruxionItem, newItem: ThruxionItem) = oldItem == newItem
}
