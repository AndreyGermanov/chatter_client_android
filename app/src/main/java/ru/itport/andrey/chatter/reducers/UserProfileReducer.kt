/**
 * Created by Andrey Germanov on 3/9/18.
 */
package ru.itport.andrey.chatter.reducers

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.UserProfileActions

/**
 * Created by Andrey Germanov on 3/9/18.
 */
/**
 * Created by Andrey Germanov on 2/27/18.
 */


/**
 * Reducer which mutates state of User Profile according
 * to actions
 */
fun UserProfileReducer(state: JSONObject, action:Any): JSONObject {
    // Reducer process
    var newState = state
    if (action is JSONObject) {
        if (action["type"] is UserProfileActions.UserProfileActionTypes) {
            when (action["type"] as UserProfileActions.UserProfileActionTypes) {
                UserProfileActions.UserProfileActionTypes.CHANGE_PROPERTY ->{
                    if (newState.containsKey(action["property_name"].toString())) {
                        if (action["property_value"]!=null) {
                            newState[action["property_name"].toString()] = action["property_value"]
                        } else {
                            newState[action["property_name"].toString()] = null
                        }
                    }
                }
            }
        }
    }
    return newState
}