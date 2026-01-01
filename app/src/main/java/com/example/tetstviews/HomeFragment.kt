package com.example.tetstviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import kotlin.random.Random

class HomeFragment : Fragment() {

    private lateinit var tvTipEmoji: TextView
    private lateinit var tvTipTitle: TextView
    private lateinit var tvTipText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTipEmoji = view.findViewById(R.id.tvTipEmoji)
        tvTipTitle = view.findViewById(R.id.tvTipTitle)
        tvTipText = view.findViewById(R.id.tvTipText)

        loadRandomTip()
    }

    private fun loadRandomTip() {
        val emojis = resources.getStringArray(R.array.tip_emojis)
        val titles = resources.getStringArray(R.array.tip_titles)
        val texts = resources.getStringArray(R.array.tip_texts)

        val randomIndex = Random.nextInt(emojis.size)

        tvTipEmoji.text = emojis[randomIndex]
        tvTipTitle.text = titles[randomIndex]
        tvTipText.text = texts[randomIndex]
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}