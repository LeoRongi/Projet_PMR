package com.example.projet_pmr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class ShowListActivity : AppCompatActivity() {

    private lateinit var id: String
    private lateinit var itemList: MutableList<ListItem>
    private lateinit var itemAdapter: ItemListAdapter
    private lateinit var coordinatesList: MutableList<Point>
    private lateinit var articleNamesList :MutableList<String>

    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemButton: Button
    private lateinit var newItemEditText: EditText

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson
    private lateinit var urlAPI: String




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_list)

        // Initialisation des SharedPreferences
        sharedPreferences = getSharedPreferences("ListData", Context.MODE_PRIVATE)
        gson = Gson()

        id = intent.getStringExtra("id").toString()

        itemList = mutableListOf()
        urlAPI = loadUrl()

        retrieveListItems(id, urlAPI)



        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemListAdapter(itemList)
        recyclerView.adapter = itemAdapter

        addItemButton = findViewById(R.id.buttonAddItem)
        newItemEditText = findViewById(R.id.editTextItem)

        val startButton = findViewById<Button>(R.id.startNavigation)


        startButton.setOnClickListener {
            coordinatesList = ArrayList<Point>()
            articleNamesList = ArrayList<String>()
            getCoordinates()
            val intent = Intent(applicationContext, Itineraire::class.java)
            intent.putStringArrayListExtra("articleNamesList", ArrayList(articleNamesList))
            intent.putExtra("coordinatesList", ArrayList(coordinatesList))
            startActivity(intent)
        }

        addItemButton.setOnClickListener {
            val newItem = newItemEditText.text.toString()
            if (newItem.isNotEmpty()) {
                addItem(newItem, urlAPI)
                newItemEditText.text.clear()
            }
        }




    }



    // Dans la méthode onActivityResult()
    private fun handleVoiceResults(results: ArrayList<String>) {
        for (voiceInput in results) {
            if (voiceInput.equals("Démarrer la navigation", ignoreCase = true)) {
                Toast.makeText(this, "Démarrer la navigation", Toast.LENGTH_SHORT).show()
                // Effectuer les actions souhaitées pour démarrer la navigation
                break
            }
        }
    }


    inner class ItemListAdapter(private val itemList: MutableList<ListItem>) :
        RecyclerView.Adapter<ItemListAdapter.ItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_layout, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = itemList[position]
            holder.bind(item.label)
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val textView: TextView = itemView.findViewById(R.id.textViewItem)
            private val imageViewDelete: ImageView = itemView.findViewById(R.id.imageViewDeleteItem)

            init {
                itemView.setOnClickListener(this)
                imageViewDelete.setOnClickListener(this)
            }

            fun bind(item: String) {
                textView.text = item
            }

            override fun onClick(view: View) {
                if (view.id == R.id.imageViewDeleteItem) {
                    val item = itemList[adapterPosition]
                    delItem(item, urlAPI)
                    itemList.remove(item)
                    notifyDataSetChanged()
                } else {
                    // Handle item click
                    val item = itemList[adapterPosition]
                }
            }
        }

    }

    private fun retrieveListItems(id: String, urlAPI: String) {
        val url = "$urlAPI/lists/$id/items"

        val requestQueue = Volley.newRequestQueue(this)
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                handleListItemsResponse(response)
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "API connection error: ${error.message}", Toast.LENGTH_LONG)
                    .show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = getToken() // Get the identification token from preferences
                headers["hash"] = token
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun handleListItemsResponse(response: JSONObject) {
        try {
            val itemsArray = response.getJSONArray("items")
            for (i in 0 until itemsArray.length()) {
                val itemObject = itemsArray.getJSONObject(i)
                val id = itemObject.getString("id")
                val label = itemObject.getString("label")
                val url = itemObject.getString("url")
                val checked = itemObject.getString("checked")
                val listItem = ListItem(id, label, url, checked)
                itemList.add(listItem)
            }

            itemAdapter.notifyDataSetChanged()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun addItem(label: String, urlAPI: String) {
        val url = "$urlAPI/lists/$id/items?label=$label"

        val requestQueue = Volley.newRequestQueue(this)
        val request = object : JsonObjectRequest(
            Request.Method.POST, url, null,
            Response.Listener { response ->
                addItemResponse(response)
                System.out.println(response.toString())
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "API connection error: ${error.message}", Toast.LENGTH_LONG)
                    .show()
                System.out.println(error.message)
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = getToken() // Get the identification token from preferences
                headers["hash"] = token
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun addItemResponse(response: JSONObject) {
        try {
            val itemObject = response.getJSONObject("item")
            val id = itemObject.getString("id")
            val label = itemObject.getString("label")
            val url = itemObject.getString("url")
            val checked = itemObject.getString("checked")
            val listItem = ListItem(id, label, url, checked)
            itemList.add(listItem)

            recyclerView.adapter?.notifyDataSetChanged()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun loadUrl(): String {
        val sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        return sharedPreferences.getString("url", "http://tomnab.fr/todo-api/")
            ?: "http://tomnab.fr/todo-api/"
    }

    private fun getToken(): String {
        val sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        return sharedPreferences.getString("token", "") ?: ""
    }



    private fun getCoordinates() {
        val cartographyJson = applicationContext.resources.openRawResource(R.raw.product).bufferedReader().use {
            it.readText()
        }
        val cartographyArray = JSONArray(cartographyJson)
        logItems()
        for (item in itemList) {

            val coordinates = getCoordinatesByLabel(cartographyArray, item.label)
            if (coordinates.isNotEmpty()) {
                articleNamesList.add(item.label)
                println("Les coordonnées du produit '${item.label}' sont :")
                for (coordinate in coordinates) {
                    val ligne = coordinate.getInt("ligne")
                    val colonne = coordinate.getInt("colonne")
                    println("Ligne: $ligne, Colonne: $colonne")
                    coordinatesList.add(Point(colonne, ligne))
                }
            } else {
                println("Le produit '${item.label}' n'a pas été trouvé.")
            }
        }
    }



    private fun getCoordinatesByLabel(cartographyArray: JSONArray, label: String): List<JSONObject> {
        val coordinates = mutableListOf<JSONObject>()

        for (i in 0 until cartographyArray.length()) {
            val jsonObject = cartographyArray.getJSONObject(i)
            val nomProduit = jsonObject.getString("nom_produit")
            if (nomProduit == label)  {
                coordinates.add(jsonObject)
            }
        }

        return coordinates
    }

    private fun logItems() {
        for (item in itemList) {
            Log.d("ListItem", "ID: ${item.id}, Label: ${item.label}, URL: ${item.url}, Checked: ${item.checked}")
        }
    }

    private fun delItem(item: ListItem, urlAPI: String) {
        val url = "$urlAPI/lists/$id/items/${item.id}"

        val requestQueue = Volley.newRequestQueue(this)
        val request = object : JsonObjectRequest(
            Request.Method.DELETE, url, null,
            Response.Listener { response ->
                // Item deleted successfully
                itemList.remove(item)
                recyclerView.adapter?.notifyDataSetChanged()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "API connection error: ${error.message}", Toast.LENGTH_LONG)
                    .show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = getToken() // Get the identification token from preferences
                headers["hash"] = token
                return headers
            }
        }

        requestQueue.add(request)
    }

}
data class ListItem(val id: String, val label: String, val url: String?, val checked: String)
