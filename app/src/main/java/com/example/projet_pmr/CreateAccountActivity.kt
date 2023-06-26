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
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class CreateAccountActivity : AppCompatActivity() {

    private var errorCreateAccountTextView: TextView? = null
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)

        val alreadyHasAccount = findViewById<TextView>(R.id.alreadyHasAccountBtn)
        val createAccountBtn = findViewById<Button>(R.id.createAccountBtn)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        errorCreateAccountTextView = findViewById<TextView>(R.id.errorCreateAccountTextView)
        val url ="http://tomnab.fr/todo-api/"

        createAccountBtn.setOnClickListener(View.OnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            createAccount(url, username, password)


        })
        alreadyHasAccount.setOnClickListener(View.OnClickListener {
            val connectToAccountActivity = Intent(applicationContext, MainActivity::class.java)
            startActivity(connectToAccountActivity)
        })
    }

    fun onApiResponse(response: JSONObject) {
        var success: Boolean? = null

        try {
            success = response.getBoolean("success")
            if (success == true) {

                finish()
            } else {
                val error = " Erreur lors de la création du compte"
                errorCreateAccountTextView!!.visibility = View.VISIBLE
                errorCreateAccountTextView!!.text = error
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun createAccount(urlAPI: String, username: String, password: String) {
        val requestQueue = Volley.newRequestQueue(this)

        val url = "$urlAPI" + "users?pseudo=$username&pass=$password"

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, null,
            Response.Listener { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        connectAccount(urlAPI, username, password) { token ->
                            saveToken(token)
                            startActivity(Intent(applicationContext, Menu::class.java))
                        }
                    } else {
                        val error = "Erreur lors de la création du compte"
                        errorCreateAccountTextView?.visibility = View.VISIBLE
                        errorCreateAccountTextView?.text = error
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "API connection error: ${error.message}", Toast.LENGTH_LONG).show()
                System.out.println(error.message)
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = getToken()
                headers["hash"] = token
                return headers
            }
        }
        requestQueue.add(request)
    }


    private fun connectAccount(urlAPI: String, user: String, password: String, callback: (String) -> Unit) {
        val requestQueue = Volley.newRequestQueue(this)

        val url = urlAPI +"authenticate?user=$user&password=$password"

        val request = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                val token = response.getString("hash")
                callback(token)
            },
            { error ->
                Toast.makeText(this@CreateAccountActivity, "API connection error: ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }


    private fun saveToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }
    private fun getToken(): String {
        val sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE)
        return sharedPreferences.getString("token", "") ?: ""
    }


}

