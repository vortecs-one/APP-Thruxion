package com.thruxion.app.ui.thruxion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thruxion.app.R
import com.thruxion.app.data.model.Contact
import com.thruxion.app.data.model.Folder
import com.thruxion.app.data.model.SavedPlace
import com.thruxion.app.databinding.ItemThruxionBinding

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
    private val onSaveClicked: (ThruxionItem) -> Unit,
    private val onDeleteClicked: (ThruxionItem) -> Unit = {}
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
        holder.bind(item, onItemClicked, onSaveClicked, onDeleteClicked)
    }
}

class TransformViewHolder(private val binding: ItemThruxionBinding) : RecyclerView.ViewHolder(binding.root)
{
    fun bind(
        item: ThruxionItem,
        onItemClicked: (ThruxionItem) -> Unit,
        onSaveClicked: (ThruxionItem) -> Unit,
        onDeleteClicked: (ThruxionItem) -> Unit
    ) {
        val context = binding.root.context
        val yellowColor = ContextCompat.getColor(context, R.color.purple_500)
        val fluorGreen = ContextCompat.getColor(context, R.color.fluor_green)
        
        binding.btnItemSave?.visibility = View.GONE
        binding.btnItemSecondary?.visibility = View.GONE
        binding.imageViewItemTransform.clearColorFilter()

        when (item) {
            is ThruxionItem.NearbyUser -> {
                binding.textViewItemTransform.text = item.user.name
                val resName = "avatar_${item.user.avatarIndex + 1}"
                val resId = binding.imageViewItemTransform.resources.getIdentifier(resName, "drawable", binding.imageViewItemTransform.context.packageName)
                if (resId != 0)
                    binding.imageViewItemTransform.setImageResource(resId)
                else
                {
                    // Show justice icon for lawyers, otherwise profile/smile
                    val icon = if (item.user.name.contains("Lawyer", ignoreCase = true) || item.user.name.contains("Abogado", ignoreCase = true)) 
                               R.drawable.ic_justice else R.drawable.ic_profile
                    binding.imageViewItemTransform.setImageResource(icon)
                    binding.imageViewItemTransform.setColorFilter(yellowColor)
                }
                binding.btnItemSave?.visibility = View.VISIBLE
                if (item.isSaved)
                {
                    binding.btnItemSave?.setIconResource(R.drawable.ic_edit)
                    binding.btnItemSave?.setIconTintResource(R.color.purple_500)
                }
                else
                {
                    binding.btnItemSave?.setIconResource(R.drawable.ic_add)
                    binding.btnItemSave?.setIconTintResource(R.color.fluor_green)
                }
            }
            is ThruxionItem.SearchResultItem -> {
                binding.textViewItemTransform.text = item.result.shortName
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
                binding.btnItemSave?.visibility = View.VISIBLE
                if (item.isSaved)
                {
                    binding.btnItemSave?.setIconResource(R.drawable.ic_edit)
                    binding.btnItemSave?.setIconTintResource(R.color.purple_500)
                }
                else
                {
                    binding.btnItemSave?.setIconResource(R.drawable.ic_add)
                    binding.btnItemSave?.setIconTintResource(R.color.fluor_green)
                }
            }
            is ThruxionItem.MainCategory -> {
                binding.textViewItemTransform.text = item.title
                binding.imageViewItemTransform.setImageResource(if (item.type == "CONTACT") R.drawable.ic_profile else R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.FolderItem -> {
                // Determine localized name for default folders
                val folderName = when (item.folder.systemTag) {
                    "FAV_CONTACTS" -> context.getString(R.string.default_folder_favorite_contacts)
                    "FAV_PLACES" -> context.getString(R.string.default_folder_favorite_places)
                    "LAWYERS" -> context.getString(R.string.default_folder_lawyers)
                    else -> item.folder.name
                }
                
                binding.textViewItemTransform.text = "$folderName (${item.count})"
                val folderIcon = when (item.folder.icon) {
                    "star" -> android.R.drawable.btn_star_big_on
                    "justice" -> R.drawable.ic_justice
                    else -> if (item.folder.type == "PLACE") R.drawable.ic_searched_place else R.drawable.ic_profile
                }
                binding.imageViewItemTransform.setImageResource(folderIcon)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
                
                // Show edit icon ONLY for custom (non-default) folders
                if (!item.folder.isDefault) {
                    binding.btnItemSave?.visibility = View.VISIBLE
                    binding.btnItemSave?.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                    binding.btnItemSave?.setIconTintResource(R.color.brilliant_red)
                    
                    binding.btnItemSecondary?.visibility = View.VISIBLE
                    binding.btnItemSecondary?.setIconResource(R.drawable.ic_edit)
                    binding.btnItemSecondary?.setIconTintResource(R.color.purple_500)
                }
            }
            is ThruxionItem.ContactItem -> {
                binding.textViewItemTransform.text = item.contact.name
                val resName = "avatar_${item.contact.avatarIndex + 1}"
                val resId = binding.imageViewItemTransform.resources.getIdentifier(resName, "drawable", binding.imageViewItemTransform.context.packageName)
                if (resId != 0)
                    binding.imageViewItemTransform.setImageResource(resId)
                else
                {
                    // Show justice icon for lawyers, otherwise profile/smile
                    val icon = if (item.contact.name.contains("Lawyer", ignoreCase = true) || item.contact.name.contains("Abogado", ignoreCase = true)) 
                               R.drawable.ic_justice else R.drawable.ic_profile
                    binding.imageViewItemTransform.setImageResource(icon)
                    binding.imageViewItemTransform.setColorFilter(yellowColor)
                }
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.btnItemSave?.setIconTintResource(R.color.brilliant_red)
            }
            is ThruxionItem.PlaceItem -> {
                binding.textViewItemTransform.text = item.place.name
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
                binding.btnItemSave?.visibility = View.VISIBLE
                binding.btnItemSave?.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.btnItemSave?.setIconTintResource(R.color.brilliant_red)
            }
            is ThruxionItem.SaveTargetOption -> {
                binding.textViewItemTransform.text = "Save as ${item.targetType}"
                binding.imageViewItemTransform.setImageResource(if (item.targetType == "Contact") R.drawable.ic_profile else R.drawable.ic_searched_place)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.SaveFolderOption -> {
                val folderName = when (item.folder.systemTag) {
                    "FAV_CONTACTS" -> context.getString(R.string.default_folder_favorite_contacts)
                    "FAV_PLACES" -> context.getString(R.string.default_folder_favorite_places)
                    "LAWYERS" -> context.getString(R.string.default_folder_lawyers)
                    else -> item.folder.name
                }
                binding.textViewItemTransform.text = "Save to: $folderName"
                val folderIcon = when (item.folder.icon) {
                    "star" -> android.R.drawable.btn_star_big_on
                    "justice" -> R.drawable.ic_justice
                    else -> if (item.folder.type == "PLACE") R.drawable.ic_searched_place else R.drawable.ic_profile
                }
                binding.imageViewItemTransform.setImageResource(folderIcon)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
            is ThruxionItem.NewFolderOption -> {
                binding.textViewItemTransform.text = "+ Create New Group/Folder"
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_add)
                binding.imageViewItemTransform.setColorFilter(fluorGreen)
            }
            is ThruxionItem.BackAction -> {
                binding.textViewItemTransform.text = "Go Back"
                binding.imageViewItemTransform.setImageResource(android.R.drawable.ic_menu_revert)
                binding.imageViewItemTransform.setColorFilter(yellowColor)
            }
        }

        binding.root.setOnClickListener { onItemClicked(item) }
        binding.btnItemSave?.setOnClickListener { onSaveClicked(item) }
        binding.btnItemSecondary?.setOnClickListener { onDeleteClicked(item) }
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
