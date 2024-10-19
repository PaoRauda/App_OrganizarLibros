package edu.udb.mybooks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.udb.mybooks.R
import edu.udb.mybooks.data.Books

class BooksAdapter(private val books: List<Books>, private val onAddBookClick: (Books) -> Unit) : RecyclerView.Adapter<BooksAdapter.ViewHolder>() {
    private var onItemClick: OnItemClickListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.tvBookTitle)
        val categoryTextView: TextView = view.findViewById(R.id.tvBookCategory)
        val authorTextView: TextView = view.findViewById(R.id.tvBookAuthor)
        val descriptionTextView: TextView = view.findViewById(R.id.tvBookDescription)
        val buttonAddBook: Button = view.findViewById(R.id.buttonAddBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.books_item, parent, false)  // Asegúrate que book_item.xml es el nombre del layout
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.titleTextView.text = book.title
        holder.categoryTextView.text = book.category
        holder.authorTextView.text = book.author
        holder.descriptionTextView.text = book.description

        holder.buttonAddBook.setOnClickListener {
            onAddBookClick(book)
        }

        // Listener para manejar clics en los elementos de la lista
        holder.itemView.setOnClickListener {
            onItemClick?.onItemClick(book)
        }
    }

    override fun getItemCount(): Int {
        return books.size
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClick = listener
    }

    interface OnItemClickListener {
        fun onItemClick(book: Books)
    }
}