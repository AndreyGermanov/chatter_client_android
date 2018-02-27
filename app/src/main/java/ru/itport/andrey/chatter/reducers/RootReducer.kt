/**
 * Created by Andrey Germanov on 2/27/18.
 */
package ru.itport.andrey.chatter.reducers

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import redux.api.Reducer
import ru.itport.andrey.chatter.utils.toJSONString

/**
 * Core reducer, which applies changes dispatched to store by actions of all detail
 * reducers
 */
val rootReducer = Reducer { state: JSONObject, action: Any ->
    val parser = JSONParser()
    val newState = parser.parse(toJSONString(state)) as JSONObject
    println(newState)
    newState["LoginForm"] = LoginScreenReducer(newState["LoginForm"] as JSONObject,action)
    newState
}