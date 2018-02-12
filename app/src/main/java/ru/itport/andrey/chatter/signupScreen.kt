package ru.itport.andrey.chatter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.fragment_signup_screen.*
import java.util.LinkedHashMap


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [signupScreen.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [signupScreen.newInstance] factory method to
 * create an instance of this fragment.
 */
class signupScreen : Fragment(), View.OnClickListener {

    private var mListener: OnFragmentInteractionListener? = null

    lateinit var signupLoginField: EditText
    lateinit var signupPasswordField: EditText
    lateinit var signupPasswordAgainField: EditText
    lateinit var signupEmailField: EditText
    lateinit var registerBtn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view = inflater!!.inflate(R.layout.fragment_signup_screen, container, false)
        val btn = view.findViewById<Button>(R.id.registerBtn)
        this.signupLoginField = view.findViewById(R.id.signupLoginField)
        this.signupPasswordField = view.findViewById(R.id.signupPasswordField)
        this.signupPasswordAgainField = view.findViewById(R.id.signupPasswordAgainField)
        this.signupEmailField = view.findViewById(R.id.signupEmailField)
        btn.setOnClickListener(this)
        return view
    }


    override fun onClick(view: View) {
        mListener?.onRegisterButtonClick()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context

        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onRegisterButtonClick()
    }

}// Required empty public constructor
