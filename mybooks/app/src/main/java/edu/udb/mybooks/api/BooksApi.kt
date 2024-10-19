package edu.udb.mybooks.api

import edu.udb.mybooks.data.Books
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BooksApi {

    @GET("app-libros/books")
    fun obtenerBooks() : Call<List<Books>>

    @GET("app-libros/books/{id}")
    fun obtenerBooksPorId(@Path("id") id: Int): Call<Books>

    @POST("app-libros/books")
    fun crearBooks(@Body books: Books): Call<Books>

    @PUT("app-libros/books/{id}")
    fun actualizarBooks(@Path("id") id: Int, @Body books: Books): Call<Books>

    @DELETE("app-libros/books/{id}")
    fun eliminarBooks(@Path("id") id: Int): Call<Void>

}