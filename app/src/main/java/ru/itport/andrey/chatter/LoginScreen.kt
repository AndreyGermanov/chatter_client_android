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


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LoginScreen.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LoginScreen.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginScreen : Fragment(), View.OnClickListener {

    private var mListener: OnFragmentInteractionListener? = null

    lateinit var loginField: EditText
    lateinit var passwordField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view =  inflater!!.inflate(R.layout.fragment_login_screen, container, false)
        var btn = view.findViewById<Button>(R.id.loginBtn)
        btn.setOnClickListener(this)
        this.loginField = view.findViewById(R.id.loginField)
        this.passwordField = view.findViewById(R.id.passwordField)
        return view
    }

    override fun onClick(view:View) {
        mListener?.onLoginButtonClick()
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
        // TODO: Update argument type and name
        fun onLoginButtonClick()
    }
}
