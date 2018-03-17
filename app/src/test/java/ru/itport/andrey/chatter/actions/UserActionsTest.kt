package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.store.appStore

/**
 * Created by Andrey Germanov on 3/17/18.
 */
class UserActionsTest {

    @Test
    fun changeProperty() {
        val currentState = appStore.state["User"] as JSONObject
        UserActions.changeProperty("", "")
        assertTrue("Should not change anything if property name not provided", currentState.equals(appStore.state["User"] as JSONObject))
        appStore.dispatch(UserActions.changeProperty("not_exist", "Cool setup"))
        var state = appStore.state["User"] as JSONObject
        assertNull("Should not set property which is not initially defined when Store initialized", state["not_exist"])
        appStore.dispatch(UserActions.changeProperty("default_room", "r5"))
        state = appStore.state["User"] as JSONObject
        appStore.dispatch(assertEquals("Should change property correctly", "r5", state["default_room"].toString()))

    }
}