@file:Suppress("PropertyName")

package io.bidswipe.app.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.permissionx.guolindev.PermissionX
import io.bidswipe.app.utils.Alerts


abstract class BaseFragment<VM : ViewModel , BIND : ViewBinding> : Fragment() {

	protected lateinit var viewModel : VM
	protected lateinit var mCtx : Context
	protected lateinit var bind : BIND

	protected lateinit var userId : String


	protected lateinit var TAG : String

	override fun onCreateView(inflater : LayoutInflater , view : ViewGroup? , savedInstanceState : Bundle?) : View? {
		bind = getBind(inflater , view)
		mCtx = inflater.context

		TAG = try {
			findNavController().currentDestination?.label.toString().uppercase()
		} catch (e : Exception) {
			"FRAGMENT_$tag"
		}

/*		userId = Prefs(mCtx).getUserData()?.id.toString()
		authUserData = Prefs(mCtx).getUserData()
		viewModel = ViewModelProvider(requireActivity())[getModel()]*/

		return bind.root
	}

	protected fun log(msg : String) {
		Alerts.log(TAG , msg)
	}

	protected fun errorToast(msg : String) {
		Alerts.error(mCtx , msg)
	}

	protected fun successToast(msg : String) {
		Alerts.success(mCtx , msg)
	}

	protected fun requestPerms(perms : Array<String> , result : (status : Boolean) -> Unit) {
		PermissionX.init(this)
			.permissions(*perms)
			.explainReasonBeforeRequest()
			.onExplainRequestReason { scope , deniedList ->
				scope.showRequestReasonDialog(deniedList , "Grant Permission" , "OK" , "Cancel")
			}.onForwardToSettings { scope , deniedList ->
				scope.showForwardToSettingsDialog(deniedList , "You need to allow necessary permissions in Settings manually" , "OK" , "Cancel")
			}.request { allGranted , grantedList , deniedList ->
				if (allGranted) {
					Alerts.log(TAG , "GRANTED : $grantedList")
					result(true)
				} else {
					Alerts.log(TAG , "DENIED : $deniedList")
					result(false)
				}
			}
	}

	protected fun onBackPressed(callBack : () -> Unit) {
		activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner , object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				callBack.invoke()
			}
		})
	}

	abstract fun getModel() : Class<VM>

	abstract fun getBind(inflater : LayoutInflater , view : ViewGroup?) : BIND

}