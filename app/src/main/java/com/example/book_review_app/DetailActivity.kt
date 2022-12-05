package com.example.book_review_app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.book_review_app.databinding.ActivityDetailBinding
import com.example.book_review_app.model.Book
import com.example.book_review_app.model.Review

class DetailActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var db: AppDatabase

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)!!

        val model = intent.getParcelableExtra<Book>("bookModel")
        binding.titleTextView.text = model?.title.orEmpty()
        binding.descriptionTextView.text = model?.description.orEmpty()

        Glide.with(binding.coverImageView.context)
            .load(model?.coverLargeUrl.orEmpty())
            .into(binding.coverImageView)

        Thread {
            val reviewModel = model?.id?.toInt()?.let { db.reviewDao().getOneReview(it) }
            runOnUiThread {
                binding.reviewEditText.setText(reviewModel?.review.orEmpty())
            }
        }.start()

        binding.saveButton.setOnClickListener {
            Thread {
                db.reviewDao().saveReview(
                    Review(model?.id?.toInt() ?: 0,
                    binding.reviewEditText.text.toString()
                    )
                )
            }.start()

            Toast.makeText(this, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }

    }
}