package ru.itport.andrey.chatter.actions

import ru.itport.andrey.chatter.store.LoginFormMode

/**
 * Created by andrey on 2/27/18.
 */

/**
 * Class groups all actions, which user can implement on LoginScreen.
 * Action functions of this class used to dispatch events to store via Redux
 */
class LoginScreenActions {

    /**
     * Possible action identifiers
     */
    enum class LoginScreenActionTypes {
        SWITCH_MODE,
        DO_LOGIN,
        DO_REGISTER,
        CHANGE_TEXT_FIELD
    }

    companion object {
        /**
         * Actions switches mode of LoginWindow from "Login" to "Register" and
         * from "Register" to "Login", when user presses appropriate button on
         * top of screen
         */
        public fun switchMode(mode:LoginFormMode):Any {
            return mapOf(
                    "type" to LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE,
                    "mode" to mode

            )
        }
    }

}