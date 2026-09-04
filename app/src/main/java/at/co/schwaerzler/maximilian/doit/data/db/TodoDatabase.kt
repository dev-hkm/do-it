/*
 * Copyright 2026 Maximilian Schwärzler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package at.co.schwaerzler.maximilian.doit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import at.co.schwaerzler.maximilian.doit.data.db.dao.TagDao
import at.co.schwaerzler.maximilian.doit.data.db.dao.TodoDao
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo

/**
 * Room database for the app, containing [Todo] and [Tag] entities.
 */
@Database(
    entities = [Todo::class, Tag::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultTags(db)
                        }
                    })
                    .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
        }

        private fun seedDefaultTags(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Work', 'lucide_ic_briefcase', -14979310, 0)")
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Personal', 'lucide_ic_user', -16744214, 1)")
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Study', 'lucide_ic_graduation_cap', -10044434, 2)")
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Shopping', 'lucide_ic_shopping_bag', -2206509, 3)")
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Health', 'lucide_ic_activity', -1364375, 4)")
            db.execSQL("INSERT OR IGNORE INTO tags (name, icon_name, color_argb, order_index) VALUES ('Finance', 'lucide_ic_wallet', -14043403, 5)")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN state TEXT NOT NULL DEFAULT 'OPEN'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, icon_name TEXT NOT NULL, color_argb INTEGER, order_index INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("ALTER TABLE todos ADD COLUMN is_locked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN tag_id INTEGER DEFAULT NULL")
                seedDefaultTags(db)
            }
        }
    }
}