/**
 * Created by Andrey Germanov on 3/10/18.
 */
package ru.itport.andrey.chatter.views

import org.json.simple.JSONObject
import ru.itport.andrey.chatter.store.appStore
import trikita.anvil.DSL.*
import trikita.anvil.RenderableView

/**
 * Class represents User Profile screen
 */
class UserProfileScreen : BaseScreen() {

    /**
     * Class constructor
     */
    init {
        val currentState = appStore.getState() as JSONObject
        state = currentState["UserProfile"] as JSONObject
    }

    /**
     * Function, which returns layout to draw on screen with all items
     */
    override fun drawView(): RenderableView {
        return object: RenderableView(this) {
            override fun view() {
                linearLayout {
                    textView {
                        text("User profile")
                    }
                }
            }
        }
    }
}
