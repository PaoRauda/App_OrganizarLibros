package edu.udb.mybooks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.udb.mybooks.R
import edu.udb.mybooks.data.Books
import edu.udb.mybooks.data.UserBooks

class UserBooksAdapter(
    private val userBooks: List<UserBooks>,
    private val booksMap: Map<Int, Books>  // Mapa que relaciona bookId con Books
) : RecyclerView.Adapter<UserBooksAdapter.ViewHolder>() {

    private var onItemClick: OnItemClickListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.tvBookTitle)
        val authorTextView: TextView = view.findViewById(R.id.tvBookAuthor)
        val stateTextView: TextView = view.findViewById(R.id.tvBookState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.userbooks_item, parent, false)  // Asegúrate de usar userbook_item.xml
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userBook = userBooks[position]
        val bookDetails = booksMap[userBook.bookId]  // Obtener los detalles del libro usando el bookId

        if (bookDetails != null) {
            holder.titleTextView.text = bookDetails.title
            holder.authorTextView.text = bookDetails.author
            holder.stateTextView.text = userBook.state  // Mostrar el estado del libro para este usuario
        } else {
            holder.titleTextView.text = "Unknown Book"
            holder.authorTextView.text = ""
            holder.stateTextView.text = ""
        }

        // Listener para manejar clics en los elementos de la lista
        holder.itemView.setOnClickListener {
            onItemClick?.onItemClick(userBook, bookDetails)
        }
    }

    override fun getItemCount(): Int {
        return userBooks.size
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClick = listener
    }

    interface OnItemClickListener {
        fun onItemClick(userBook: UserBooks, bookDetails: Books?)
    }

}