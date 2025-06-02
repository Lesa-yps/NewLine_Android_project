package com.example.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.models.Task

class TaskDiffCallback(
    private val oldList: List<Task>,
    private val newList: List<Task>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    // сравнение по уникальному идентификатору
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    // сравнение по содержимому
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}