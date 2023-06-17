package com.example.projet_pmr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity() {

    private var errorConnectAccountTextView: TextView? = null
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText

    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)

        errorConnectAccountTextView = findViewById(R.id.errorConnectAccountTextView)
        editTextUsername = findViewById(R.id.usernameEditText)
        editTextPassword = findViewById(R.id.passwordEditText)

        val connectBtn = findViewById<Button>(R.id.connectBtn)
        val createAccountBtn = findViewById<TextView>(R.id.createAccountBtn)
        val url ="http://tomnab.fr/todo-api/"

        connectBtn.setOnClickListener {
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()
            makeApiRequest(url, username, password)
        }

        createAccountBtn.setOnClickListener {
            val createAccountActivity = Intent(
                applicationContext,
                CreateAccountActivity::class.java
            )
            startActivity(createAccountActivity)
            finish()
        }
    }



    fun onUserDataError(errorMessage: String?) {
        // Handle the error
        errorConnectAccountTextView!!.visibility = View.VISIBLE
        errorConnectAccountTextView!!.text = errorMessage
    }

    private fun loadUsername(): String? {
        // Charger le nom d'utilisateur enregistré à partir des SharedPreferences
        return sharedPreferences.getString("username", null)
    }

    private fun saveUsername(username: String) {
        // Enregistrer le nom d'utilisateur dans les SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply()
    }

    private fun loadPassword(): String? {
        // Charger le nom d'utilisateur enregistré à partir des SharedPreferences
        return sharedPreferences.getString("password", null)
    }

    private fun savePassword(password: String) {
        // Enregistrer le nom d'utilisateur dans les SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("Password", password)
        editor.apply()
    }



    private fun makeApiRequest(urlAPI: String, user: String, password: String) {
        val requestQueue = Volley.newRequestQueue(this)

        val url = "$urlAPI"+"authenticate?user=$user&password=$password"

        val request = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                // Handle API response here
                if (response.getBoolean("success")) {
                    val token = response.getString("hash")
                    saveToken(token)
                    startActivity(Intent(applicationContext, Menu::class.java))
                    finish()
                }
            },
            { error ->
                // Handle API request error
                Toast.makeText(this@MainActivity, "API connection error: ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }

    private fun saveToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }

}