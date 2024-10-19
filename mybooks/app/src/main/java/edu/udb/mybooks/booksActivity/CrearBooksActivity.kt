package edu.udb.mybooks.booksActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import edu.udb.mybooks.MainActivity
import edu.udb.mybooks.R
import edu.udb.mybooks.api.BooksApi
import edu.udb.mybooks.data.Books
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class CrearBooksActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var authorEditText: EditText
    private lateinit var crearButton: Button
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_books)

        titleEditText = findViewById(R.id.editTextTitle)
        descriptionEditText = findViewById(R.id.editTextDescription)
        categoryEditText = findViewById(R.id.editTextCategory)
        authorEditText = findViewById(R.id.editTextAuthor)
        crearButton = findViewById(R.id.btnSave)
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_id"

        crearButton.setOnClickListener {
            val titulo = titleEditText.text.toString()
            val descripcion = descriptionEditText.text.toString()
            val categoria = categoryEditText.text.toString()
            val autor = authorEditText.text.toString()

            val book = Books(0, titulo, descripcion, categoria, autor, userId)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://670f43183e71518616571a14.mockapi.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(BooksApi::class.java)

            api.crearBooks(book).enqueue(object : Callback<Books> {
                override fun onResponse(call: Call<Books>, response: Response<Books>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearBooksActivity, "Libro creado exitosamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CrearBooksActivity, MainActivity::class.java))
                    } else {
                        val error = response.errorBody()?.string()
                        Toast.makeText(this@CrearBooksActivity, "Error al crear libro: $error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Books>, t: Throwable) {
                    Toast.makeText(this@CrearBooksActivity, "Error al crear libro", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}