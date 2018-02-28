package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.isValidEmail

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

    /**
     * User registration error definitions
     */
    enum class LoginScreenRegisterErrors {
        RESULT_ERROR_FIELD_IS_EMPTY,
        RESULT_ERROR_INCORRECT_EMAIL,
        RESULT_ERROR_EMAIL_EXISTS,
        RESULT_ERROR_LOGIN_EXISTS,
        RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT,
        RESULT_ERROR_ACTIVATION_EMAIL,
        RESULT_ERROR_UNKNOWN;
        fun getMessage():String {
            var result = ""
            when(this) {
                RESULT_ERROR_FIELD_IS_EMPTY -> result = "Value of this field is required."
                RESULT_ERROR_INCORRECT_EMAIL -> result = "Incorrect email format."
                RESULT_ERROR_EMAIL_EXISTS -> result = "User with provided email already exists."
                RESULT_ERROR_LOGIN_EXISTS -> result = "User with provided login already exists."
                RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT -> result = "Passwords must be the same."
                RESULT_ERROR_ACTIVATION_EMAIL -> result = "Failed to send activation email. Please contact support."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please contact support"
            }
            return result
        }
    }

    /**
     * User login error definitions
     */
    enum class LoginScreenLoginErrors {
        RESULT_ERROR_FIELD_IS_EMPTY,
        RESULT_ERROR_NO_USER,
        RESULT_ERROR_USER_ALREADY_ACTIVATED,
        RESULT_ERROR_UNKNOWN;
        fun getMessage(): String {
            var result = ""
            when (this) {
                RESULT_ERROR_FIELD_IS_EMPTY -> result = "Value of this field is required."
                RESULT_ERROR_NO_USER -> result = "User account not found. Please, try to register again or contact support."
                RESULT_ERROR_USER_ALREADY_ACTIVATED -> result = "User account already activated. You can login now."
                RESULT_ERROR_UNKNOWN -> result = "Unknown error. Please, contact support."
            }
            return result
        }
    }

    companion object {
        /**
         * Action switches mode of LoginWindow from "Login" to "Register" and
         * from "Register" to "Login", when user presses appropriate button on
         * top of screen
         */
        fun switchMode(mode:LoginFormMode): JSONObject {
            return JSONObject(mapOf(
                    "type" to LoginScreenActions.LoginScreenActionTypes.SWITCH_MODE,
                    "mode" to mode)

            )
        }

        /**
         * Action updates appropriate param of state with provided value
         *
         * @param property_name Name of property to set
         * @param property_value Value of property to set
         * @return Action structure as a JSONObject for reducer
         */
        fun changeProperty(property_name:String,property_value:Any): JSONObject {
            return JSONObject(mapOf(
                    "type" to LoginScreenActions.LoginScreenActionTypes.CHANGE_TEXT_FIELD,
                    "property_name" to property_name,
                    "property_value" to property_value
                 )
            )
        }

        /**
         * Action fires when user presses "Register" button on login screen
         *
         * @param params Register form params. Must have following: "login", "password",
         * "confirm_password","email". This is optional param. If it does not provided,
         * it will get values from LoginScreen state fields
         *
         */
        fun register(params:JSONObject? = null) {
            var form = params!!
            if (params == null) {
                form = JSONObject()
                val state = appStore.state["LoginForm"] as JSONObject
                if (state["login"]!=null) {
                    form["login"] = state["login"].toString()
                }
                if (state["password"]!=null) {
                    form["password"] = state["password"].toString()
                }
                if (state["confirm_password"]!=null) {
                    form["confirm_password"] = state["confirm_password"].toString()
                }
                if (state["email"]!=null) {
                    form["email"] = state["email"].toString()
                }
            }

            val errors = JSONObject()
            if (form["login"]==null || form["login"].toString().isEmpty()) {
                errors["login"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            }
            if (form["password"]==null || form["password"].toString().isEmpty()) {
                errors["password"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            } else if (form["password"].toString() != form["confirm_password"].toString()) {
                errors["password"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT
            }
            if (form["email"]==null || form["email"].toString().isEmpty()) {
                errors["email"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY
            } else if (!isValidEmail(form["email"].toString())) {
                errors["email"] = LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_INCORRECT_EMAIL
            }
            if (errors.count()>0) {
                appStore.dispatch(changeProperty("errors",errors))
            } else {
                appStore.dispatch(changeProperty("show_spinner",true))

            }
        }
    }

}