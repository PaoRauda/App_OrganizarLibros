package edu.udb.mybooks.data

data class Users(
    val id: Int,
    val uid: String,
    val books: List<UserBooks>
)
