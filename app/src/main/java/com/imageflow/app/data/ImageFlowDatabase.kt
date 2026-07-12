package com.imageflow.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.imageflow.app.data.dao.HistoryDao
import com.imageflow.app.data.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ImageFlowDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: ImageFlowDatabase? = null

        fun getInstance(context: Context): ImageFlowDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): ImageFlowDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ImageFlowDatabase::class.java,
                "imageflow.db"
            )
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
