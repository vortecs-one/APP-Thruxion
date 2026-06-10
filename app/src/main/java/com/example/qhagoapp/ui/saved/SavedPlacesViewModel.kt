package com.example.qhagoapp.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.data.AppDatabase
import com.example.qhagoapp.data.model.Contact
import com.example.qhagoapp.data.model.Folder
import com.example.qhagoapp.data.model.SavedPlace
import com.example.qhagoapp.data.repository.SavedPlaceRepository
import com.example.qhagoapp.utils.UserSession
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
        
        // Favorites Folder for CONTACTS
        val favName = context.getString(com.example.qhagoapp.R.string.default_folder_favorites)
        val favContactExisting = repository.getFolderByName(userId, favName, "CONTACT")
        if (favContactExisting == null) {
            repository.insertFolder(Folder(userId = userId, name = favName, type = "CONTACT", icon = "star", isDefault = true))
        }

        // Favorites Folder for PLACES
        val favPlaceExisting = repository.getFolderByName(userId, favName, "PLACE")
        if (favPlaceExisting == null) {
            repository.insertFolder(Folder(userId = userId, name = favName, type = "PLACE", icon = "star", isDefault = true))
        }

        // Lawyers Folder
        val lawyerName = context.getString(com.example.qhagoapp.R.string.default_folder_lawyers)
        val lawyerExisting = repository.getFolderByName(userId, lawyerName, "CONTACT")
        if (lawyerExisting == null) {
            repository.insertFolder(Folder(userId = userId, name = lawyerName, type = "CONTACT", isDefault = true))
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
