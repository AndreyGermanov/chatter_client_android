/**
 * Created by Andrey Germanov on 3/9/18.
 */
package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver


/**
 * Class contains all actions to change current user object in global application
 * state.
 */
class UserActions: Actions() {
    /**
     * Possible action identifiers
     */
    enum class UserActionTypes {
        CHANGE_PROPERTY
    }

    companion object {

        /**
         * Action updates appropriate param of state with provided value
         *
         * @param property_name Name of property to set
         * @param property_value Value of property to set
         * @return Action structure as a JSONObject for reducer
         */
        fun changeProperty(property_name: String, property_value: Any): JSONObject {
            return JSONObject(mapOf(
                    "type" to UserActions.UserActionTypes.CHANGE_PROPERTY,
                    "property_name" to property_name,
                    "property_value" to property_value
            ))
        }
    }
}