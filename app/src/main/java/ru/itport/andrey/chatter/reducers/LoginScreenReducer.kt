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
    val newState = state
    if (action is JSONObject) {
        val act = action as JSONObject
        when (act["type"] as LoginScreenActions.LoginScreenActionTypes) {
            LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE -> newState["mode"] = act["mode"] as LoginFormMode
        }
    }
    return newState
}