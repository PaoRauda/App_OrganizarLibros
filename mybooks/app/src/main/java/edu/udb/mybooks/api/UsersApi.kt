package edu.udb.mybooks.api

import edu.udb.mybooks.data.Users
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UsersApi {

    @GET("app-libros/users")
    fun obtenerUsers() : Call<List<Users>>

    @GET("app-libros/users/{id}")
    fun obtenerUsersPorId(@Path("id") id: Int): Call<Users>

    @POST("app-libros/users")
    fun crearUsers(@Body users: Users): Call<Users>

    @PUT("app-libros/users/{id}")
    fun actualizarUsers(@Path("id") id: Int, @Body users: Users): Call<Users>

    @DELETE("app-libros/users/{id}")
    fun eliminarUsers(@Path("id") id: Int): Call<Void>

}