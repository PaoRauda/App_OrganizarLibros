package edu.udb.mybooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import edu.udb.mybooks.api.UsersApi
import edu.udb.mybooks.data.UserBooks
import edu.udb.mybooks.data.Users
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var api: UsersApi


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Crea una instancia de Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://670f43183e71518616571a14.mockapi.io/")  // URL de tu API
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(UsersApi::class.java)

        buttonRegister = findViewById<Button>(R.id.btnRegister)
        buttonRegister.setOnClickListener {
            val email = findViewById<EditText>(R.id.etRegisterEmail).text.toString()
            val password = findViewById<EditText>(R.id.etRegisterPassword).text.toString()
            this.register(email, password)
        }

        buttonLogin = findViewById<Button>(R.id.btnLogin)
        buttonLogin.setOnClickListener {
            this.goToLogin()
        }

        // Validar si existe un usuario activo
        this.checkUser()
    }

    override fun onResume(){
        super.onResume()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onPause(){
        super.onPause()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Obtener UID del usuario registrado en Firebase
                val user = auth.currentUser
                val uid = user?.uid

                // Crear objeto de usuario para API
                val userApi = Users(
                    id = 0,  // O el ID que corresponda si es autogenerado por tu API
                    uid = uid ?: "",  // UID del usuario de Firebase
                    books = emptyList()// Inicializa la lista de libros como corresponda
                )

                // Registrar usuario en la API
                saveUserInApi(userApi)

                // Redirigir a MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserInApi(user: Users) {
        api.crearUsers(user).enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                if (response.isSuccessful) {
                    Log.d("API", "Usuario registrado en la API con éxito")
                } else {
                    Log.e("API", "Error al registrar usuario en la API: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                Log.e("API", "Error al registrar usuario en la API: ${t.message}")
            }
        })
    }


    private fun goToLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun checkUser(){
        //Verificacion del usuario
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if(auth.currentUser != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

}