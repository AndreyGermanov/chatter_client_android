package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.isValidEmail
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by andrey on 2/27/18.
 */

/**
 * Base interface, which all enum classes should implement
 */
interface SmartEnum {
    /**
     * Function which returns text message for specified enum member
     */
    fun getMessage():String
}

/**
 * Class groups all actions, which user can implement on LoginScreen.
 * Action functions of this class used to dispatch events to store via Redux
 */
class LoginScreenActions {

    /**
     * Possible action identifiers
     */
    enum class LoginScreenActionTypes {
        SWITCH_MODE,
        DO_LOGIN,
        DO_REGISTER,
        CHANGE_TEXT_FIELD
    }

    /**
     * User registration error definitions
     */
    enum class LoginScreenRegisterErrors(val value:String): SmartEnum {
        RESULT_OK("RESULT_OK"),
        RESULT_ERROR_FIELD_IS_EMPTY("RESULT_ERROR_FIELD_IS_EMPTY"),
        RESULT_ERROR_INCORRECT_EMAIL("RESULT_ERROR_INCORRECT_EMAIL"),
        RESULT_ERROR_EMAIL_EXISTS("RESULT_ERROR_EMAIL_EXISTS"),
        RESULT_ERROR_LOGIN_EXISTS("RESULT_ERROR_LOGIN_EXISTS"),
        RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT("RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT"),
        RESULT_ERROR_CONNECTION_ERROR("RESULT_ERROR_CONNECTION_ERROR"),
        RESULT_ERROR_ACTIVATION_EMAIL("RESULT_ERROR_ACTIVATION_EMAIL"),
        RESULT_ERROR_UNKNOWN("RESULT_ERROR_UNKNOWN");
        override fun getMessage():String {
            var result = ""
            when (this) {
                RESULT_OK -> result = "You are registered. Activation email sent. Please, open it and activate your account."
                RESULT_ERROR_FIELD_IS_EMPTY -> result = "Value of this field is required."
                RESULT_ERROR_INCORRECT_EMAIL -> result = "Incorrect email format."
                RESULT_ERROR_EMAIL_EXISTS -> result = "User with provided email already exists."
                RESULT_ERROR_LOGIN_EXISTS -> result = "User with provided login already exists."
                RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT -> result = "Passwords must be the same."
                RESULT_ERROR_CONNECTION_ERROR -> result = "Server connection error."
                RESULT_ERROR_ACTIVATION_EMAIL -> result = "Failed to send activation email. Please contact support."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please contact support"
            }
            return result
        }
    }

    /**
     * User login error definitions
     */
    enum class LoginScreenLoginErrors(val value:String):SmartEnum {
        RESULT_ERROR_FIELD_IS_EMPTY("RESULT_ERROR_FIELD_IS_EMPTY"),
        RESULT_ERROR_NO_USER("RESULT_ERROR_NO_USER"),
        RESULT_ERROR_USER_ALREADY_ACTIVATED("RESULT_ERROR_USER_ALREADY_ACTIVATED"),
        RESULT_ERROR_UNKNOWN("RESULT_ERROR_UNKNOWN");
        override fun getMessage(): String {
            var result = ""
            when (this) {
                RESULT_ERROR_FIELD_IS_EMPTY -> result = "Value of this field is required."
                RESULT_ERROR_NO_USER -> result = "User account not found. Please, try to register again or contact support."
                RESULT_ERROR_USER_ALREADY_ACTIVATED -> result = "User account already activated. You can login now."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please, contact support."
            }
            return result
        }
    }

