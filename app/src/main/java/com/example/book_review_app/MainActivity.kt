package com.example.book_review_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.book_review_app.Adapter.BookAdapter
import com.example.book_review_app.Adapter.HistoryAdapter
import com.example.book_review_app.api.BookService
import com.example.book_review_app.databinding.ActivityMainBinding
import com.example.book_review_app.model.BestSellerDto
import com.example.book_review_app.model.History
import com.example.book_review_app.model.SearchBookDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BookAdapter
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var bookService: BookService

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBookRecyclerView()
        initHistoryRecyclerView()
        initSearchEditText()

        db = AppDatabase.getInstance(this)!!

        val retrofit = Retrofit.Builder()
            .baseUrl("https://book.interpark.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        bookService = retrofit.create(BookService::class.java)

        bookService.getBestSellerBooks(getString(R.string.interParkAPIKey))
            .enqueue(object: Callback<BestSellerDto> {
                override fun onResponse(
                    call: Call<BestSellerDto>,
                    response: Response<BestSellerDto>
                ) {
                    if (response.isSuccessful.not()) {
                        Log.e(TAG, "NOT!! SUCCESS")
                        return
                    }
                    adapter.submitList(response.body()?.books.orEmpty())
                }

                override fun onFailure(call: Call<BestSellerDto>, t: Throwable) {
                    Log.e(TAG, t.toString())
                }

            })


    }

    private fun search(keyword: String, isAlreadyExist: Boolean) {
        bookService.getBooksByName(getString(R.string.interParkAPIKey), keyword)
            .enqueue(object: Callback<SearchBookDto> {
                override fun onResponse(
                    call: Call<SearchBookDto>,
                    response: Response<SearchBookDto>
                ) {
                    hideHistoryView()
                    if (isAlreadyExist.not()) saveSearchKeyword(keyword)
                    if (response.isSuccessful.not()) {
                        Log.e(TAG, "NOT!! SUCCESS")
                        return
                    }
                    if(response.body()?.books.isNullOrEmpty()) hideBookRecyclerView()
                    else {
                        adapter.submitList(response.body()?.books)
                        showBookRecyclerView()
                    }
                }

                override fun onFailure(call: Call<SearchBookDto>, t: Throwable) {
                    hideHistoryView()
                    Log.e(TAG, t.toString())
                }

            })
    }

    private fun initBookRecyclerView() {
        adapter = BookAdapter(itemClickedLitener = {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("bookModel", it)
            startActivity(intent)
        })
        binding.bookRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.bookRecyclerView.adapter = adapter
    }

    private fun initHistoryRecyclerView() {
        historyAdapter = HistoryAdapter(
            historyDeleteClickedListener = {
            deleteSearchKeyword(it)
        }, historyKeywordClicktedListener = {
            search(it, true)
        })
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = historyAdapter
        hideHistoryView()
    }

    private fun initSearchEditText() {
        binding.searchEditText.setOnTouchListener { view, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_DOWN) {
                showHistoryView()
            }
            return@setOnTouchListener false
        }

        binding.searchEditText.setOnKeyListener { view, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == MotionEvent.ACTION_DOWN) {
                search(binding.searchEditText.text.toString(), false)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    private fun showHistoryView() {
        Thread {
            val keywords = db.historyDao().getAll().reversed()

            runOnUiThread {
                if (keywords.size == 0){
                    binding.historyRecyclerView.isVisible = false
                    return@runOnUiThread
                }
                binding.historyRecyclerView.isVisible = true
                historyAdapter.submitList(keywords.orEmpty())
            }
        }.start()
    }

    private fun hideHistoryView() {
        binding.historyRecyclerView.isVisible = false
    }

    private fun hideBookRecyclerView() {
        binding.bookRecyclerView.isVisible = false
    }

    private fun showBookRecyclerView() {
        binding.bookRecyclerView.isVisible = true
    }

    private fun saveSearchKeyword(keyword: String) {
        Thread {
            db.historyDao()?.insertHistory(History(null, keyword))
        }.start()
    }

    private fun deleteSearchKeyword(keyword: String) {
        Thread {
            db.historyDao()?.delete(keyword)
            showHistoryView()
        }.start()
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}