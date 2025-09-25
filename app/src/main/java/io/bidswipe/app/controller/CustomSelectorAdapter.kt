package io.bidswipe.app.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import io.bidswipe.app.R
import io.bidswipe.app.databinding.UserSelectorItemBinding
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class CustomSelectorAdapter(
	context : Context ,
	resource : Int ,
	objects : MutableList<UserSearchingResponse.Data?> ,
	var selected : (index : Int , name : String) -> Unit ,
) : ArrayAdapter<UserSearchingResponse.Data?>(context , resource , objects) {

	override fun getView(position : Int , convertView : View? , parent : ViewGroup) : View {
		var view = convertView

		var bind : UserSelectorItemBinding? = null
		if (view == null) {
			view = LayoutInflater.from(context).inflate(R.layout.user_selector_item , parent , false)
			bind = UserSelectorItemBinding.bind(view !!)
		}

		bind?.userImage?.loadUrl(context , getItem(position)?.profileImage.toString())
		bind?.text?.text = getItem(position)?.name

        bind?.root?.setHapticClickListener {
			selected(position , getItem(position)?.name.toString())
		}

		return view
	}

}