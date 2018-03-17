package ru.itport.andrey.chatter.actions

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.core.MessageCenter

/**
 * Created by Andrey Germanov on 3/9/18.
 */

import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver
import ru.itport.andrey.chatter.store.AppScreens
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.store.getStateOf
import java.util.*
import java.util.zip.Adler32


/**
 * Class contains all actions to change current user object in global application
 * state.
 */
class UserProfileActions: Actions() {
    /**
     * Possible action identifiers
     */
    enum class UserProfileActionTypes {
        CHANGE_PROPERTY
    }

    /**
     * User profile submit errors definition
     */
    enum class UserProfileErrors(val value:String): SmartEnum {
        RESULT_OK("RESULT_OK"),
        RESULT_OK_PENDING_IMAGE_UPLOAD("RESULT_OK_PENDING_IMAGE_UPLOAD"),
        RESULT_ERROR_IMAGE_UPLOAD("RESULT_ERROR_IMAGE_UPLOAD"),
        AUTHENTICATION_ERROR("AUTHENTICATION_ERROR"),
        INTERNAL_ERROR("INTERNAL_ERROR"),
        RESULT_ERROR_FIELD_IS_EMPTY("RESULT_ERROR_FIELD_IS_EMPTY"),
        RESULT_ERROR_INCORRECT_FIELD_VALUE("RESULT_ERROR_INCORRECT_FIELD_VALUE"),
        RESULT_ERROR_CONNECTION_ERROR("RESULT_ERROR_CONNECTION_ERROR"),
        RESULT_ERROR_PASSWORDS_SHOULD_MATCH("RESULT_ERROR_PASSWORDS_SHOULD_MATCH"),
        RESULT_ERROR_INCORRECT_USER_ID("RESULT_ERROR_INCORRECT_USER_ID"),
        RESULT_ERROR_INCORRECT_SESSION_ID("RESULT_ERROR_INCORRECT_SESSION_ID"),
        RESULT_ERROR_EMPTY_REQUEST("RESULT_ERROR_EMPTY_REQUEST"),
        RESULT_ERROR_UNKNOWN("RESULT_ERROR_UNKNOWN");
        override fun getMessage():String {
            return when(this) {
                RESULT_OK -> ""
                RESULT_OK_PENDING_IMAGE_UPLOAD -> ""
                RESULT_ERROR_IMAGE_UPLOAD -> "Error during profile image upload. Please try again."
                AUTHENTICATION_ERROR -> "Authentication error."
                INTERNAL_ERROR -> "System error. Please, contact support."
                RESULT_ERROR_FIELD_IS_EMPTY -> "This field is required."
                RESULT_ERROR_INCORRECT_FIELD_VALUE -> "Incorrect field value."
                RESULT_ERROR_CONNECTION_ERROR -> "Server connection error."
                RESULT_ERROR_PASSWORDS_SHOULD_MATCH -> "Passwords should match."
                RESULT_ERROR_INCORRECT_USER_ID -> "Incorrect user_id. Please, login and try again."
                RESULT_ERROR_INCORRECT_SESSION_ID -> "Incorrect user session. Please login and try again."
                RESULT_ERROR_EMPTY_REQUEST -> "Please, change data before submit."
                RESULT_ERROR_UNKNOWN -> "Unknown error. Please contact support."
            }
        }
    }

