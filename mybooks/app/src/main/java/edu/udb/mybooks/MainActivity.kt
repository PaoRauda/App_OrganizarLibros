package edu.udb.mybooks

import android.content.Intent
import android.graphics.Insets.add
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import edu.udb.mybooks.adapter.BooksAdapter
import edu.udb.mybooks.adapter.UserBooksAdapter
import edu.udb.mybooks.api.BooksApi
import edu.udb.mybooks.api.UsersApi
import edu.udb.mybooks.booksActivity.ActualizarBooksActivity
import edu.udb.mybooks.booksActivity.CrearBooksActivity
import edu.udb.mybooks.data.Books
import edu.udb.mybooks.data.UserBooks
import edu.udb.mybooks.data.Users
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BooksAdapter
    private lateinit var booksApi: BooksApi
    private lateinit var usersApi: UsersApi
    private lateinit var editTextResourceId: EditText
    private lateinit var userId: String

    //Credenciales predeterminadas de autenticación
    val auth_username = "admin"
    val auth_password = "admin123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val agregarBook: FloatingActionButton = findViewById<FloatingActionButton>(R.id.fab_agregar)
        val buttonMostrarTodos: FloatingActionButton = findViewById<FloatingActionButton>(R.id.buttonMostrarTodos)
        editTextResourceId = findViewById(R.id.editTextResourceId)
        val buttonBuscar: Button = findViewById(R.id.buttonBuscar)
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_id"

        //Configuración de la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Crea una instancia de Retrofit con el cliente OkHttpClient
        val retrofit = Retrofit.Builder()
            .baseUrl("https://670f43183e71518616571a14.mockapi.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crea una instancia del servicio que utiliza la autenticación HTTP básica
        booksApi = retrofit.create(BooksApi::class.java)
        usersApi = retrofit.create(UsersApi::class.java)

        //Carga el listado de libros
        cargarDatos(booksApi)


        /*
        * BUTTON LISTENERS
        */

        buttonBuscar.setOnClickListener {
            val resourceIdStr = editTextResourceId.text.toString()
            val resourceId = resourceIdStr.toIntOrNull()
            if (resourceId != null) {
                buscarRecurso(resourceId, booksApi)
            } else {
                Toast.makeText(this, "Por favor, ingrese un ID válido", Toast.LENGTH_SHORT).show()
            }
        }

        buttonMostrarTodos.setOnClickListener(View.OnClickListener {
            cargarDatos(booksApi)
        })

        agregarBook.setOnClickListener(View.OnClickListener {
            val i = Intent(getBaseContext(), CrearBooksActivity::class.java)
            i.putExtra("auth_username", auth_username)
            i.putExtra("auth_password", auth_password)
            startActivity(i)
        })
    }

    override fun onResume() {
        super.onResume()
        cargarDatos(booksApi)
    }

    /*
    *
    * FUNCIONES DE CRUD PARA BOOKS
    *
    * */

    private fun cargarDatos(api: BooksApi) {
        val call = api.obtenerBooks()
        call.enqueue(object : Callback<List<Books>> {
            override fun onResponse(call: Call<List<Books>>, response: Response<List<Books>>) {
                if (response.isSuccessful) {
                    val resources = response.body()
                    if (resources != null) {
                        adapter = BooksAdapter(resources) { book ->
                            // Mostrar AlertDialog para seleccionar el estado del libro
                            val estados = arrayOf("Obtenido", "Deseado")
                            var estadoSeleccionado = estados[0] // Valor por defecto

                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Selecciona el estado")
                                .setSingleChoiceItems(estados, 0) { _, which ->
                                    estadoSeleccionado = estados[which]
                                }
                                .setPositiveButton("Agregar") { _, _ ->
                                    // Aquí agregamos el libro a la lista de UserBooks del usuario con el estado seleccionado
                                    val nuevoLibro = UserBooks(bookId = book.id, state = estadoSeleccionado)

                                    // Supongamos que tienes una función para agregar este libro a la lista del usuario
                                    agregarLibroAlUsuario(nuevoLibro)
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }


                        recyclerView.adapter = adapter

                        // Establecemos el escuchador de clics en el adaptador
                        adapter.setOnItemClickListener(object : BooksAdapter.OnItemClickListener {
                            override fun onItemClick(book: Books) {
                                // Verifica si el uid del usuario actual coincide con el createdBy
                                val opciones = if (book.createdBy == userId) {
                                    arrayOf("Modificar recurso", "Eliminar recurso")
                                } else {
                                    arrayOf("") // Solo permitir ver el recurso
                                }

                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle(book.title)
                                    .setItems(opciones) { dialog, index ->
                                        when (index) {
                                            0 -> {
                                                if (book.createdBy == userId) {
                                                    Modificar(book)
                                                } else {
                                                    // Si no tiene permiso, no hacer nada o mostrar un mensaje
                                                    Toast.makeText(this@MainActivity, "No tienes permiso para modificar este recurso.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            1 -> {
                                                if (book.createdBy == userId) {
                                                    eliminarRecurso(book, api)
                                                } else {
                                                    // Si no tiene permiso, no hacer nada o mostrar un mensaje
                                                    Toast.makeText(this@MainActivity, "No tienes permiso para eliminar este recurso.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            }
                        })
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Error al obtener los recursos: $error")
                    Toast.makeText(
                        this@MainActivity,
                        "Error al obtener los recursos 1",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Books>>, t: Throwable) {
                Log.e("API", "Error al obtener los recursos: ${t.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error al obtener los recursos 2",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun buscarRecurso(resourceId: Int, api: BooksApi) {
        val llamada = api.obtenerBooksPorId(resourceId)
        llamada.enqueue(object : Callback<Books> {
            override fun onResponse(call: Call<Books>, response: Response<Books>) {
                if (response.isSuccessful && response.body() != null) {
                    val resourceData = response.body()!!

                    // Aquí puedes crear una lista con un solo recurso para el adaptador
                    val resourcesData = listOf(resourceData) // Convertir a lista

                    // Inicializamos el adapter con la lista de recursos
                    adapter = BooksAdapter(resourcesData) { book ->
                        // Mostrar AlertDialog para seleccionar el estado del libro
                        val estados = arrayOf("Obtenido", "Deseado")
                        var estadoSeleccionado = estados[0] // Valor por defecto

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Selecciona el estado")
                            .setSingleChoiceItems(estados, 0) { _, which ->
                                estadoSeleccionado = estados[which]
                            }
                            .setPositiveButton("Agregar") { _, _ ->
                                // Aquí agregamos el libro a la lista de UserBooks del usuario con el estado seleccionado
                                val nuevoLibro = UserBooks(bookId = book.id, state = estadoSeleccionado)

                                // Supongamos que tienes una función para agregar este libro a la lista del usuario
                                agregarLibroAlUsuario(nuevoLibro)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }

                    recyclerView.adapter = adapter

                    // Establecemos el escuchador de clics en el adaptador
                    adapter.setOnItemClickListener(object : BooksAdapter.OnItemClickListener {
                        override fun onItemClick(book: Books) {
                            val opciones = arrayOf("Modificar recurso", "Eliminar recurso")

                            AlertDialog.Builder(this@MainActivity)
                                .setTitle(book.title)
                                .setItems(opciones) { dialog, index ->
                                    when (index) {
                                        0 -> Modificar(book)
                                        1 -> eliminarRecurso(book, api)
                                    }
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    })
                } else {
                    Log.e("API", "Error en la respuesta: ${response.message()}")
                    Toast.makeText(this@MainActivity, "Error en la respuesta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Books>, t: Throwable) {
                Log.e("API", "Error al obtener el recurso: ${t.message}")
                Toast.makeText(this@MainActivity, "Error al obtener el recurso", Toast.LENGTH_SHORT).show()
            }
        })
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
                    Toast.makeText(this@MainActivity, "Recurso eliminado", Toast.LENGTH_SHORT).show()
                    cargarDatos(api)
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Error al eliminar recurso : $error")
                    Toast.makeText(this@MainActivity, "Error al eliminar recurso 1", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API", "Error al eliminar recurso : $t")
                Toast.makeText(this@MainActivity, "Error al eliminar recurso 2", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /*
    *
    * FUNCIONES PARA AÑADIR A MYBOOKS
    *
    * */

    private fun agregarLibroAlUsuario(nuevoLibro: UserBooks) {

        cargarUsers(usersApi) { Users ->
            if (Users != null) {

                val listaActualizadaDeLibros = Users.books.toMutableList().apply {
                    // Verifica si el libro no está ya en la lista antes de agregarlo
                    if (!any { it.bookId == nuevoLibro.bookId }) {
                        add(nuevoLibro)
                    } else {
                        Toast.makeText(this@MainActivity, "Este libro ya esta en su listado", Toast.LENGTH_SHORT).show()
                    }
                }

                val userActualizado = Users(
                    id = Users.id,
                    uid = Users.uid,
                    books = listaActualizadaDeLibros
                )

                usersApi.actualizarUsers(Users.id, userActualizado).enqueue(object : Callback<Users> {
                    override fun onResponse(call: Call<Users>, response: Response<Users>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Libro actualizado correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        } else {
                            val error = response.errorBody()?.string()
                            Toast.makeText(this@MainActivity, "Error al actualizar libro: $error", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Users>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Error al actualizar libro", Toast.LENGTH_SHORT).show()
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
                        this@MainActivity,
                        "Error al obtener los recursos 1",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Users>>, t: Throwable) {
                Log.e("API", "Error al obtener los recursos: ${t.message}")
                Toast.makeText(
                    this@MainActivity,
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