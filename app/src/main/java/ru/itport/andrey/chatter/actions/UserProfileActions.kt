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