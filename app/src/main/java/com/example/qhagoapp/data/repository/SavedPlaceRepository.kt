package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.dao.ContactDao
import com.example.qhagoapp.data.dao.FolderDao
import com.example.qhagoapp.data.dao.SavedPlaceDao
import com.example.qhagoapp.data.model.Contact
import com.example.qhagoapp.data.model.Folder
import com.example.qhagoapp.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

class SavedPlaceRepository(
    private val folderDao: FolderDao,
    private val savedPlaceDao: SavedPlaceDao,
    private val contactDao: ContactDao
) {
    fun getAllFolders(userId: String): Flow<List<Folder>> = folderDao.getAllFolders(userId)
    suspend fun getFolderByName(userId: String, name: String, type: String): Folder? = folderDao.getFolderByName(userId, name, type)
    suspend fun getFolderByTag(userId: String, tag: String): Folder? = folderDao.getFolderByTag(userId, tag)
    fun getAllSavedPlaces(userId: String): Flow<List<SavedPlace>> = savedPlaceDao.getAllSavedPlaces(userId)
    fun getAllContacts(userId: String): Flow<List<Contact>> = contactDao.getAllContacts(userId)

    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    fun getPlacesInFolder(folderId: Long, userId: String): Flow<List<SavedPlace>> = savedPlaceDao.getPlacesInFolder(folderId, userId)
    suspend fun insertPlace(place: SavedPlace) = savedPlaceDao.insertPlace(place)
    suspend fun deletePlace(place: SavedPlace) = savedPlaceDao.deletePlace(place)

    fun getContactsInFolder(folderId: Long, userId: String): Flow<List<Contact>> = contactDao.getContactsInFolder(folderId, userId)
    suspend fun insertContact(contact: Contact) = contactDao.insertContact(contact)
    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)
}
