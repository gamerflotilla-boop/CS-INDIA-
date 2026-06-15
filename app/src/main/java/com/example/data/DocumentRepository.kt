package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val db: AppDatabase) {
    val allDocuments: Flow<List<Document>> = db.documentDao().getAllDocuments()
    val allFolders: Flow<List<Folder>> = db.folderDao().getAllFolders()

    fun getDocumentsInFolder(folderId: Int): Flow<List<Document>> {
        return db.documentDao().getDocumentsInFolder(folderId)
    }

    fun searchDocuments(query: String): Flow<List<Document>> {
        return db.documentDao().searchDocuments("%$query%")
    }

    suspend fun getDocumentById(id: Int): Document? {
        return db.documentDao().getDocumentById(id)
    }

    suspend fun getFolderById(id: Int): Folder? {
        return db.folderDao().getFolderById(id)
    }

    suspend fun insertDocument(document: Document): Long {
        return db.documentDao().insertDocument(document)
    }

    suspend fun deleteDocument(id: Int) {
        db.documentDao().deleteDocumentById(id)
    }

    suspend fun insertFolder(folder: Folder): Int {
        return db.folderDao().insertFolder(folder).toInt()
    }

    suspend fun deleteFolder(id: Int) {
        db.folderDao().deleteFolderById(id)
    }
}