    companion object:MessageCenterResponseReceiver {

        /**
         * Link to MessageCenter, to send commands to server
         */
        override lateinit var messageCenter: MessageCenter

        /**
         * Action updates appropriate param of state with provided value
         *
         * @param property_name Name of property to set
         * @param property_value Value of property to set
         * @return Action structure as a JSONObject for reducer
         */
        fun changeProperty(property_name: String, property_value: Any?): JSONObject {
            return JSONObject(mapOf(
                    "type" to UserProfileActionTypes.CHANGE_PROPERTY,
                    "property_name" to property_name,
                    "property_value" to property_value
            ))
        }

        /**
         * Action used to update user profile with data from state
         *
         * @return List of properties and their values, which actually sent for update to server
         */
        fun update(params: JSONObject? = null):HashMap<String,Any> {

            val userState = getStateOf("User")!!
            val errors = JSONObject()
            val request = HashMap<String,Any>()

            appStore.dispatch(UserProfileActions.changeProperty("errors",errors))

            if (userState["user_id"].toString().isEmpty()) {
                errors["general"] = UserProfileErrors.RESULT_ERROR_INCORRECT_USER_ID
                appStore.dispatch(this.changeProperty("errors",errors))
                return request
            }

            if (userState["session_id"].toString().isEmpty()) {
                errors["general"] = UserProfileErrors.RESULT_ERROR_INCORRECT_SESSION_ID
                appStore.dispatch(this.changeProperty("errors",errors))
                return request
            }

            var form = params
            if (form==null) {
                form = getStateOf("UserProfile")!!
            }

            val nameValidatePattern = "[A-Za-z\\ ]+"

            fun processField(fieldName:String) {
                var form = form as JSONObject
                if (form.containsKey(fieldName)) {
                    var value = form[fieldName].toString().trim().toLowerCase()
                    value = value.capitalize()
                    if (value.isEmpty()) {
                        errors[fieldName] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                    } else if (!value.matches(Regex(nameValidatePattern))) {
                        errors[fieldName] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                    } else {
                        if (userState[fieldName].toString() != value) {
                            request[fieldName] = value
                        }
                    }
                }
            }

            processField("first_name")
            processField("last_name")

            if (form.containsKey("gender")) {
                if (form["gender"].toString().isEmpty()) {
                    errors["gender"] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                } else if (!listOf("M","F").contains(form["gender"].toString())) {
                    errors["gender"] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                } else if (form["gender"].toString() != userState["gender"].toString()) {
                    request["gender"] = form["gender"].toString()
                }
            }

            if (form.containsKey("birthDate")) {
                if (form["birthDate"].toString().isEmpty()) {
                    errors["birthDate"] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                } else if (form["birthDate"].toString().toIntOrNull() == null) {
                    errors["birthDate"] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                } else if (form["birthDate"].toString().toInt() == 0) {
                    errors["birthDate"] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                } else if (form["birthDate"].toString().toLong()>System.currentTimeMillis()/1000) {
                    errors["birthDate"] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                } else if (form["birthDate"].toString().toInt()!=userState["birthDate"].toString().toInt()) {
                    request["birthDate"] = form["birthDate"].toString().toInt()
                }
            }

            if (form.containsKey("default_room") && !form["default_room"].toString().isEmpty()) {
                val rooms = form["rooms"] as JSONArray
                val found_rooms = rooms.filter {
                    val form = form as JSONObject
                    val room = it as JSONObject
                    room["_id"].toString() == form["default_room"].toString()
                }
                if (found_rooms.size!=1) {
                    errors["default_room"] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                } else if (form["default_room"].toString() != userState["default_room"].toString()) {
                    request["default_room"] = form["default_room"].toString()
                }
            } else {
                if (userState["default_room"].toString().isEmpty()) {
                    errors["default_room"] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                }
            }

            if (form.containsKey("password")) {
                if (form["password"].toString().isEmpty()) {
                    errors["password"] = UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY
                } else if (!form.containsKey("confirm_password") || form["confirm_password"].toString().isEmpty()) {
                    errors["password"] = UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE
                } else if (form["password"]!=form["confirm_password"]) {
                    errors["password"] = UserProfileErrors.RESULT_ERROR_PASSWORDS_SHOULD_MATCH
                } else {
                    request["password"] = form["password"].toString()
                }
            }

            if (errors.size==0) {
                if (!this.messageCenter.connected) {
                    errors["general"] = UserProfileErrors.RESULT_ERROR_CONNECTION_ERROR
                    appStore.dispatch(UserProfileActions.changeProperty("errors",errors))
                    return request
                } else if (!(form["show_progress_indicator"] as Boolean)) {
                    if (request.size>0) {
                        request["request_id"] = UUID.randomUUID().toString()
                        request["sender"] = this
                        request["action"] = "update_user"
                        if (form.containsKey("profileImage") && form["profileImage"] is ByteArray) {
                            val checksumEngine = Adler32()
                            checksumEngine.update(form["profileImage"] as ByteArray)
                            val checksum_to_send = checksumEngine.value
                            var original_checksum:Long = 0
                            if (userState["profileImage"] is ByteArray) {
                                checksumEngine.reset()
                                checksumEngine.update(userState["profileImage"] as ByteArray)
                                original_checksum = checksumEngine.value
                            }
                            if (checksum_to_send != original_checksum) {
                                request["profile_image_checksum"] = checksum_to_send
                                request["profileImage"] = form["profileImage"] as ByteArray
                            }
                        }

                        this.messageCenter.addRequest(request)
                        appStore.dispatch(UserProfileActions.changeProperty("show_progress_indicator", true))
                    } else {
                        appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject(mapOf("general" to UserProfileErrors.RESULT_ERROR_EMPTY_REQUEST))))
                    }
                } else {
                    return request
                }
            } else {
                if (request.size == 0) {
                    appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject(mapOf("general" to UserProfileErrors.RESULT_ERROR_EMPTY_REQUEST))))
                } else {
                    appStore.dispatch(UserProfileActions.changeProperty("errors", errors))
                }
            }
            return request
        }

        /**
         * Action used to cancel profile update procedure
         */
        fun cancel() {
            val state = getStateOf("UserProfile")!!
            val userState = getStateOf("User")!!
            appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject()))
            if (userState["default_room"].toString().isEmpty()) {
                appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject(mapOf("default_room" to UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY))))
            } else {
                val rooms = state["rooms"] as JSONArray
                val filtered_rooms = rooms.filter {
                    val room = it as JSONObject
                    it["_id"].toString() == userState["default_room"].toString()
                }
                if (filtered_rooms.size!=1) {
                    appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject(mapOf("default_room" to UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY))))
                } else {
                    appStore.dispatch(UserProfileActions.changeProperty("first_name",userState["first_name"].toString()))
                    appStore.dispatch(UserProfileActions.changeProperty("last_name",userState["last_name"].toString()))
                    appStore.dispatch(UserProfileActions.changeProperty("gender",userState["gender"].toString()))
                    appStore.dispatch(UserProfileActions.changeProperty("birthDate",userState["birthDate"].toString().toInt()))
                    appStore.dispatch(UserProfileActions.changeProperty("default_room",userState["default_room"].toString()))
                    appStore.dispatch(UserProfileActions.changeProperty("profileImage",userState["profileImage"]))
                    appStore.dispatch(UserProfileActions.changeProperty("password",""))
                    appStore.dispatch(UserProfileActions.changeProperty("confirm_password",""))
                    appStore.dispatch(UserProfileActions.changeProperty("errors",JSONObject()))
                    appStore.dispatch(Actions.changeActivity(AppScreens.CHAT))
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
                                "update_user" -> {
                                    this.handleUpdateUserResponse(request, response)
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * This function used to process response for update_user request
         *
         * @param request - original request, response to which received
         * @param response - body of received response
         */
        fun handleUpdateUserResponse(request:HashMap<String,Any>,response:JSONObject) {
            if ("ok" == response["status"] as String) {
                appStore.dispatch(UserProfileActions.changeProperty("show_progress_indicator",false))
                if (request["first_name"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("first_name", request["first_name"].toString()))
                }
                if (request["last_name"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("last_name",request["last_name"].toString()))
                }
                if (request["gender"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("gender",request["gender"].toString()))
                }
                if (request["birthDate"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("birthDate",request["birthDate"].toString().toInt()))
                }
                if (request["default_room"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("default_room",request["default_room"].toString()))
                }
                if (request["profileImage"]!=null) {
                    appStore.dispatch(UserActions.changeProperty("profileImage",request["profileImage"] as ByteArray))
                }
                appStore.dispatch(UserProfileActions.changeProperty("password",""))
                appStore.dispatch(UserProfileActions.changeProperty("confirm_password",""))
                appStore.dispatch(Actions.changeActivity(AppScreens.CHAT))
                UserProfileActions.messageCenter.removePendingResponse(request["request_id"].toString())
            } else if ("error" == response["status"] as String) {
                appStore.dispatch(UserProfileActions.changeProperty("show_progress_indicator",false))
                val errors = JSONObject()
                try {
                    val error = UserProfileActions.UserProfileErrors.valueOf(response["status_code"].toString())
                    if (error == UserProfileErrors.RESULT_ERROR_PASSWORDS_SHOULD_MATCH) {
                        errors["password"] = error
                    } else if (error == UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY || error == UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE) {
                        val userState = getStateOf("UserProfile")!!
                        if (response["field"]!=null && userState.containsKey(response["field"].toString())) {
                            errors[response["field"].toString()] = error
                        } else {
                            errors["general"] = UserProfileErrors.RESULT_ERROR_UNKNOWN
                        }
                    } else {
                        errors["general"] = error
                    }
                } catch (e:Exception) {
                    errors["general"] = UserProfileActions.UserProfileErrors.RESULT_ERROR_UNKNOWN
                }
                appStore.dispatch(UserProfileActions.changeProperty("errors",errors))
                UserProfileActions.messageCenter.removePendingResponse(request["request_id"].toString())
            }
        }
    }
}