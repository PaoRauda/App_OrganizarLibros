package edu.udb.mybooks.booksActivity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import edu.udb.mybooks.MainActivity
import edu.udb.mybooks.R
import edu.udb.mybooks.api.BooksApi
import edu.udb.mybooks.data.Books
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ActualizarBooksActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var authorEditText: EditText
    private lateinit var actualizarButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizar_books)

        titleEditText = findViewById(R.id.editTextTitle)
        descriptionEditText = findViewById(R.id.editTextDescription)
        categoryEditText = findViewById(R.id.editTextCategory)
        authorEditText = findViewById(R.id.editTextAuthor)
        actualizarButton = findViewById(R.id.btnUpdate)

        // Recibe el ID y otros detalles de la actividad anterior
        val libroId = intent.getIntExtra("libro_id", -1)
        val titulo = intent.getStringExtra("title").toString()
        val descripcion = intent.getStringExtra("description").toString()
        val categoria = intent.getStringExtra("category").toString()
        val autor = intent.getStringExtra("author").toString()
        val userId = intent.getStringExtra("user_id").toString()

        titleEditText.setText(titulo)
        descriptionEditText.setText(descripcion)
        categoryEditText.setText(categoria)
        authorEditText.setText(autor)

        actualizarButton.setOnClickListener {
            val libroActualizado = Books(
                libroId,
                titleEditText.text.toString(),
                descriptionEditText.text.toString(),
                categoryEditText.text.toString(),
                authorEditText.text.toString(),
                userId
            )

            val retrofit = Retrofit.Builder()
                .baseUrl("https://670f43183e71518616571a14.mockapi.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(BooksApi::class.java)

            api.actualizarBooks(libroId, libroActualizado).enqueue(object : Callback<Books> {
                override fun onResponse(call: Call<Books>, response: Response<Books>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ActualizarBooksActivity, "Libro actualizado correctamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ActualizarBooksActivity, MainActivity::class.java))
                    } else {
                        val error = response.errorBody()?.string()
                        Toast.makeText(this@ActualizarBooksActivity, "Error al actualizar libro: $error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Books>, t: Throwable) {
                    Toast.makeText(this@ActualizarBooksActivity, "Error al actualizar libro", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}