package ru.itport.andrey.chatter.models

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.experimental.*
import org.json.JSONObject
import ru.itport.andrey.chatter.MainActivity
import ru.itport.andrey.chatter.core.WebSocketResponseHandler
import ru.itport.andrey.chatter.utils.showAlertDialog
import java.util.*
import kotlin.collections.HashMap

@SuppressLint("StaticFieldLeak")
/**
 * Created by andrey on 2/10/18.
 */
object User: WebSocketResponseHandler {

    var id = ""
    lateinit var login: String
    lateinit var password: String
    lateinit var email: String
    lateinit var context: Context
    var isLogin = false

    var mHandler: Handler

    init {
        mHandler = Handler() {
            var response = it.obj as JSONObject
            if (response.has("action")) {
                if (response.getString("action") == "login_user") {
                    if (response.getString("status") == "error") {
                        showAlertDialog("Alert", response.get("message").toString(), context)
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

    override fun handleResponse(response:JSONObject) {
        mHandler.obtainMessage(1,response).sendToTarget()
    }
}