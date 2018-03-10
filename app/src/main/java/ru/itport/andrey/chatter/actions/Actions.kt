package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver
import ru.itport.andrey.chatter.store.AppScreens

/**
 * Created by Andrey Germanov on 3/9/18.
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
 * Base class for all action creators, used to send actions to reducer to update state
 * via Redux
 */
open class Actions {

    /**
     * Class, which defines global application action types
     */
    enum class AppActionTypes  {
        CHANGE_ACTIVITY
    }

    companion object: MessageCenterResponseReceiver {

        /**
         * Action which changes current application screen
         */
        fun changeActivity(activity:AppScreens):JSONObject {
            return JSONObject(mapOf("type" to AppActionTypes.CHANGE_ACTIVITY,"screen" to activity))
        }

        /**
         * This function receives responses to requests, sent to message center
         */
        override fun handleResponse(request_id: String, response: Any) {

        }
    }
}