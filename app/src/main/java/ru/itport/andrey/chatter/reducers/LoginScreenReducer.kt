/**
 * Created by Andrey Germanov on 2/27/18.
 */

package ru.itport.andrey.chatter.reducers

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.store.LoginFormMode

/**
 * Reducer which mutates state of LoginScreen according
 * to actions
 */
fun LoginScreenReducer(state:JSONObject,action:Any):JSONObject {
    val parser = JSONParser()
    val newState = state
    val act = action as HashMap<String,Any>
    when (act.get("type") as LoginScreenActions.LoginScreenActionTypes) {
        LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE -> newState["mode"] = act.get("mode") as LoginFormMode
    }
    return newState
}