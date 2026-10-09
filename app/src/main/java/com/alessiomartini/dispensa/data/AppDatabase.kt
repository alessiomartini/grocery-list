package com.alessiomartini.dispensa.data

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [GroceryItem::class, PurchaseRecord::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchase_history` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `purchasedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds what the sync backend needs: a stable uuid, an updatedAt for last-write-wins, and
         * a soft-delete flag. The DEFAULTs and index names here must match the entities exactly,
         * or Room rejects the migrated database on open - see [GroceryItem].
         *
         * SQLite can't give each existing row its own UUID through a DEFAULT, so they're
         * backfilled in a second pass. updatedAt is set to "now" for all of them rather than
         * reconstructed from addedAt/statusChangedAt: none of this has ever been synced, and
         * "the server has never seen any of it" is exactly what that says.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grocery_items ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE grocery_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grocery_items ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchase_history ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                val now = System.currentTimeMillis()
                backfill(db, "grocery_items", now)
                backfill(db, "purchase_history", now)

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_items_uuid` ON `grocery_items` (`uuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_history_uuid` ON `purchase_history` (`uuid`)")
            }

            private fun backfill(db: SupportSQLiteDatabase, table: String, now: Long) {
                db.query("SELECT id FROM $table").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    while (cursor.moveToNext()) {
                        val values = ContentValues().apply {
                            put("uuid", UUID.randomUUID().toString())
                            put("updatedAt", now)
                        }
                        db.update(
                            table,
                            SupportSQLiteDatabase.CONFLICT_ABORT,
                            values,
                            "id = ?",
                            arrayOf<Any?>(cursor.getLong(idIndex))
                        )
                    }
                }
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pantry.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
