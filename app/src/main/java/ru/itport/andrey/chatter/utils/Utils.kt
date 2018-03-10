/**
* Created by Andrey Germanov on 2/10/18.
*/

package ru.itport.andrey.chatter.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.provider.MediaStore
import android.widget.ProgressBar
import org.json.simple.JSONObject
import java.util.regex.Pattern
import android.app.Activity
import android.view.inputmethod.InputMethodManager


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
 * Function shows progress bar of operation in popup dialog
 *
 * @param context Link to activity, which displays dialog
 */
fun showProgressBar(context:Context):AlertDialog {
    var dialog = AlertDialog.Builder(context)
    var progressBar = ProgressBar(context)
    progressBar.isIndeterminate = true
    dialog.setView(progressBar)
    dialog.setTitle("Status")
    dialog.setMessage("Connecting ...")
    return dialog.create()
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
        if (i !is String && i !is Int && i !is Long && i !is Boolean && i !is JSONObject) {
            response.set(index,i.toString())
        } else if (i is JSONObject) {
            response.set(index, toJSONString(i))
        }
    }
    val result = response.toJSONString()
    return result
}

/**
 * Function validates email address
 *
 * @param email Email address to check
 * @return True if provided email address is correct and False otherwise
 */
fun isValidEmail(email: CharSequence): Boolean {
    var result = false
    try {
        val pattern = Pattern.compile("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
        val matches = pattern.matcher(email)
        result = (matches!=null && matches.matches())
    } catch (e:Exception) {
        result = false
    }
    return result
}

/**
 * Function used to hide onscreen keyboard
 *
 * @param activity Link to activity which wants to hide keyboard
 */
fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager = activity.getSystemService(
            Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager!!.hideSoftInputFromWindow(
            activity.currentFocus!!.windowToken, 0)
}