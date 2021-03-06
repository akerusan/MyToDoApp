package com.akerusan.mytodoapp

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.akerusan.mytodoapp.Adapter.ListAdapter
import com.akerusan.mytodoapp.Common.Task
import com.akerusan.mytodoapp.Adapter.ListAdapter.OnTaskItemClickListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_task_dialog.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.edit_task_dialog.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var listView: ListView? = null
    private var todoListItems = ArrayList<Task>()
    private var mListAdapter : ListAdapter? = null
    private var mContext: Context? = null
    private var showActiveMode = false
    private var showCompletedMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this@MainActivity
        listView = list_view

        // 既存データがあればロードする
        loadData()

        // 各リスナーの設定
        set_all_check_all_off.setOnClickListener(this)
        set_all_check_all_on.setOnClickListener(this)
        show_all.setOnClickListener(this)
        show_active.setOnClickListener(this)
        show_completed.setOnClickListener(this)
        clear_completed_task.setOnClickListener(this)

        add_btn.setOnClickListener {
            addTaskDialog()
        }

        // listView内の変更の検知
        listView!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            var checkedItems = 0

            // 残りのタスクの表示
            for (item in todoListItems){
                if (item.isCheckboxChecked()) {
                    checkedItems++
                }
            }
            editLeftItems(todoListItems.size - checkedItems)

            // 完了のタスクの存在次第、クリアボタン表示
            if (checkedItems != 0){
                clear_completed_task.visibility = View.VISIBLE
            } else {
                clear_completed_task.visibility = View.INVISIBLE
            }

            // 全タスクのチェック状態により、チェックオールボタンON/OFF
            if (checkedItems == todoListItems.size && checkedItems != 0){
                set_all_check_all_on.visibility = View.GONE
                set_all_check_all_off.visibility = View.VISIBLE
            } else {
                set_all_check_all_on.visibility = View.VISIBLE
                set_all_check_all_off.visibility = View.GONE
            }

            // リスト表示設定パネルの表示が必要かどうかの確認
            if (todoListItems.size == 0 && mode_selector_panel.visibility == View.VISIBLE){
                // パネルの見た目初期化
                showActiveMode = false
                showCompletedMode = false
                changeModeButtonAppearance()
                // パネル非表示
                mode_selector_panel.visibility = View.GONE
                val animate = TranslateAnimation(0f, 0f, 0f, -mode_selector_panel.height.toFloat())
                animate.duration = 500
                mode_selector_panel.startAnimation(animate)
            } else if (todoListItems.size > 0 && mode_selector_panel.visibility == View.GONE){
                // パネル非表示
                mode_selector_panel.visibility = View.VISIBLE
                val animate = TranslateAnimation(0f, 0f, -mode_selector_panel.height.toFloat(), 0f)
                animate.duration = 500
                mode_selector_panel.startAnimation(animate)
            }
        }
    }

    override fun onClick(v: View?) {
        if (todoListItems.size > 0){
            when (v) {
                set_all_check_all_on -> {
                    setAllCheckOn()
                }
                set_all_check_all_off -> {
                    setAllCheckOff()
                }
                show_all -> {
                    showActiveMode = false
                    showCompletedMode = false
                    changeModeButtonAppearance()
                    mListAdapter!!.setItemsList(todoListItems)
                }
                show_active -> {
                    showActiveMode = true
                    showCompletedMode = false
                    changeModeButtonAppearance()
                    populateTaskListOnActiveMode()
                }
                show_completed -> {
                    showActiveMode = false
                    showCompletedMode = true
                    changeModeButtonAppearance()
                    populateTaskListOnCompletedMode()
                }
                clear_completed_task -> {
                    val notCompletedItemsList = ArrayList<Task>()
                    for (item in todoListItems){
                        if (!item.isCheckboxChecked()){
                            notCompletedItemsList.add(item)
                        }
                    }
                    todoListItems = notCompletedItemsList
                    populateTaskList(todoListItems)
                }
            }
        }
    }

    /**
     * activity削除前、データリスト保持
     */
    override fun onStop() {
        super.onStop()
        saveData()
    }

    /**
     * 各タスクに対するクリックリスナー
     */
    private val listListener: OnTaskItemClickListener = object : OnTaskItemClickListener {
        override fun onCheckboxClicked(position: Int, item: Task?) {
            item!!.setCheckboxChecked(!item.isCheckboxChecked())
            mListAdapter!!.notifyDataSetChanged()
            populateTaskList(todoListItems)
        }

        override fun onDeleteClicked(position: Int, item: Task?) {
            todoListItems.remove(item)
            populateTaskList(todoListItems)
        }

        override fun onEditClicked(position: Int, item: Task?) {
            editTaskDialog(position)
        }
    }

    /**
     * タスク追加のダイアログボックス
     */
    private fun addTaskDialog(){
        val dialog = Dialog(mContext!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.add_task_dialog)
        // ダイアログのキャンセルボタンでのみhy表示可能のため
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // キャンセルボタン押下時
        dialog.dialog_cancel.setOnClickListener {
            dialog.dismiss()
        }

        // 追加ボタン押下時
        dialog.dialog_add.setOnClickListener{
            val addText = dialog.findViewById(R.id.add_task_text) as EditText
            val text = addText.text.toString()
            // 空文字のタスクを追加させないため
            if (text.isNotEmpty()){
                val task = Task(text, false)
                todoListItems.add(task)
                populateTaskList(todoListItems)
                dialog.dismiss()
            }
        }
    }

    /**
     * 選択されたタスクの文字編集ためのダイアログボックス
     */
    private fun editTaskDialog(position: Int){
        val dialog = Dialog(mContext!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.edit_task_dialog)
        // ダイアログのキャンセルボタンでのみhy表示可能のため
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // キャンセルボタン押下時
        dialog.dialog_cancel_edit.setOnClickListener {
            dialog.dismiss()
        }

        // 編集ボタン押下時
        dialog.dialog_edit.setOnClickListener{
            val editedText = dialog.findViewById(R.id.edit_task_text) as EditText
            val text = editedText.text.toString()
            // 空文字のタスクを追加させないため
            if (text.isNotEmpty()){
                todoListItems[position].setText(text)
                populateTaskList(todoListItems)
                dialog.dismiss()
            }
        }
    }

    /**
     * ListViewにタスクリスト入れ込み
     */
    private fun populateTaskList(list: ArrayList<Task>?) {
        if (list != null) {
            if (mListAdapter == null) {
                mListAdapter = ListAdapter(
                    mContext!!,
                    todoListItems
                )
                mListAdapter!!.setOnTaskClickListener(listListener)
                listView!!.adapter = mListAdapter
            } else {
                when {
                    showActiveMode -> {
                        populateTaskListOnActiveMode()
                    }
                    showCompletedMode -> {
                        populateTaskListOnCompletedMode()
                    }
                    else -> {
                        mListAdapter!!.setItemsList(todoListItems)
                    }
                }
            }
        }
    }

    /**
     * 完了タスクのみ表示時のリスト送信
     */
    private fun populateTaskListOnCompletedMode(){
        val completedItemsList = ArrayList<Task>()
        for (item in todoListItems){
            if (item.isCheckboxChecked()){
                completedItemsList.add(item)
            }
        }
        mListAdapter!!.setItemsList(completedItemsList)
    }

    /**
     * 未完了タスクのみ表示時のリスト送信
     */
    private fun populateTaskListOnActiveMode(){
        val activeItemsList = ArrayList<Task>()
        for (item in todoListItems){
            if (!item.isCheckboxChecked()){
                activeItemsList.add(item)
            }
        }
        mListAdapter!!.setItemsList(activeItemsList)
    }

    /**
     * 全タスクを完了にする
     */
    private fun setAllCheckOn(){
        for (item in todoListItems){
            item.setCheckboxChecked(true)
        }
        populateTaskList(todoListItems)
    }

    /**
     * 全タスクを未完了にする
     */
    private fun setAllCheckOff(){
        for (item in todoListItems){
            item.setCheckboxChecked(false)
        }
        populateTaskList(todoListItems)
    }

    /**
     * 未完了のタスク数を表示する
     */
    private fun editLeftItems(leftItems: Int){
        if (todoListItems.size != 0){

            if (leftItems >= 2){
                left_items_string.setText(R.string.multipleItems)
            } else {
                left_items_string.setText(R.string.zeroOrOneItem)
            }
            left_items_number.text = leftItems.toString()
            left_items.visibility = View.VISIBLE
        } else {
            left_items.visibility = View.INVISIBLE
        }
    }

    /**
     * リスト表示設定パネルのボタン見た目を変更する
     */
    private fun changeModeButtonAppearance(){
        when {
            showActiveMode -> {
                show_active.setBackgroundResource(R.drawable.mode_selector_button_background)
                show_active.setTypeface(null, Typeface.BOLD)

                show_completed.setBackgroundResource(0)
                show_completed.setTypeface(null, Typeface.NORMAL)

                show_all.setBackgroundResource(0)
                show_all.setTypeface(null, Typeface.NORMAL)
            }
            showCompletedMode -> {
                show_completed.setBackgroundResource(R.drawable.mode_selector_button_background)
                show_completed.setTypeface(null, Typeface.BOLD)

                show_active.setBackgroundResource(0)
                show_active.setTypeface(null, Typeface.NORMAL)

                show_all.setBackgroundResource(0)
                show_all.setTypeface(null, Typeface.NORMAL)
            }
            else -> {
                show_all.setBackgroundResource(R.drawable.mode_selector_button_background)
                show_all.setTypeface(null, Typeface.BOLD)

                show_completed.setBackgroundResource(0)
                show_completed.setTypeface(null, Typeface.NORMAL)

                show_active.setBackgroundResource(0)
                show_active.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    // データの保持
    private fun saveData(){
        val data: SharedPreferences = getSharedPreferences("list", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = data.edit()
        val gsonList = Gson()
        val jsonList: String = gsonList.toJson(todoListItems)
        editor.putString("taskList", jsonList)
        editor.apply()
    }

    // 保持データの存在確認とロード
    private fun loadData(){
        val sharedPreferences = getSharedPreferences("list", Context.MODE_PRIVATE)
        val gsonList = Gson()
        val jsonList = sharedPreferences.getString("taskList", null)
        val type = object : TypeToken<ArrayList<Task?>?>(){}.type

        if (jsonList != null){
            todoListItems = gsonList.fromJson(jsonList, type)
            populateTaskList(todoListItems)
        }
    }
}
