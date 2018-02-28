/**
 * Created by Andrey Germanov on 2/27/18.
 */

package ru.itport.andrey.chatter.reducers

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.store.LoginFormMode

/**
 * Reducer which mutates state of LoginScreen according
 * to actions
 */
fun LoginScreenReducer(state:JSONObject,action:Any):JSONObject {


    // Reducer process
    var newState = state
    if (action is JSONObject) {
        when (action["type"] as LoginScreenActions.LoginScreenActionTypes) {
            LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE -> newState["mode"] = action["mode"] as LoginFormMode
            LoginScreenActions.LoginScreenActionTypes.CHANGE_TEXT_FIELD -> newState[action["property_name"].toString()] = action["property_value"]
        }
    }
    return newState
}