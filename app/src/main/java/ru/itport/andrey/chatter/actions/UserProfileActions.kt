package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject

/**
 * Created by Andrey Germanov on 3/9/18.
 */

import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver


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
        RESULT_ERROR_FIELD_IS_EMPTY("RESULT_ERROR_FIELD_IS_EMPTY"),
        RESULT_ERROR_INCORRECT_FIELD_VALUE("RESULT_ERROR_INCORRECT_FIELD_VALUE"),
        RESULT_ERROR_CONNECTION_ERROR("RESULT_ERROR_CONNECTION_ERROR"),
        RESULT_ERROR_UNKNOWN("RESULT_ERROR_UNKNOWN");
        override fun getMessage():String {
            var result = ""
            when(this) {
                RESULT_OK -> result = ""
                RESULT_ERROR_FIELD_IS_EMPTY -> result = "This field is required."
                RESULT_ERROR_INCORRECT_FIELD_VALUE -> result = "Incorrect field value."
                RESULT_ERROR_CONNECTION_ERROR -> result = "Server connection error."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please contact support."
            }
            return result
        }
    }

    companion object:MessageCenterResponseReceiver {

        /**
         * Action updates appropriate param of state with provided value
         *
         * @param property_name Name of property to set
         * @param property_value Value of property to set
         * @return Action structure as a JSONObject for reducer
         */
        fun changeProperty(property_name: String, property_value: Any): JSONObject {
            return JSONObject(mapOf(
                    "type" to UserProfileActionTypes.CHANGE_PROPERTY,
                    "property_name" to property_name,
                    "property_value" to property_value
            ))
        }

        /**
         * This function receives responses to requests, sent to message center
         */
        override fun handleResponse(request_id: String, response: Any) {

        }
    }
}