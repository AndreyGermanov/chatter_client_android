/**
* Created by Andrey Germanov on 2/10/18.
*/

package ru.itport.andrey.chatter.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.provider.MediaStore
import org.json.simple.JSONObject


/**
 * Function used to show alert dialog with "OK" button
 *
 * @param title Title of dialog
 * @param message Message inside dialog
 * @param context Link to activity, which displays dialog
 */
fun showAlertDialog(title:String, message: String, context:Context) {
    var dialog = AlertDialog.Builder(context)
    dialog.setTitle(title).
    setMessage(message).
    setNeutralButton("OK", fun (iface:DialogInterface,textId:Int) {}).
    create().show()
}

/**
 * Function used to generate full path on disk from provided URI
 *
 * @param Uri URI of file
 * @param context Link to activity which used to call function
 * @return String with full path of file
 */
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

/**
 * Function used to stringify some enum values in source JSON object and return JSON string with values either
 *
 * @param response JSONObject to convert
 * @return JSON String
 */
fun toJSONString(response: JSONObject):String {
    for ((index,i) in response) {
        if (i !is String && i !is Int && i !is Long && i !is Boolean) {
            response.set(index,i.toString())
        }
    }
    val result = response.toJSONString()
    return result
}