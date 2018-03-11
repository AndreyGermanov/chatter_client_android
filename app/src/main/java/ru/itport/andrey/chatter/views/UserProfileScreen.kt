/**
 * Created by Andrey Germanov on 3/10/18.
 */
package ru.itport.andrey.chatter.views

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.view.Gravity
import android.widget.DatePicker
import android.widget.LinearLayout
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.UserProfileActions
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.createTempImage
import ru.itport.andrey.chatter.utils.hideSoftKeyboard
import ru.itport.andrey.chatter.utils.showDatePickerDialog
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL
import trikita.anvil.DSL.*
import trikita.anvil.RenderableView
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Class represents User Profile screen
 */
class UserProfileScreen : BaseScreen(), DatePickerDialog.OnDateSetListener {

    /**
     * Link to temporary profile image file, which captured from camera or from
     * gallery
     */
    lateinit var tmpImage: File

    /**
     * Class constructor
     */
    init {
        val currentState = appStore.getState() as JSONObject
        state = currentState["UserProfile"] as JSONObject
    }

    /**
     * Function updates user interface on application state update
     */
    inner class UpdateUI: Runnable {
        override fun run() {
            globalState = appStore.getState() as JSONObject
            if (state["show_date_picker_dialog"] != null) {
                if (state["show_date_picker_dialog"] as Boolean) {
                    showDatePickerDialog(Date(state["birthDate"].toString().toLong()*1000), this@UserProfileScreen)
                    appStore.dispatch(UserProfileActions.changeProperty("show_date_picker_dialog", false))
                }
            }
            Anvil.render()
        }
    }

