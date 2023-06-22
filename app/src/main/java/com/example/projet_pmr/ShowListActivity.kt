package com.example.projet_pmr

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.projet_pmr.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File

class ShowListActivity : AppCompatActivity() {

    private lateinit var id: String
    private lateinit var itemList: MutableList<ListItem>
    private lateinit var itemAdapter: ItemListAdapter
    private lateinit var coordinatesList: HashMap<Int, List<JSONObject>>

    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemButton: Button
    private lateinit var newItemEditText: EditText

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson

    private lateinit var speechRecognizer: SpeechRecognizer

    companion object {
        private const val SPEECH_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_list)

        // Initialisation des SharedPreferences
        sharedPreferences = getSharedPreferences("ListData", Context.MODE_PRIVATE)
        gson = Gson()

        id = intent.getStringExtra("id").toString()

        itemList = mutableListOf()
        val urlAPI = loadUrl()

        retrieveListItems(id, urlAPI)
        coordinatesList = HashMap()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemListAdapter(itemList)
        recyclerView.adapter = itemAdapter

        addItemButton = findViewById(R.id.buttonAddItem)
        newItemEditText = findViewById(R.id.editTextItem)

        addItemButton.setOnClickListener {
            val newItem = newItemEditText.text.toString()
            if (newItem.isNotEmpty()) {
                addItem(newItem, urlAPI)
                newItemEditText.text.clear()
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "La reconnaissance vocale n'est pas disponible sur cet appareil.", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Le service est prêt pour la parole
                startListening()
            }

            override fun onBeginningOfSpeech() {
                // Le début de la parole est détecté

            }

            override fun onRmsChanged(rmsdB: Float) {
                // Le niveau audio est en cours de changement
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Un tampon audio est reçu
            }

            override fun onEndOfSpeech() {
                // La fin de la parole est détectée
            }

            override fun onError(error: Int) {
                // Une erreur de reconnaissance vocale s'est produite
            }

            override fun onResults(results: Bundle?) {
                // Les résultats de la reconnaissance vocale sont disponibles
                val voiceResults = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                voiceResults?.let { handleVoiceResults(it) }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Des résultats partiels de la reconnaissance vocale sont disponibles
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Un événement non spécifié est survenu
            }
        })

        startListening()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.let { handleVoiceResults(it) }
        }
    }

    // Ajoutez la méthode startListening() pour démarrer la reconnaissance vocale
    private fun startListening() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        startActivityForResult(speechRecognizerIntent, SPEECH_REQUEST_CODE)
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



    inner class ItemListAdapter(private val itemList: MutableList<  ListItem>) :
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

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {

            private val textView: TextView = itemView.findViewById(R.id.textViewItem)

            init {
                itemView.setOnClickListener(this)
            }

            fun bind(item: String) {
                textView.text = item
            }

            override fun onClick(view: View) {
                val item = itemList[adapterPosition]

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

    private fun startSpeechToText() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        startActivityForResult(speechRecognizerIntent, SPEECH_REQUEST_CODE)
    }



    private fun getCoordinates(json: String) {
        for (item in itemList) {
            val coordinates = getCoordinatesByLabel(json, item.label)
            if (coordinates.isNotEmpty()) {
                println("Les coordonnées du produit '${item.label}' sont :")
                for (coordinate in coordinates) {
                    println("Ligne: ${coordinate.getInt("ligne")}, Colonne: ${coordinate.getInt("colonne")}")
                }
            } else {
                println("Le produit '${item.label}' n'a pas été trouvé.")
            }
            coordinatesList[item.id.toInt()] = coordinates
        }

    }

    private fun getCoordinatesByLabel(json: String, label: String): List<JSONObject> {
        val jsonArray = JSONArray(json)
        val coordinates = mutableListOf<JSONObject>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val nomProduit = jsonObject.getString("nom_produit")
            if (nomProduit.equals(label, ignoreCase = true)) {
                coordinates.add(jsonObject)
            }
        }

        return coordinates
    }
}

data class ListItem(val id: String, val label: String, val url: String?, val checked: String)
