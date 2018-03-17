/**
 * Created by Andrey Germanov on 2/27/18.
 */
package ru.itport.andrey.chatter.store

import org.json.simple.JSONArray
import org.json.simple.JSONObject
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
 * List of screens, on which application can be
 */
enum class AppScreens {
    LOGIN_FORM,
    USER_PROFILE,
    CHAT,
    SYSTEM_SETTINGS
}

/**
 * Structure which represents whole state of application, used by Redux to maintain
 * consistent UI
 */
var appState = JSONObject(mapOf(

    // Current screen of application
    "current_activity" to AppScreens.LOGIN_FORM,

    // Data of LOGIN_FORM screen
    "LoginForm" to JSONObject(mapOf(
        "mode" to LoginFormMode.LOGIN,
        "login" to "",
        "email" to "",
        "password" to "",
        "confirm_password" to "",
        "show_progress_indicator" to false,
        "popup_message" to "",
        "errors" to JSONObject()

    )),

    // Data of USER_PROFILE screen
    "UserProfile" to JSONObject(mapOf(
        "errors" to JSONObject(),
        "login" to "",
        "email" to "",
        "password" to "",
        "confirm_password" to "",
        "first_name" to "",
        "last_name" to "",
        "gender" to "",
        "birthDate" to 0,
        "default_room" to "",
        "profileImage" to null,
        "rooms" to JSONArray(),
        "show_progress_indicator" to false,
        "popup_message" to "",
        "show_date_picker_dialog" to false
    )),

    // Data of loaded user
    "User" to JSONObject(mapOf(
            "user_id" to "",
            "session_id" to "",
            "isLogin" to false,
            "login" to "",
            "email" to "",

            "first_name" to "",
            "last_name" to "",
            "gender" to "",
            "birthDate" to System.currentTimeMillis()/1000,
            "default_room" to "",
            "profileImage" to null
    ))
))

/**
 * Redux store for Application state
 */
val appStore = createStore(rootReducer,appState)

fun getStateOf(section:String): JSONObject? {
    val state = appStore.state
    if (state.containsKey(section)) {
        return state[section] as JSONObject
    } else {
        return null
    }
}