    companion object: MessageCenterResponseReceiver {
        /**
         * Link to MessageCenter, to send commands to server
         */
        lateinit var messageCenter: MessageCenter

        /**
         * Action switches mode of LoginWindow from "Login" to "Register" and
         * from "Register" to "Login", when user presses appropriate button on
         * top of screen
         */
        fun switchMode(mode:LoginFormMode): JSONObject {
            return JSONObject(mapOf(
                    "type" to LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE,
                    "mode" to mode)
            )
        }

        /**
         * Action updates appropriate param of state with provided value
         *
         * @param property_name Name of property to set
         * @param property_value Value of property to set
         * @return Action structure as a JSONObject for reducer
         */
        fun changeProperty(property_name:String,property_value:Any): JSONObject {
            return JSONObject(mapOf(
                    "type" to LoginScreenActions.LoginScreenActionTypes.CHANGE_TEXT_FIELD,
                    "property_name" to property_name,
                    "property_value" to property_value
                 )
            )
        }

        /**
         * Action fires when user presses "Register" button on login screen
         *
         * @param params Register form params. Must have following: "login", "password",
         * "confirm_password","email". This is optional param. If it does not provided,
         * it will get values from LoginScreen state fields
         *
         */
        fun register(params:JSONObject? = null) {
            var form = JSONObject()
            if (params == null) {
                val state = appStore.state["LoginForm"] as JSONObject
                if (state["login"]!=null) {
                    form["login"] = state["login"].toString()
                }
                if (state["password"]!=null) {
                    form["password"] = state["password"].toString()
                }
                if (state["confirm_password"]!=null) {
                    form["confirm_password"] = state["confirm_password"].toString()
                }
                if (state["email"]!=null) {
                    form["email"] = state["email"].toString()
                }
            } else {
                form = params
            }

            val errors = JSONObject()
            appStore.dispatch(changeProperty("errors",errors))
            if (form["login"]==null || form["login"].toString().isEmpty()) {
                errors["login"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            }
            if (form["password"]==null || form["password"].toString().isEmpty()) {
                errors["password"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            } else if (form["password"].toString() != form["confirm_password"].toString()) {
                errors["password"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT
            }
            if (form["email"]==null || form["email"].toString().isEmpty()) {
                errors["email"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            } else if (!isValidEmail(form["email"].toString())) {
                errors["email"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_INCORRECT_EMAIL
            } else if (!messageCenter.connected) {
                errors["general"]  = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONNECTION_ERROR
            }
            if (errors.count()>0) {
                appStore.dispatch(changeProperty("errors",errors))
            } else {
                val state = appStore.state["LoginForm"] as JSONObject
                val alreadyRunning = state["show_progress_indicator"] as Boolean
                if (!alreadyRunning) {
                    appStore.dispatch(changeProperty("show_progress_indicator", true))
                    val request = HashMap<String, Any>()
                    val request_id = UUID.randomUUID().toString()
                    request.set("request_id", request_id)
                    request.set("action", "register_user")
                    request.set("sender", this)
                    request.set("login", form["login"].toString())
                    request.set("email", form["email"].toString())
                    request.set("password", form["password"].toString())
                    messageCenter.addRequest(request)
                }
            }
        }

        /**
         * This function receives responses to requests, sent to message center
         */
        override fun handleResponse(request_id: String, response: Any) {
            if (response is JSONObject) {
                val pendingResponses = this.messageCenter.getPendingResponsesQueue()
                if (pendingResponses.containsKey(request_id)) {
                    val pendingResponse = pendingResponses.get(request_id) as HashMap<String, Any>
                    if (pendingResponse.containsKey("request")) {
                        val request = pendingResponse.get("request") as HashMap<String, Any>
                        if (request.containsKey("action")) {
                            val action = request.get("action") as String
                            when (action) {
                                "register_user" -> {
                                    if ("ok" == response["status"] as String) {
                                        appStore.dispatch(LoginScreenActions.changeProperty("show_progress_indicator",false))
                                        appStore.dispatch(LoginScreenActions.changeProperty("popup_message",LoginScreenRegisterErrors.RESULT_OK.getMessage()))
                                        appStore.dispatch(LoginScreenActions.changeProperty("mode", LoginFormMode.LOGIN))
                                    } else {
                                        appStore.dispatch(LoginScreenActions.changeProperty("show_progress_indicator",false))
                                        val errors = JSONObject()
                                        try {
                                            errors["general"] = LoginScreenRegisterErrors.valueOf(response["status_code"].toString())
                                        } catch (e:Exception) {
                                            errors["general"] = LoginScreenRegisterErrors.RESULT_ERROR_UNKNOWN
                                        }
                                        appStore.dispatch(LoginScreenActions.changeProperty("errors",errors))
                                        appStore.dispatch(LoginScreenActions.changeProperty("mode", LoginFormMode.REGISTER))
                                    }
                                }
                            }
                            messageCenter.removePendingResponse(request_id)
                        }
                    }
                }
            }
        }
    }
}