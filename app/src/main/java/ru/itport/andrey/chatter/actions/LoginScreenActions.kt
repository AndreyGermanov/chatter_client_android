package ru.itport.andrey.chatter.actions

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver
import ru.itport.andrey.chatter.store.AppScreens
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.isValidEmail
import java.util.*
import java.util.zip.Adler32
import kotlin.collections.HashMap

/**
 * Created by andrey on 2/27/18.
 */

/**
 * Action creator for Login screen actions. Contains all actions, which user can run
 * from login screen, which then send to reducer to change "LoginScreen" part of application
 * state via Redux
 */
class LoginScreenActions: Actions() {

    /**
     * Possible action identifiers
     */
    enum class LoginScreenActionTypes {
        SWITCH_MODE,
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
        RESULT_OK("RESULT_OK"),
        RESULT_ERROR_NOT_ACTIVATED("RESULT_ERROR_NOT_ACTIVATED"),
        RESULT_ERROR_INCORRECT_LOGIN("RESULT_ERROR_INCORRECT_LOGIN"),
        RESULT_ERROR_INCORRECT_PASSWORD("RESULT_ERROR_INCORRECT_PASSWORD"),
        RESULT_ERROR_CONNECTION_ERROR("RESULT_ERROR_CONNECTION_ERROR"),
        RESULT_ERROR_ALREADY_LOGIN("RESULT_ERROR_ALREADY_LOGIN"),
        RESULT_ERROR_UNKNOWN("RESULT_ERROR_UNKNOWN");
        override fun getMessage(): String {
            var result = ""
            when (this) {
                RESULT_ERROR_NOT_ACTIVATED -> result = "Please, activate this account. Open activation email."
                RESULT_ERROR_INCORRECT_LOGIN -> result = "Incorrect login."
                RESULT_ERROR_INCORRECT_PASSWORD -> result = "Incorrect password."
                RESULT_ERROR_CONNECTION_ERROR -> result = "Server connection error."
                RESULT_ERROR_ALREADY_LOGIN -> result = "User already in the system."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please contact support."
            }
            return result;
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
         * Action fires when user presses "Login" button on login screen
         *
         * @param params Login form params. Must have following: "login" and "password".
         * If it does not provided, it will get values from LoginScreen state fields
         *
         */
        fun login(params:JSONObject?=null) {
            var form = JSONObject()
            if (params == null) {
                val state = appStore.state["LoginForm"] as JSONObject
                if (state["login"]!=null) {
                    form["login"] = state["login"].toString()
                }
                if (state["password"]!=null) {
                    form["password"] = state["password"].toString()
                }
            } else {
                form = params
            }

            val errors = JSONObject()
            appStore.dispatch(changeProperty("errors",errors))
            if (form["login"]==null || form["login"].toString().isEmpty()) {
                errors["login"] = LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_INCORRECT_LOGIN
            }
            if (form["password"]==null || form["password"].toString().isEmpty()) {
                errors["password"] = LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_INCORRECT_PASSWORD
            } else if (!messageCenter.connected) {
                errors["general"] = LoginScreenLoginErrors.RESULT_ERROR_CONNECTION_ERROR
            }
            if (errors.count()>0) {
                appStore.dispatch(LoginScreenActions.changeProperty("errors",errors))
            } else {
                val state = appStore.state["LoginForm"] as JSONObject
                val alreadyRunning = state["show_progress_indicator"] as Boolean
                if (!alreadyRunning) {
                    appStore.dispatch(changeProperty("show_progress_indicator", true))
                    val request = HashMap<String, Any>()
                    val request_id = UUID.randomUUID().toString()
                    request.set("request_id", request_id)
                    request.set("action", "login_user")
                    request.set("sender", this)
                    request.set("login", form["login"].toString())
                    request.set("password", form["password"].toString())
                    messageCenter.addRequest(request)
                }
            }
        }

        /**
         * This function receives responses to requests, sent to message center. Responses can
         * be either JSON formatted, or binary (files).
         *
         * @param request_id ID of request in a queue, for which response is received. If it binary
         * response, then request_id is checksum of received response
         * @param response - body of response, usually it is a JSONObject
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
                                "register_user" -> this.handleRegisterUserResponse(request,response)
                                "login_user" -> this.handleLoginUserResponse(request,response)
                            }
                            messageCenter.removePendingResponse(request_id)
                        }
                    }
                }
            } else if (response is ByteArray) {
                this.handleBinaryDataResponse(request_id.toLong(),response)
            }
        }

        /**
         * Function used to handle WebSocket server response on "register_user" aciton
         *
         * @param request - original request, response to which received
         * @param response - body of received response
         */
        fun handleRegisterUserResponse(request:HashMap<String,Any>,response:JSONObject) {
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

        /**
         * Function used to handle WebSocket server response on "login_user" aciton
         *
         * @param request - original request, response to which received
         * @param response - body of received response
         */
        fun handleLoginUserResponse(request:HashMap<String,Any>,response:JSONObject) {
            appStore.dispatch(changeProperty("show_progress_indicator", false))
            val errors = JSONObject()
            if ("ok" == response["status"] as String) {
                var rooms = response["rooms"]
                if (rooms==null || rooms !is JSONArray) {
                    if (rooms is String) {
                        var parser = JSONParser()
                        try {
                            response["rooms"] = parser.parse(rooms) as JSONArray
                        } catch(e:Exception) {
                            errors["general"] = LoginScreenLoginErrors.RESULT_ERROR_UNKNOWN
                        }
                        if (response["rooms"] is JSONArray) {
                            if ((response["rooms"] as JSONArray).count()<=0) {
                                errors["general"] = LoginScreenLoginErrors.RESULT_ERROR_UNKNOWN
                            }
                        }
                    }
                } else if ((rooms).count()<=0) {
                    errors["general"] = LoginScreenLoginErrors.RESULT_ERROR_UNKNOWN
                }
                if (errors.count()>0) {
                    appStore.dispatch(LoginScreenActions.changeProperty("errors",errors))
                    appStore.dispatch(LoginScreenActions.changeProperty("mode", LoginFormMode.LOGIN))
                } else {
                    appStore.dispatch(UserActions.changeProperty("isLogin", true))
                    appStore.dispatch(UserActions.changeProperty("user_id", response["user_id"].toString()))
                    appStore.dispatch(UserActions.changeProperty("session_id", response["session_id"].toString()))
                    if (response["first_name"] != null) {
                        appStore.dispatch(UserActions.changeProperty("first_name", response["first_name"].toString()))
                        appStore.dispatch(UserProfileActions.changeProperty("first_name", response["first_name"].toString()))
                    } else {
                        appStore.dispatch(UserActions.changeProperty("first_name", ""))
                        appStore.dispatch(UserProfileActions.changeProperty("first_name", ""))
                    }
                    if (response["last_name"] != null) {
                        appStore.dispatch(UserActions.changeProperty("last_name", response["last_name"].toString()))
                        appStore.dispatch(UserProfileActions.changeProperty("last_name", response["last_name"].toString()))
                    } else {
                        appStore.dispatch(UserActions.changeProperty("last_name", ""))
                        appStore.dispatch(UserProfileActions.changeProperty("last_name", ""))
                    }
                    if (response["gender"] != null) {
                        appStore.dispatch(UserActions.changeProperty("gender", response["gender"].toString()))
                        appStore.dispatch(UserProfileActions.changeProperty("gender", response["gender"].toString()))
                    } else {
                        appStore.dispatch(UserActions.changeProperty("gender", "M"))
                        appStore.dispatch(UserProfileActions.changeProperty("gender", "M"))
                    }
                    if (response["birthDate"] != null) {
                        appStore.dispatch(UserActions.changeProperty("birthDate", response["birthDate"].toString().toInt()))
                        appStore.dispatch(UserProfileActions.changeProperty("birthDate", response["birthDate"].toString().toInt()))
                    } else {
                        appStore.dispatch(UserActions.changeProperty("birthDate", (System.currentTimeMillis() / 1000).toInt()))
                        appStore.dispatch(UserProfileActions.changeProperty("birthDate", (System.currentTimeMillis() / 1000).toInt()))
                    }
                    if (response["default_room"] != null) {
                        appStore.dispatch(UserActions.changeProperty("default_room", response["default_room"].toString()))
                        appStore.dispatch(UserProfileActions.changeProperty("default_room", response["default_room"].toString()))
                        appStore.dispatch(Actions.changeActivity(AppScreens.CHAT))
                    } else {
                        appStore.dispatch(Actions.changeActivity(AppScreens.USER_PROFILE))
                    }
                    if (response["rooms"]!=null) {
                        appStore.dispatch(UserProfileActions.changeProperty("rooms", response["rooms"] as JSONArray))
                    }
                    if (response["checksum"]!= null) {
                        messageCenter.addPendingFile(response["checksum"].toString().toLong(),request)
                    }
                }
            } else {
                try {
                    errors["general"] = LoginScreenLoginErrors.valueOf(response["status_code"].toString())
                } catch (e:Exception) {
                    errors["general"] = LoginScreenLoginErrors.RESULT_ERROR_UNKNOWN
                }
                appStore.dispatch(LoginScreenActions.changeProperty("errors",errors))
                appStore.dispatch(LoginScreenActions.changeProperty("mode", LoginFormMode.LOGIN))
            }
        }

        /**
         * Function used to handle binary data responses, coming from WebSocket server
         * which is associated with request, originated by current object
         *
         * @param checksum of received binary file
         * @param data Binary data file
         */
        fun handleBinaryDataResponse(checksum:Long,data:ByteArray) {
            val pending_files = messageCenter.getPendingFilesQueue()
            val checkSumEngine = Adler32()
            checkSumEngine.update(data)
            if (pending_files.containsKey(checksum) && checksum == checkSumEngine.value) {
                val pending_file = pending_files[checksum] as HashMap<String,Any>
                val request = pending_file["request"] as HashMap<String,Any>
                if (request.containsKey("action")) {
                    when (request["action"].toString()) {
                        "login_user" -> {
                            appStore.dispatch(UserActions.changeProperty("profileImage",data))
                            appStore.dispatch(UserProfileActions.changeProperty("profileImage",data))
                        }
                    }
                }
                messageCenter.removePendingFile(checksum)
            }
        }
    }
}