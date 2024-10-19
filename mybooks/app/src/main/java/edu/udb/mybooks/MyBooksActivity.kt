package edu.udb.mybooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import edu.udb.mybooks.adapter.BooksAdapter
import edu.udb.mybooks.adapter.UserBooksAdapter
import edu.udb.mybooks.api.BooksApi
import edu.udb.mybooks.api.UsersApi
import edu.udb.mybooks.booksActivity.ActualizarBooksActivity
import edu.udb.mybooks.data.Books
import edu.udb.mybooks.data.UserBooks
import edu.udb.mybooks.data.Users
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyBooksActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserBooksAdapter
    private lateinit var apiBooks: BooksApi
    private lateinit var apiUsers: UsersApi
    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mybooks)

        //Configuración de la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val buttonMostrarTodos: FloatingActionButton = findViewById<FloatingActionButton>(R.id.buttonMostrarTodos)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://670f43183e71518616571a14.mockapi.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiBooks = retrofit.create(BooksApi::class.java)
        apiUsers = retrofit.create(UsersApi::class.java)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_id"

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)


        cargarDatos(apiBooks)


        /*
        * BUTTON LISTENERS
        */

        buttonMostrarTodos.setOnClickListener(View.OnClickListener {
            cargarDatos(apiBooks)
        })
    }

    override fun onResume() {
        super.onResume()
        cargarDatos(apiBooks)
    }

    private fun cargarDatos(api: BooksApi) {
        val call = api.obtenerBooks()
        call.enqueue(object : Callback<List<Books>> {
            override fun onResponse(call: Call<List<Books>>, response: Response<List<Books>>) {
                if (response.isSuccessful) {
                    val booksList = response.body()
                    if (booksList != null) {
                        // Obtener los UserBooks del usuario
                        obtenerUserBooks(apiUsers) { userBooksList ->
                            if (userBooksList.isNotEmpty()) {
                                // Crear un mapa de Books
                                val booksMap = booksList.associateBy { it.id }

                                // Inicializar el adaptador
                                adapter = UserBooksAdapter(userBooksList, booksMap)
                                recyclerView.adapter = adapter

                                // Establecer el escuchador de clics en el adaptador
                                adapter.setOnItemClickListener(object : UserBooksAdapter.OnItemClickListener {
                                    override fun onItemClick(userBook: UserBooks, bookDetails: Books?) {
                                        manejarClicEnLibro(userBook, bookDetails)
                                    }
                                })
                            } else {
                                Log.d("UserBooks", "No se encontraron libros para este usuario")
                            }
                        }
                    } else {
                        Log.e("API", "Error: Lista de libros vacía")
                    }
                } else {
                    Log.e("API", "Error al obtener los libros: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<Books>>, t: Throwable) {
                Log.e("API", "Error al obtener los libros: ${t.message}")
            }
        })
    }

    private fun obtenerUserBooks(api: UsersApi, onResult: (List<UserBooks>) -> Unit): List<UserBooks> {
        val call = api.obtenerUsers() // Supón que este método obtiene todos los usuarios con sus libros
        call.enqueue(object : Callback<List<Users>> {
            override fun onResponse(call: Call<List<Users>>, response: Response<List<Users>>) {
                if (response.isSuccessful) {
                    val usersList = response.body()
                    if (usersList != null) {
                        // Filtrar los libros del usuario por su uid
                        val user = usersList.find { it.uid == userId }
                        if (user != null) {
                            onResult(user.books) // Aquí obtienes los libros del usuario filtrado
                        } else {
                            Log.e("API", "Error: Usuario no encontrado con uid $userId")
                            onResult(emptyList()) // Retorna una lista vacía si no se encuentra el usuario
                        }
                    } else {
                        Log.e("API", "Error: Lista de usuarios vacía")
                        onResult(emptyList())
                    }
                } else {
                    Log.e("API", "Error al obtener los usuarios 1: ${response.errorBody()?.string()}")
                    onResult(emptyList())
                }
            }

            override fun onFailure(call: Call<List<Users>>, t: Throwable) {
                Log.e("API", "Error al obtener los usuarios 2: ${t.message}")
                onResult(emptyList())
            }
        })
        return emptyList()
    }


    private fun manejarClicEnLibro(userBook: UserBooks, bookDetails: Books?) {
        if (bookDetails != null) {
            // Verifica si el uid del usuario actual coincide con el createdBy del libro
            val opciones = if (bookDetails.createdBy == userId) {
                arrayOf("Cambiar estado", "Eliminar de mi lista", "Modificar recurso", "Eliminar recurso")
            } else {
                arrayOf("Cambiar estado", "Eliminar de mi lista") // Solo permitir ver el recurso
            }

            AlertDialog.Builder(this)
                .setTitle(bookDetails.title)
                .setItems(opciones) { dialog, index ->
                    when (index) {
                        0 -> {
                            cambiarEstado(bookDetails.id )
                        }
                        1 -> {
                            eliminarMyBooks(bookDetails.id )
                        }
                        2 -> {
                            // Opción de modificar el recurso
                            if (bookDetails.createdBy == userId) {
                                Modificar(bookDetails) // Llama a la función para modificar
                            } else {
                                Toast.makeText(this@MyBooksActivity, "No tienes permiso para modificar este recurso.", Toast.LENGTH_SHORT).show()
                            }
                        } 3 -> {
                        // Opción de eliminar el recurso
                        if (bookDetails.createdBy == userId) {
                            eliminarRecurso(bookDetails, apiBooks) // Llama a la función para eliminar
                        } else {
                            Toast.makeText(this@MyBooksActivity, "No tienes permiso para eliminar este recurso.", Toast.LENGTH_SHORT).show()
                        }
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun Modificar(book: Books) {
        // Creamos un intent para ir a la actividad de actualización de alumnos
        val i = Intent(getBaseContext(), ActualizarBooksActivity::class.java)
        // Pasamos el ID del alumno seleccionado a la actividad de actualización
        i.putExtra("libro_id", book.id)
        i.putExtra("title", book.title)
        i.putExtra("description", book.description)
        i.putExtra("category", book.category)
        i.putExtra("author", book.author)
        i.putExtra("user_id", book.createdBy)
        // Iniciamos la actividad de actualización
        startActivity(i)
    }

    private fun eliminarRecurso(book: Books, api: BooksApi) {
        //Log.e("API", "id : $resource")
        val llamada = api.eliminarBooks(book.id)
        llamada.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MyBooksActivity, "Recurso eliminado", Toast.LENGTH_SHORT).show()
                    cargarDatos(api)
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Error al eliminar recurso : $error")
                    Toast.makeText(this@MyBooksActivity, "Error al eliminar recurso 1", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API", "Error al eliminar recurso : $t")
                Toast.makeText(this@MyBooksActivity, "Error al eliminar recurso 2", Toast.LENGTH_SHORT).show()
            }
        })
    }


    /*
    *
    * FUNCIONES PARA AÑADIR A MYBOOKS
    *
    * */
    private fun cambiarEstado(bookId: Int) {
        cargarUsers(apiUsers) { Users ->
            if (Users != null) {

                val librosActualizados = Users.books.map { libro ->
                    if (libro.bookId == bookId) {
                        // Cambiar el estado del libro
                        libro.copy(state = if (libro.state == "Obtenido") "Deseado" else "Obtenido")
                    } else {
                        libro // Retorna el libro sin cambios
                    }
                }

                val userActualizado = Users(
                    id = Users.id,
                    uid = Users.uid,
                    books = librosActualizados
                )

                apiUsers.actualizarUsers(Users.id, userActualizado).enqueue(object : Callback<Users> {
                    override fun onResponse(call: Call<Users>, response: Response<Users>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MyBooksActivity, "Libro actualizado correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MyBooksActivity, MainActivity::class.java))
                        } else {
                            val error = response.errorBody()?.string()
                            Toast.makeText(this@MyBooksActivity, "Error al actualizar libro: $error", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Users>, t: Throwable) {
                        Toast.makeText(this@MyBooksActivity, "Error al actualizar libro", Toast.LENGTH_SHORT).show()
                    }
                })


            } else {
                Log.d("UserBooks", "No se encontraron libros para este usuario")
            }
        }

    }

    private fun eliminarMyBooks(bookIdToRemove : Int) {

        cargarUsers(apiUsers) { Users ->
            if (Users != null) {

                val listaActualizadaDeLibros = Users.books.filter { it.bookId != bookIdToRemove }

                val userActualizado = Users(
                    id = Users.id,
                    uid = Users.uid,
                    books = listaActualizadaDeLibros
                )

                apiUsers.actualizarUsers(Users.id, userActualizado).enqueue(object : Callback<Users> {
                    override fun onResponse(call: Call<Users>, response: Response<Users>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MyBooksActivity, "Libro actualizado correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MyBooksActivity, MainActivity::class.java))
                        } else {
                            val error = response.errorBody()?.string()
                            Toast.makeText(this@MyBooksActivity, "Error al actualizar libro: $error", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Users>, t: Throwable) {
                        Toast.makeText(this@MyBooksActivity, "Error al actualizar libro", Toast.LENGTH_SHORT).show()
                    }
                })


            } else {
                Log.d("UserBooks", "No se encontraron libros para este usuario")
            }
        }
    }

    private fun cargarUsers(api: UsersApi, onResult: (Users?) -> Unit) {
        val call = api.obtenerUsers()
        call.enqueue(object : Callback<List<Users>> {
            override fun onResponse(call: Call<List<Users>>, response: Response<List<Users>>) {
                if (response.isSuccessful) {
                    val users = response.body()
                    if (users != null) {

                        val user = users.find { it.uid == userId }

                        if (user != null) {
                            onResult(user) // Aquí obtienes los libros del usuario filtrado
                        } else {
                            Log.e("API", "Error: Usuario no encontrado con uid $userId")
                            onResult(null) // Notificas que no se encontraron usuarios
                        }
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Error al obtener los recursos: $error")
                    Toast.makeText(
                        this@MyBooksActivity,
                        "Error al obtener los recursos 1",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Users>>, t: Throwable) {
                Log.e("API", "Error al obtener los recursos: ${t.message}")
                Toast.makeText(
                    this@MyBooksActivity,
                    "Error al obtener los recursos 2",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    /*
    * TOOLBAR FUNCTIONS
    */

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.activity_view_main -> {
                goToMain()
                true
            }
            R.id.action_view_mybooks -> {
                goToMyBooks()
                true
            }
            R.id.action_sign_out -> {
                FirebaseAuth.getInstance().signOut().also {
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    private fun goToMyBooks() {
        val intent = Intent(this, MyBooksActivity::class.java)
        startActivity(intent)
    }
}