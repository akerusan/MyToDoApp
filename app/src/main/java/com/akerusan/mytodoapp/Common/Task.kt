package com.akerusan.mytodoapp.Common

class Task(private var text: String, private var isTaskDone: Boolean) {

    /**
     * タスクの文字列の返却
     */
    fun getText() : String{
        return text
    }

    /**
     * タスクの文字列の返却
     */
    fun setText(editedText: String){
        text = editedText
    }

    /**
     * チェックノックスの状態確認用
     */
    fun isCheckboxChecked(): Boolean {
        return isTaskDone
    }

    /**
     * チェックノックスの設定
     */
    fun setCheckboxChecked(isCheckboxChecked: Boolean) {
        isTaskDone = isCheckboxChecked
    }
}