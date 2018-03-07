/**
 * Created by Andrey Germanov on 2/27/18.
 */
package ru.itport.andrey.chatter.reducers
import org.json.simple.JSONObject
import redux.api.Reducer
import ru.itport.andrey.chatter.store.oldAppState

/**
 * Core reducer, which applies changes dispatched to store by actions of all detail
 * reducers
 */
val rootReducer = Reducer { state: JSONObject, action: Any ->
    val newState = state.clone() as JSONObject
    newState["LoginForm"] = LoginScreenReducer(newState["LoginForm"] as JSONObject,action)
    newState
}