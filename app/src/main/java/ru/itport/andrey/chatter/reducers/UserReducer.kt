package ru.itport.andrey.chatter.reducers

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.actions.UserActions
import ru.itport.andrey.chatter.store.LoginFormMode

/**
 * Created by Andrey Germanov on 3/9/18.
 */
/**
 * Created by Andrey Germanov on 2/27/18.
 */


/**
 * Reducer which mutates state of User according
 * to actions
 */
fun UserReducer(state: JSONObject, action:Any): JSONObject {
    // Reducer process
    var newState = state
    if (action is JSONObject) {
        if (action["type"] is UserActions.UserActionTypes) {
            when (action["type"] as UserActions.UserActionTypes) {
                UserActions.UserActionTypes.CHANGE_PROPERTY -> newState[action["property_name"].toString()] = action["property_value"]
            }
        }
    }
    return newState
}