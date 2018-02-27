package ru.itport.andrey.chatter

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import trikita.anvil.DSL.*
import trikita.anvil.BaseDSL.WRAP
import android.widget.LinearLayout
import org.json.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import trikita.anvil.Anvil

import trikita.anvil.BaseDSL
import trikita.anvil.RenderableView



class MainActivity : AppCompatActivity() {

    var state:JSONObject

    init {
        val currentState = appStore.getState() as JSONObject
        state = currentState["LoginForm"] as JSONObject
    }
    /**
     * Function runs when activity starts or restarts
     * It draws view of current activity and subscribes to application store,
     * which provides notifications about any changes in application state,
     * which requires to update current view
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getView())
        appStore.subscribe {
            val currentState = appStore.getState() as JSONObject
            state = currentState["LoginForm"] as JSONObject
            Anvil.render()
        }
    }

    /**
     * Function used to construct view, it draws all items
     * and provides it to setContentView function
     */
    fun getView() : RenderableView {
        return object: RenderableView(this) {
            override fun view() {
                linearLayout {
                    size(MATCH, MATCH)
                    padding(dip(8))
                    orientation(LinearLayout.VERTICAL)
                    linearLayout {
                        gravity(BaseDSL.CENTER_HORIZONTAL)
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        button {
                            text("Login")
                            backgroundColor(Color.BLUE)
                            textColor(Color.WHITE)
                            onClick { v ->
                                appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.LOGIN))
                            }
                        }
                        button {
                            text("Register")
                            backgroundColor(Color.WHITE)
                            textColor(Color.BLACK)
                            onClick { v ->
                                appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.REGISTER))
                            }
                        }
                    }
                    linearLayout {
                        orientation(LinearLayout.VERTICAL)
                        visibility(state["mode"] as LoginFormMode == LoginFormMode.LOGIN)
                        tableLayout {
                            size(MATCH,MATCH)
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text("Login")
                                }
                                editText {
                                    text("")
                                }
                            }
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)
                                textView {
                                    text("Password")
                                }
                                editText {
                                    text("")
                                }
                            }
                        }
                        tableLayout {
                            size(MATCH,WRAP)
                            button {
                                size(MATCH, WRAP)
                                text("LOGIN")
                                onClick { v ->

                                }
                            }
                        }
                    }
                    linearLayout {
                        size(MATCH,MATCH)
                        visibility(state["mode"] as LoginFormMode == LoginFormMode.REGISTER)
                        orientation(LinearLayout.VERTICAL)
                        tableLayout {
                            tableRow {
                                orientation(LinearLayout.HORIZONTAL)

                            }
                        }
                        tableLayout {
                            size(MATCH,WRAP)
                            button {
                                size(MATCH, WRAP)
                                text("REGISTER")
                                onClick { v ->

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
