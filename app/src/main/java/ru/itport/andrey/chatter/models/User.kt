package ru.itport.andrey.chatter.models

import android.content.Context
import ru.itport.andrey.chatter.utils.showAlertDialog

/**
 * Created by andrey on 2/10/18.
 */
object User {

    lateinit var login: String
    lateinit var password: String
    lateinit var email: String

    fun validateRegister(params: Map<String,String>,context:Context): Boolean {
        var errors = false
        println(params)
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
        return errors
    }

    fun register(params: Map<String,String>,context:Context) {
        if (validateRegister(params,context)) {

        }
    }
}