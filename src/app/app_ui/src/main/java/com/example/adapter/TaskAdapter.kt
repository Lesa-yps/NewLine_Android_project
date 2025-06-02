package com.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.view.DetailActivity
import com.example.R
import com.example.models.Task
import com.example.models.TaskPriority
import com.example.models.TaskStatus

class TaskAdapter(private val items: MutableList<Task>):
        RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Объект, который хранит ссылки на элементы карточки
    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textDeadline: TextView = view.findViewById(R.id.textDeadline)
        val imageIconLeft: ImageView = view.findViewById(R.id.imageIconLeft)
        val imageIconRight: ImageView = view.findViewById(R.id.imageIconRight)
    }

    // Создание новой карточки
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_item, parent, false)
        return TaskViewHolder(view)
    }

    // Заполнение карточки данными
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // Получение элемента из списка
        val item = items[position]
        // Формирование текста (заголовка)
        holder.textTitle.text = item.name.toString()
        // Дата дедлайна
        holder.textDeadline.text = item.deadlineDay

        // Установка левой картинки в зависимости от статуса задачи
        val imageStatusRes = when (item.status) {
            TaskStatus.TODO -> R.drawable.ic_todo
            TaskStatus.IN_PROGRESS -> R.drawable.ic_in_progress
            TaskStatus.DONE -> R.drawable.ic_done
        }
        holder.imageIconLeft.setImageResource(imageStatusRes)

        // Установка правой картинки в зависимости от сложности задачи
        val imageComplexityRes = when (item.priority) {
            TaskPriority.LOW -> R.drawable.ic_low
            TaskPriority.MEDIUM -> R.drawable.ic_medium
            TaskPriority.HIGH -> R.drawable.ic_high
        }
        holder.imageIconRight.setImageResource(imageComplexityRes)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = DetailActivity.Companion.createIntent(context, item, isReadOnly = true)
            context.startActivity(intent)
        }
    }

    // Общее количество элементов
    override fun getItemCount() = items.size

    fun getTaskAt(position: Int): Task = items[position]

    fun setItems(newItems: List<Task>) {
        val sortedItems = newItems.sortedBy {it.deadlineDate }
        val diffCallback = TaskDiffCallback(items, sortedItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(sortedItems)
        diffResult.dispatchUpdatesTo(this)
    }
}