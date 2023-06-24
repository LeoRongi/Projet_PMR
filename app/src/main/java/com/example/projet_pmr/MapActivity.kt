package com.example.projet_pmr

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView

class MapActivity : AppCompatActivity() {
    private lateinit var gridView: GridView
    private lateinit var matrixAdapter: MatrixAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val cartographyHelper = CartographyDatabaseHelper(this)
        cartographyHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS ${CartographyDatabaseHelper.TABLE_CARTOGRAPHY}")
        cartographyHelper.onCreate(cartographyHelper.writableDatabase)

        val mapMatrix = cartographyHelper.getMapMatrix()
        affMap(mapMatrix)

        gridView = findViewById(R.id.gridView)
        matrixAdapter = MatrixAdapter(this, mapMatrix.size, mapMatrix[0].size)
        gridView.adapter = matrixAdapter
        matrixAdapter.setMapMatrix(mapMatrix)
        matrixAdapter.notifyDataSetChanged()
    }

    fun affMap(mapMatrix: Array<IntArray>) {
        val sb = StringBuilder()

        for (i in mapMatrix.indices) {
            sb.append("ligne ${i + 1} : ")

            for (j in mapMatrix[i].indices) {
                sb.append("${mapMatrix[i][j]} ")

                // Ajouter un point-virgule après chaque élément, sauf le dernier
                if (j < mapMatrix[i].lastIndex) {
                    sb.append("; ")
                }
            }

            // Ajouter un point-virgule à la fin de chaque ligne, sauf la dernière
            if (i < mapMatrix.lastIndex) {
                sb.append("; ")
            }
        }

        println(sb.toString())
    }

    class MatrixAdapter(private val context: Context, private val numRows: Int, private val numCols: Int) : BaseAdapter() {
        private var mapMatrix: Array<IntArray> = arrayOf()

        fun setMapMatrix(mapMatrix: Array<IntArray>) {
            this.mapMatrix = mapMatrix
        }

        override fun getCount(): Int {
            return numRows * numCols
        }

        override fun getItem(position: Int): Any? {
            val row = position / numCols
            val col = position % numCols

            return if (row in mapMatrix.indices && col in mapMatrix[row].indices) {
                mapMatrix[row][col]
            } else {
                null
            }
        }
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val cellView: TextView
            val viewHolder: ViewHolder

            if (convertView == null) {
                cellView = LayoutInflater.from(context).inflate(R.layout.matrix_cell_layout, parent, false) as TextView
                viewHolder = ViewHolder()
                viewHolder.cell = cellView
                cellView.tag = viewHolder
            } else {
                cellView = convertView as TextView
                viewHolder = convertView.tag as ViewHolder
            }

            val value = getItem(position) as Int


            if (value == 1) {
                cellView.setBackgroundColor(Color.WHITE)
            } else {
                cellView.setBackgroundColor(Color.BLACK)
            }

            return cellView
        }
        private class ViewHolder {
            lateinit var cell: View
        }
    }
}
