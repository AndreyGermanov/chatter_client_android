package ru.itport.andrey.chatter.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

/**
 * Created by andrey on 2/10/18.
 */
fun showAlertDialog(title:String, message: String, context:Context) {
    var dialog = AlertDialog.Builder(context)
    dialog.setTitle(title).
    setMessage(message).
    setNeutralButton("OK", fun (iface:DialogInterface,textId:Int) {}).
    create().show()
}