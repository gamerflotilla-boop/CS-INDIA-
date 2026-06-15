package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val originalImagePath: String,
    val processedImagePath: String,
    val extractedText: String = "",
    val folderId: Int? = null,
    val priorityLabel: String = "Normal", // "High", "Medium", "Normal"
    val category: String = "General", // "Invoice", "Receipt", "Academic", "Personal", "Other"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Int): Document?

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getDocumentsInFolder(folderId: Int): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE title LIKE :query OR extractedText LIKE :query ORDER BY timestamp DESC")
    fun searchDocuments(query: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Int): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: Int)
}

@Database(entities = [Document::class, Folder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "doc_scanner_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
