package com.example.qhagoapp.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.data.AppDatabase
import com.example.qhagoapp.data.model.Contact
import com.example.qhagoapp.data.model.Folder
import com.example.qhagoapp.data.model.SavedPlace
import com.example.qhagoapp.data.repository.SavedPlaceRepository
import com.example.qhagoapp.utils.UserSession
import kotlinx.coroutines.launch

class SavedPlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavedPlaceRepository
    val allFolders: androidx.lifecycle.LiveData<List<Folder>>
    val allSavedPlaces: androidx.lifecycle.LiveData<List<SavedPlace>>
    val allContacts: androidx.lifecycle.LiveData<List<Contact>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SavedPlaceRepository(database.folderDao(), database.savedPlaceDao(), database.contactDao())
        val userId = UserSession.userId ?: "guest"
        allFolders = repository.getAllFolders(userId).asLiveData()
        allSavedPlaces = repository.getAllSavedPlaces(userId).asLiveData()
        allContacts = repository.getAllContacts(userId).asLiveData()
    }

    suspend fun insertFolder(name: String, type: String = "PLACE", concept: String? = null, city: String? = null, isShared: Boolean = false): Long {
        val userId = UserSession.userId ?: "guest"
        return repository.insertFolder(Folder(userId = userId, name = name, type = type, concept = concept, city = city, isShared = isShared))
    }

    fun insertPlace(name: String, address: String?, lat: Double, lon: Double, folderId: Long, id: Long = 0) = viewModelScope.launch {
        val userId = UserSession.userId ?: "guest"
        repository.insertPlace(SavedPlace(id = id, userId = userId, name = name, address = address, latitude = lat, longitude = lon, folderId = folderId))
    }

    fun insertContact(name: String, avatarIndex: Int, lat: Double, lon: Double, folderId: Long, id: Long = 0) = viewModelScope.launch {
        val userId = UserSession.userId ?: "guest"
        repository.insertContact(Contact(id = id, userId = userId, name = name, avatarIndex = avatarIndex, latitude = lat, longitude = lon, folderId = folderId))
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
}
