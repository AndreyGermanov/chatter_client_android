package ru.itport.andrey.chatter.models

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.experimental.*
import org.json.JSONObject
import ru.itport.andrey.chatter.MainActivity
import ru.itport.andrey.chatter.ProfileSettings
import ru.itport.andrey.chatter.R
import ru.itport.andrey.chatter.core.WebSocketResponseHandler
import ru.itport.andrey.chatter.utils.showAlertDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

import java.io.ByteArrayOutputStream


@SuppressLint("StaticFieldLeak")
/**
 * Created by andrey on 2/10/18.
 */
object User: WebSocketResponseHandler {

    var id = ""
    lateinit var login: String
    lateinit var password: String
    lateinit var email: String
    var firstName = "Andrey"
    var lastName = ""
    var defaultRoom = ""
    var birthDate = (System.currentTimeMillis()/1000).toInt()
    var gender = ""
    var rooms: ArrayList<String> = ArrayList()
    lateinit var image: Bitmap
    var imageChanged = false


    lateinit var context: Context
    var isLogin = false

    var mHandler: Handler

    init {
        rooms.add("Room1")
        rooms.add("Room2")
        rooms.add("Room3")
        mHandler = Handler() {
            var response = it.obj as JSONObject
            if (response.has("action")) {
                if (response.getString("action") == "login_user") {
                    if (response.getString("status") == "error") {
                        showAlertDialog("Alert", response.get("message").toString(), context)
                    } else {
                        if (response.has("first_name")) {
                            this.firstName = response.getString("first_name")
                        }
                        if (response.has("last_name")) {
                            this.lastName = response.getString("last_name")
                        }
                        if (response.has("default_room")) {
                            this.defaultRoom = response.getString("default_room")
                        }
                        if (response.has("birthDate")) {
                            this.birthDate = response.getString("birthDate").toInt()
                        }
                        if (response.has("gender")) {
                            this.gender = response.getString("gender")
                        }
                        if (response.has("user_id")) {
                            this.id = response.getString("user_id")
                        }
                        //if (this.defaultRoom=="") {
                            val intent = Intent(context, ProfileSettings().javaClass)
                            val activity = context as Activity
                            activity.startActivity(intent)
                            activity.finish()
                        //}
                        this.image = BitmapFactory.decodeResource(context.resources, R.drawable.profile)
                    }
                } else if (response.getString("action") == "update_user_profile") {
                    if (response.getString("status") == "ok") {
                        this.imageChanged = false
                    } else {
                        showAlertDialog("Error",response.getString("message"),context)
                    }
                }
            } else {
                showAlertDialog("Alert", response.get("message").toString(), context)
            }
            true
        }
    }
    fun validateRegister(params: Map<String,String>,context:Context): Boolean {
        var errors = false
        if (params["login"]?.length==0) {
            showAlertDialog("Error", "Login is required", context)
            errors = true
        }
        if (!errors && params["password"]?.length==0) {
            showAlertDialog("Error", "Password is required", context)
            errors = true
        }
        if (!errors && params["password"] != params["password_again"]) {
            showAlertDialog("Error", "Passwords must match", context)
            errors = true
        }
        if (!errors && params["email"]?.length==0) {
            showAlertDialog("Error", "Email is required", context)
            errors = true
        }
        return !errors
    }

    fun register(params: Map<String,String>,context:Context) {
        if (validateRegister(params,context)) {
            val activity = context as MainActivity
            var register_packet: HashMap<String,Any> = params as HashMap<String, Any>
            register_packet.put("action","register_user")
            register_packet.put("request_id", UUID.randomUUID().toString())
            register_packet.put("sender",this)
            activity.mService.scheduleRequest(register_packet)
        }
    }

    fun login(params: Map<String,String>, context:Context) {
        val activity = context as MainActivity
        val login_packet: HashMap<String,Any> = params as HashMap<String,Any>
        login_packet.put("action","login_user")
        login_packet.put("request_id", UUID.randomUUID().toString())
        login_packet.put("sender",this)
        activity.mService.scheduleRequest(login_packet)
    }

    fun validateProfileChange(params: Map<String,String>,context:Context): Boolean {
        var errors = false
        if (params["first_name"]?.length==0) {
            showAlertDialog("Error", "First name is required", context)
            errors = true
        }
        if (!errors && params["last_name"]?.length==0) {
            showAlertDialog("Error", "Last name is required", context)
            errors = true
        }
        if (!errors && params["gender"]?.length==0) {
            showAlertDialog("Error", "Gender is required", context)
            errors = true
        }
        if (!errors && params["birthDate"]?.length==0) {
            showAlertDialog("Error", "Date of birth is requred", context)
            errors = true
        }
        if (!errors && params["default_room"]?.length==0) {
            showAlertDialog("Error", "Default room is required", context)
            errors = true
        }
        return !errors
    }

    fun updateProfile(params: Map<String,String>,bitmap:Bitmap?,context:Context) {
        if (validateProfileChange(params,context)) {
            val activity = context as ProfileSettings
            var register_packet: HashMap<String,Any> = params as HashMap<String, Any>
            register_packet.put("user_id",this.id)
            register_packet.put("action","update_user")
            register_packet.put("request_id", UUID.randomUUID().toString())
            register_packet.put("sender",this)
            activity.mService.scheduleRequest(register_packet)
            if (bitmap!=null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                activity.mService.ws.sendBinary(byteArray)
            }
        }
    }

    override fun handleResponse(response:JSONObject) {
        mHandler.obtainMessage(1,response).sendToTarget()
    }

    fun getRoomIndex(room:String) : Int {
        val result = rooms.indexOf(room)
        return result
    }
}