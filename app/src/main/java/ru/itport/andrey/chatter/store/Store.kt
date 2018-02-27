/**
 * Created by Andrey Germanov on 2/27/18.
 */
package ru.itport.andrey.chatter.store

import org.json.simple.JSONObject
import redux.api.Store
import redux.createStore
import ru.itport.andrey.chatter.reducers.rootReducer


/**
 * Modes of LoginForm activity. Can be either in register mode, or in login mode
 */
enum class LoginFormMode {
    LOGIN,
    REGISTER
}

/**
 * Structure which represents whole state of application, used by Redux-Kotlin to maintain
 * consistend UI
 */
var appState = JSONObject(mapOf(
        "current_activity" to "LoginForm",
        "LoginForm" to JSONObject(mapOf(
                "mode" to LoginFormMode.LOGIN,
                "login" to "",
                "email" to "",
                "password" to "",
                "confirm_password" to ""
        )
    )
))

/**
 * Redux store for Application state
 */
val appStore = createStore(rootReducer,appState)