package com.thruxion.app.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.thruxion.app.data.AppDatabase
import com.thruxion.app.data.model.Contact
import com.thruxion.app.data.model.Folder
import com.thruxion.app.data.model.SavedPlace
import com.thruxion.app.data.repository.SavedPlaceRepository
import com.thruxion.app.utils.UserSession
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SavedPlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavedPlaceRepository
    val allFolders: LiveData<List<Folder>>
    val allSavedPlaces: LiveData<List<SavedPlace>>
    val allContacts: LiveData<List<Contact>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SavedPlaceRepository(database.folderDao(), database.savedPlaceDao(), database.contactDao())
        
        val userIdLiveData = UserSession.userFlow.map { it?.userId ?: "guest" }.asLiveData()

        // Create default folders when user changes
        viewModelScope.launch {
            UserSession.userFlow.collect { user ->
                val userId = user?.userId ?: "guest"
                ensureDefaultFoldersCreated(userId)
            }
        }
        
        allFolders = userIdLiveData.switchMap { userId ->
            repository.getAllFolders(userId).asLiveData()
        }
        allSavedPlaces = userIdLiveData.switchMap { userId ->
            repository.getAllSavedPlaces(userId).asLiveData()
        }
        allContacts = userIdLiveData.switchMap { userId ->
            repository.getAllContacts(userId).asLiveData()
        }
    }

    private suspend fun ensureDefaultFoldersCreated(userId: String) {
        val context = getApplication<Application>()
        
        // Tags to identify folders uniquely across languages
        val TAG_FAV_CONTACTS = "FAV_CONTACTS"
        val TAG_FAV_PLACES = "FAV_PLACES"
        val TAG_LAWYERS = "LAWYERS"

        // Cleanup legacy folders without tags (if any) using their names
        val legacyNames = listOf("Favorites", "Favoritos", "Favorite Contacts", "Favorite Places", "Contactos Favoritos", "Lugares Favoritos", "Lawyers", "Abogados")
        legacyNames.forEach { name ->
            repository.getFolderByName(userId, name, "CONTACT")?.let { 
                if (it.systemTag == null) repository.deleteFolder(it) 
            }
            repository.getFolderByName(userId, name, "PLACE")?.let { 
                if (it.systemTag == null) repository.deleteFolder(it) 
            }
        }

        // 1. Favorite Contacts Folder
        repository.getFolderByTag(userId, TAG_FAV_CONTACTS).let { existing ->
            if (existing == null) {
                val name = context.getString(com.thruxion.app.R.string.default_folder_favorite_contacts)
                repository.insertFolder(Folder(userId = userId, name = name, type = "CONTACT", icon = "star", isDefault = true, systemTag = TAG_FAV_CONTACTS))
            }
        }

        // 2. Favorite Places Folder
        repository.getFolderByTag(userId, TAG_FAV_PLACES).let { existing ->
            if (existing == null) {
                val name = context.getString(com.thruxion.app.R.string.default_folder_favorite_places)
                repository.insertFolder(Folder(userId = userId, name = name, type = "PLACE", icon = "star", isDefault = true, systemTag = TAG_FAV_PLACES))
            }
        }

        // 3. Lawyers Folder
        repository.getFolderByTag(userId, TAG_LAWYERS).let { existing ->
            if (existing == null) {
                val name = context.getString(com.thruxion.app.R.string.default_folder_lawyers)
                repository.insertFolder(Folder(userId = userId, name = name, type = "CONTACT", icon = "justice", isDefault = true, systemTag = TAG_LAWYERS))
            }
        }
    }

    suspend fun insertFolder(name: String, type: String = "PLACE", concept: String? = null, city: String? = null, isShared: Boolean = false): Long {
        val userId = UserSession.userId ?: "guest"
        return repository.insertFolder(Folder(userId = userId, name = name, type = type, concept = concept, city = city, isShared = isShared))
    }

    fun insertPlace(name: String, address: String?, lat: Double, lon: Double, folderId: Long, remoteUserId: String? = null, id: Long = 0) = viewModelScope.launch {
        val userId = UserSession.userId ?: "guest"
        repository.insertPlace(SavedPlace(id = id, userId = userId, name = name, address = address, latitude = lat, longitude = lon, folderId = folderId, remoteUserId = remoteUserId))
    }

    fun insertContact(name: String, avatarIndex: Int, lat: Double, lon: Double, folderId: Long, remoteUserId: String? = null, id: Long = 0) = viewModelScope.launch {
        val userId = UserSession.userId ?: "guest"
        repository.insertContact(Contact(id = id, userId = userId, name = name, avatarIndex = avatarIndex, latitude = lat, longitude = lon, folderId = folderId, remoteUserId = remoteUserId))
    }

    fun deletePlace(place: SavedPlace) = viewModelScope.launch {
        repository.deletePlace(place)
    }

    fun deleteContact(contact: Contact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
    }

    fun updateFolder(folder: Folder) = viewModelScope.launch {
        repository.updateFolder(folder)
    }
}
