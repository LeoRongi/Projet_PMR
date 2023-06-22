package com.example.projet_pmr

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class CartographyDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cartography.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_CARTOGRAPHY = "cartography"
        private const val COLUMN_LINE = "ligne"
        private const val COLUMN_COLUMN = "colonne"
        private const val COLUMN_PRODUCT_LOCATION = "emplacement_produit"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_CARTOGRAPHY (" +
                "$COLUMN_LINE INTEGER, " +
                "$COLUMN_COLUMN INTEGER, " +
                "$COLUMN_PRODUCT_LOCATION INTEGER)"
        db.execSQL(createTableQuery)

        // Read the cartography data from the resource file
        val cartographyJson =
            context.resources.openRawResource(R.raw.cartography).bufferedReader().use { it.readText() }
        val cartographyArray = JSONArray(cartographyJson)

        // Insert the cartography data into the table
        for (i in 0 until cartographyArray.length()) {
            val item = cartographyArray.getJSONObject(i)
            val line = item.getInt("ligne")
            val column = item.getInt("colonne")
            val productLocation = item.getInt("emplacement_produit")

            val values = ContentValues().apply {
                put(COLUMN_LINE, line)
                put(COLUMN_COLUMN, column)
                put(COLUMN_PRODUCT_LOCATION, productLocation)
            }
            db.insert(TABLE_CARTOGRAPHY, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop the table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARTOGRAPHY")
        onCreate(db)
    }

    fun insertElement(line: Int, column: Int, productLocation: String) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_LINE, line)
        values.put(COLUMN_COLUMN, column)
        values.put(COLUMN_PRODUCT_LOCATION, productLocation)

        db.insert(TABLE_CARTOGRAPHY, null, values)
        db.close()
    }

    fun updateElement(line: Int, column: Int, newProductLocation: String) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_PRODUCT_LOCATION, newProductLocation)

        db.update(
            TABLE_CARTOGRAPHY,
            values,
            "$COLUMN_LINE = ? AND $COLUMN_COLUMN = ?",
            arrayOf(line.toString(), column.toString())
        )
        db.close()
    }

    @SuppressLint("Range")
    fun getMapMatrix(): Array<IntArray> {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_CARTOGRAPHY,
            arrayOf(COLUMN_LINE, COLUMN_COLUMN, COLUMN_PRODUCT_LOCATION),
            null,
            null,
            null,
            null,
            null
        )

        val maxLine = cursor.getCount()
        val maxColumn = 10 // Assuming there are 10 columns

        // Initialize the matrix with default values
        val matrix = Array(maxLine) { IntArray(maxColumn) }

        if (cursor.moveToFirst()) {
            do {
                val line = cursor.getInt(cursor.getColumnIndex(COLUMN_LINE))
                val column = cursor.getInt(cursor.getColumnIndex(COLUMN_COLUMN))
                val productLocation = cursor.getString(cursor.getColumnIndex(COLUMN_PRODUCT_LOCATION))

                val value = when (productLocation) {
                    "1" -> 1
                    else -> 0
                }

                matrix[line][column] = value
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return matrix
    }
}

