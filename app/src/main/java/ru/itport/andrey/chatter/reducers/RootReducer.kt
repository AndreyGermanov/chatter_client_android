/**
 * Created by Andrey Germanov on 2/27/18.
 */
package ru.itport.andrey.chatter.reducers
import org.json.simple.JSONObject
import redux.api.Reducer
import ru.itport.andrey.chatter.actions.Actions
import ru.itport.andrey.chatter.store.AppScreens

/**
 * Core reducer, which applies changes dispatched to store by actions of all detail
 * reducers
 */
val rootReducer = Reducer { state: JSONObject, action: Any ->
    val newState = state.clone() as JSONObject
    if (action is JSONObject) {
        newState["LoginForm"] = LoginScreenReducer(newState["LoginForm"] as JSONObject, action)
        newState["User"] = UserReducer(newState["User"] as JSONObject, action)
        newState["UserProfile"] = UserProfileReducer(newState["UserProfile"] as JSONObject, action)
        if (action["type"] is Actions.AppActionTypes) {
            when (action["type"] as Actions.AppActionTypes) {
                Actions.AppActionTypes.CHANGE_ACTIVITY -> newState["current_activity"] = action["screen"] as AppScreens
            }
        }
    }
    newState
}