    /**
     * Function, which returns layout to draw on screen with all items
     */
    override fun drawView(): RenderableView {
        val logger = Logger.getLogger("UserProfileScreen.drawView")
        subscription = appStore.subscribe {
            this.runOnUiThread(UpdateUI())
        }
        return object: RenderableView(this) {
            override fun view() {
                val errors = state["errors"] as JSONObject
                linearLayout {
                    onClick {
                        hideSoftKeyboard(this@UserProfileScreen)
                    }
                    size(MATCH,MATCH)
                    orientation(LinearLayout.VERTICAL)
                    tableLayout {
                        imageView {
                            width(200)
                            height(200)
                            gravity(Gravity.CENTER_HORIZONTAL)
                            if (state["profileImage"]!=null && state["profileImage"] is ByteArray) {
                                var imageData = state["profileImage"] as ByteArray
                                var bmp = BitmapFactory.decodeByteArray(imageData,0,imageData.size)
                                imageBitmap(Bitmap.createScaledBitmap(bmp,200,200,false))
                            } else {
                                try {
                                    var bmp = BitmapFactory.decodeResource(this@UserProfileScreen.resources,ru.itport.andrey.chatter.R.drawable.profile)
                                    imageBitmap(Bitmap.createScaledBitmap(bmp,200,200,false))
                                } catch (e:Exception) {
                                    logger.log(Level.SEVERE,"Could not load default user profile image: "+e.message)
                                }
                            }
                            onClick {
                                val dialog = AlertDialog.Builder(this@UserProfileScreen)
                                dialog.setMessage("Select image source")
                                dialog.setNeutralButton("Camera") { dialogInterface,i ->
                                    tmpImage = createTempImage(this@UserProfileScreen)
                                    getProfileImage(MediaStore.ACTION_IMAGE_CAPTURE,tmpImage)
                                }
                                dialog.setPositiveButton("Gallery") { dialogInterface,i ->
                                    tmpImage = createTempImage(this@UserProfileScreen)
                                    getProfileImage(Intent.ACTION_PICK)
                                }
                                dialog.setNegativeButton("Cancel") { dialogInterface, i ->

                                }
                                dialog.create().show()
                            }

                        }
                    }
                    tableLayout {
                        tableRow {
                            orientation(LinearLayout.HORIZONTAL)
                            textView {
                                text("First name")
                            }
                            editText {
                                text(state["first_name"].toString())
                                onTextChanged { text -> appStore.dispatch(UserProfileActions.changeProperty("first_name",text))}
                            }
                        }
                        if (errors["first_name"]!=null && errors["first_name"] is UserProfileActions.UserProfileErrors) {
                            val msg = (errors["first_name"] as UserProfileActions.UserProfileErrors).getMessage()
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text(msg)
                                    textColor(Color.RED)
                                }
                            }
                        }
                        tableRow {
                            orientation(LinearLayout.HORIZONTAL)
                            textView {
                                text("Last name")
                            }
                            editText {
                                text(state["last_name"].toString())
                                onTextChanged { text -> appStore.dispatch(UserProfileActions.changeProperty("last_name",text))}
                            }
                        }
                        if (errors["last_name"]!=null && errors["last_name"] is UserProfileActions.UserProfileErrors) {
                            val msg = (errors["last_name"] as UserProfileActions.UserProfileErrors).getMessage()
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text(msg)
                                    textColor(Color.RED)
                                }
                            }
                        }
                        tableRow {
                            orientation(LinearLayout.HORIZONTAL)
                            textView {
                                text("Gender")
                            }
                            radioGroup{
                                orientation(LinearLayout.HORIZONTAL)
                                radioButton {
                                    checked(state["gender"].toString() == "M")
                                    text("M")
                                    onClick {
                                        appStore.dispatch(UserProfileActions.changeProperty("gender","M"))
                                    }
                                }
                                radioButton {
                                    checked(state["gender"].toString() == "F")
                                    text("F")
                                    onClick {
                                        appStore.dispatch(UserProfileActions.changeProperty("gender","F"))
                                    }
                                }
                            }
                        }
                        if (errors["gender"]!=null && errors["gender"] is UserProfileActions.UserProfileErrors) {
                            val msg = (errors["gender"] as UserProfileActions.UserProfileErrors).getMessage()
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text(msg)
                                    textColor(Color.RED)
                                }
                            }
                        }
                        tableRow {
                            orientation(LinearLayout.HORIZONTAL)
                            textView {
                                text("Date of Birth")
                            }
                            textView {
                                var value = ""
                                textSize(42f)
                                textColor(Color.BLACK)
                                try {
                                    value = android.text.format.DateFormat.format("MM/dd/yyyy", Date(state["birthDate"].toString().toLong()*1000)).toString()
                                } catch (e:Exception) {
                                    logger.log(Level.INFO,"Could not format BirthDate")
                                }
                                text(value)
                                onClick {
                                    appStore.dispatch(UserProfileActions.changeProperty("show_date_picker_dialog",true))
                                }
                            }
                        }
                        if (errors["birthDate"]!=null && errors["birthDate"] is UserProfileActions.UserProfileErrors) {
                            val msg = (errors["birthDate"] as UserProfileActions.UserProfileErrors).getMessage()
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text(msg)
                                    textColor(Color.RED)
                                }
                            }
                        }
                    }
                    tableLayout {
                        size(MATCH, BaseDSL.WRAP)
                        button {
                            size(MATCH, BaseDSL.WRAP)
                            text("UPDATE")
                            onClick { v ->

                            }
                        }
                    }
                    tableLayout {
                        size(MATCH, BaseDSL.WRAP)
                        button {
                            size(MATCH, BaseDSL.WRAP)
                            text("CANCEL")
                            onClick { v ->

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Function used to get image from camera or gallery and store it to user profile as
     * profile image
     *
     * @param source Source of image. Can be: MediaStore.ACTION_IMAGE_CAPTURE for camera or
     * Intent.ACTION_PICK for gallery
     */
    fun getProfileImage(source:String,image: File? = null) {
        var takePictureIntent = Intent(source)
        if (source == MediaStore.ACTION_IMAGE_CAPTURE) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,FileProvider.getUriForFile(this,"ru.itport.andrey.chatter.fileprovider",image as File))
        } else if (source == Intent.ACTION_PICK) {
            takePictureIntent = Intent(source,android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        }
        startActivityForResult(takePictureIntent,1)
    }

    /**
     * Callback function which called when called activity returned result (for example Camera
     * or gallery activity
     *
     * @param requestCode code which used when called this activity
     * @param responseCode result code of operation
     * @param data Extra data related to result
     */
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent) {
        if (resultCode == RESULT_OK) {
            val targetW = 200
            val targetH = 200

            var mCurrentPhotoPath = tmpImage.absolutePath

            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            val scaleFactor = Math.min(photoW / targetW, photoH / targetH)
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor

            val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
            val buffer = ByteBuffer.allocate(bitmap.byteCount)
            var bytes = ByteArray(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(buffer)
            buffer.rewind()
            buffer.get(bytes)
            appStore.dispatch(UserProfileActions.changeProperty("profileImage",bytes))
        }
    }

    /**
     * Function runs when application destroys this screen
     */
    override fun onDestroy() {
        super.onDestroy()
        subscription.unsubscribe()
    }

    /**
     * Function called when user selects date from DatePicker dialog
     *
     * @param datePicker DatePickerDialog object, which fired this method
     * @param month Month selected
     * @param year Year selected
     * @param day Day selected
     */
    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        var cal = Calendar.getInstance()
        cal.set(year,month,day)
        appStore.dispatch(UserProfileActions.changeProperty("birthDate",cal.timeInMillis/1000))
    }

}
