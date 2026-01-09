package com.example.tetstviews.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tetstviews.R
import com.example.tetstviews.domain.model.Event
import java.text.SimpleDateFormat
import java.util.*

sealed class ListItem {
    data class DateHeader(val date: String, val dateMillis: Long) : ListItem()
    data class EventItem(val event: Event) : ListItem()
}

class GroupedEventAdapter(
    private val context: android.content.Context,
    private val onEventClick: (Event) -> Unit,
    private val onEventToggleComplete: (Event) -> Unit,
    private val onEventDelete: (Event) -> Unit
) : BaseAdapter() {

    private var items: List<ListItem> = listOf()

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_EVENT = 1

    fun updateList(events: List<Event>) {
        val groupedItems = mutableListOf<ListItem>()
        var lastDate = ""

        events.forEach { event ->
            val dateLabel = getDateLabel(event.dateMillis)
            if (dateLabel != lastDate) {
                groupedItems.add(ListItem.DateHeader(dateLabel, event.dateMillis))
                lastDate = dateLabel
            }
            groupedItems.add(ListItem.EventItem(event))
        }

        items = groupedItems
        notifyDataSetChanged()
    }

    private fun isToday(dateMillis: Long): Boolean {
        val today = Calendar.getInstance()
        val eventDate = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return today.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(dateMillis: Long): Boolean {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val eventDate = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return tomorrow.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDateLabel(dateMillis: Long): String {
        return when {
            isToday(dateMillis) -> "Сегодня"
            isTomorrow(dateMillis) -> "Завтра"
            else -> formatDate(dateMillis)
        }
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        return dateFormat.format(Date(millis))
    }

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is ListItem.DateHeader -> item.dateMillis
            is ListItem.EventItem -> item.event.id
        }
    }

    override fun getViewTypeCount() = 2

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateHeader -> VIEW_TYPE_HEADER
            is ListItem.EventItem -> VIEW_TYPE_EVENT
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return when (val item = items[position]) {
            is ListItem.DateHeader -> getHeaderView(item, convertView, parent)
            is ListItem.EventItem -> getEventView(item.event, convertView, parent)
        }
    }

    private fun getHeaderView(header: ListItem.DateHeader, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_date_header, parent, false)

        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val ivDateIcon = view.findViewById<ImageView>(R.id.ivDateIcon)

        tvDateHeader?.text = header.date

        // Меняем иконку в зависимости от дня
        when (header.date) {
            "Сегодня" -> {
                ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                tvDateHeader?.setTextColor(ContextCompat.getColor(context, R.color.greenPrimary))
            }
            "Завтра" -> {
                ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                tvDateHeader?.setTextColor(ContextCompat.getColor(context, R.color.orangeLight))
            }
            else -> {
                ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                tvDateHeader?.setTextColor(ContextCompat.getColor(context, R.color.grey))
            }
        }

        return view
    }

    private fun getEventView(event: Event, convertView: View?, parent: ViewGroup?): View {
        val view = convertView?.takeIf { it.findViewById<TextView>(R.id.tvEventTitle) != null }
            ?: LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)

        val tvEventTitle = view.findViewById<TextView>(R.id.tvEventTitle)
        val tvEventDate = view.findViewById<TextView>(R.id.tvEventDate)
        val tvEventTime = view.findViewById<TextView>(R.id.tvEventTime)
        val tvEventDescription = view.findViewById<TextView>(R.id.tvEventDescription)
        val checkboxComplete = view.findViewById<CheckBox>(R.id.checkboxComplete)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

        tvEventTitle?.text = event.title ?: ""
        tvEventDate?.visibility = View.GONE

        val timeValue = event.time ?: ""
        if (tvEventTime != null) {
            if (timeValue.isNotEmpty()) {
                tvEventTime.visibility = View.VISIBLE
                tvEventTime.text = "⏰ $timeValue"
            } else {
                tvEventTime.visibility = View.GONE
            }
        }

        val descValue = event.description ?: ""
        if (tvEventDescription != null) {
            if (descValue.isNotEmpty()) {
                tvEventDescription.visibility = View.VISIBLE
                tvEventDescription.text = descValue
            } else {
                tvEventDescription.visibility = View.GONE
            }
        }

        if (checkboxComplete != null) {
            checkboxComplete.setOnCheckedChangeListener(null)
            checkboxComplete.isChecked = event.isCompleted
            checkboxComplete.setOnCheckedChangeListener { _, _ ->
                onEventToggleComplete(event)
            }
        }

        if (tvEventTitle != null) {
            if (event.isCompleted) {
                tvEventTitle.paintFlags = tvEventTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvEventTitle.alpha = 0.5f
            } else {
                tvEventTitle.paintFlags = tvEventTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvEventTitle.alpha = 1f
            }
        }

        btnDelete?.setOnClickListener {
            onEventDelete(event)
        }

        view.setOnClickListener {
            onEventClick(event)
        }

        return view
    }
}

