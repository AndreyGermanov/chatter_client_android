package ru.itport.andrey.chatter.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.provider.MediaStore



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

fun getRealPathFromURI(contentUri: Uri,context:Context): String {

    // can post image
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(contentUri,
            proj, // WHERE clause selection arguments (none)
            null, null, null)// Which columns to return
    // WHERE clause; which rows to return (all rows)
    // Order-by clause (ascending by name)
    val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
    cursor.moveToFirst()

    return cursor.getString(column_index)
}