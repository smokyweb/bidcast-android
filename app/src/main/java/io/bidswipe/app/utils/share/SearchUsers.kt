package io.bidswipe.app.utils.share

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.SearchUsersAdapter
import io.bidswipe.app.databinding.SearchUsersSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Chats
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

@AndroidEntryPoint
class SearchUsers : BottomSheetDialogFragment() {
    private var productList = mutableListOf<Product?>()
    private lateinit var mCtx: Context
    private var userList = mutableListOf<UserSearchingResponse.Data?>()
    private var _binding: SearchUsersSheetBinding? = null
    private val bind get() = _binding!!
    val viewModel: ProductViewModel by activityViewModels()
    private var oldText = ""
    private var isLoading = false
    private lateinit var userAdapter: SearchUsersAdapter
    private var shareText=""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mCtx = inflater.context
        _binding = SearchUsersSheetBinding.inflate(inflater, container, false)
        return bind.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(arguments!=null){
            shareText=requireArguments().getString("share_text")?:""
        }else{
            Alerts.error(mCtx,"No share message attached")
            dismiss()
        }

        userList.clear()
        viewModel.searchUsersRepo.value = null

        bind.search.post {
            bind.search.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {

                }

                override fun onTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {

                }

                override fun afterTextChanged(p0: Editable?) {
                    if (p0?.toString()?.isNotEmpty() == true && p0.trim().toString() != oldText) {
                        oldText = p0.trim().toString()
                        if (!isLoading) {
                            isLoading = true
                            bind.loader.isVisible = true
                            bind.noData.isVisible = false
                            viewModel.searchUsers(search = p0.toString().request())
                        }
                    } else if (p0?.toString().isNullOrEmpty()) {
                        // Clear results when search is empty
                        userList.clear()
                        userAdapter.notifyDataSetChanged()
                        bind.noData.isVisible = false
                        bind.loader.isVisible = false
                        isLoading = false
                    }
                }

            })
        }

        userAdapter = SearchUsersAdapter(userList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                userList.forEachIndexed { index, data ->
                    data?.selected = pos == index
                    userAdapter.notifyItemChanged(index, data)
                }
                bind.send.setOnClickListener {
                    sendChat(
                        shareText,
                        userList[pos]?.id.toString(), userList[pos]?.name.toString(), userList[pos]?.profileImage.toString()
                    )
                }

                bind.send.isVisible=true
            }
        })
        bind.users.adapter = userAdapter

        viewModel.searchUsersRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    // Ensure RecyclerView is visible when we have a result
                    bind.users.isVisible = true
                    val mData = it.value.data

                    userList.clear()

                    if (mData?.isNotEmpty() == true) {
                        userList.addAll(mData)
                    }

                    if (userList.isEmpty()) {
                        bind.noData.isVisible = true
                    } else {
                        bind.noData.isVisible = false
                    }
                    Log.d("TAG", "onViewCreated: $userList")
                    userAdapter.notifyDataSetChanged()
                    isLoading = false

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.users.isVisible = false
                    bind.noData.isVisible = false
                    isLoading = false
                    it.parse(mCtx, "Search Sheet", object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })

                }

                else -> {}
            }
        }
    }

    fun sendChat(message: String, receiverId: String, receiverName: String, receiverImage: String) {
        val userId = Prefs(mCtx).getUserData()?.id.toString()
        val chatKey = if (userId > receiverId) {
            receiverId + "_chats_" + userId
        } else {
            userId + "_chats_" + receiverId
        }

        FireRef.CHAT.child(chatKey)

        val chats = Chats(
            mCtx, chatKey, ChatModel.Users(
                "" + userId,
                "" + Prefs(mCtx).getUserData()?.firstName + " " + Prefs(mCtx).getUserData()?.lastName,
                "" + (Prefs(mCtx).getUserData()?.profileImage ?: ""),
                "" + receiverId,
                "" + receiverName,
                "" + receiverImage,
            )
        )

        if (message.isNotEmpty()) {

            val model = ChatModel(isReply = false, message = message, type = Chats.ChatType.SHARE)

            chats.sendChat(model) {
                dismiss()
            }

        }
    }

}