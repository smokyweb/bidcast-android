package io.bidswipe.app.ui.dashboard.more

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.NotificationAdapter
import io.bidswipe.app.databinding.ActivityNotificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

class NotificationActivity : BaseActivity() {

	private val bind by bind(ActivityNotificationBinding::inflate)
	private val viewModel by viewModels<MoreViewModel>()
	private lateinit var notificationAdapter: NotificationAdapter
	private var notificationList = mutableListOf<GetNotificationResponse.Data?>()
	private var delPos = -1

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			when (status) {
				"delete" -> {
					bind.loader.isVisible = true
					delPos = pos
					val id = notificationList[pos]?.id
					viewModel.deleteNotification(id.toString().request())

				}
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
		bind.header.onBackClick {
			finish()
		}

		notificationAdapter = NotificationAdapter(notificationList, mClick)
		bind.notificationRec.adapter = notificationAdapter

		bind.deleteAll.setHapticClickListener {
			bind.loader.isVisible = true
			viewModel.deleteNotification("".request())
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			bind.swipeRefreshLayout.isRefreshing = false
			viewModel.getNotification()
		}

		bind.noInternet.onClick {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = true
			viewModel.getNotification()
		}

		bind.noData.onClick {
			bind.noData.isVisible = false
			viewModel.getNotification()
		}

		bind.loader.isVisible = true
		viewModel.getNotification()
		viewModel.getNotificationRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						bind.noInternet.isVisible = false
						bind.bottomLoader.isVisible = false
						bind.swipeRefreshLayout.isRefreshing = false
						bind.loader.isVisible = false

						notificationList.clear()
						val mData = it.value.data
						if (mData != null) {
							notificationList.addAll(mData)
						}
						notificationAdapter.notifyDataSetChanged()
						val isEmpty = mData.isNullOrEmpty()
						bind.noData.isVisible = mData?.isEmpty() == true
						bind.deleteAll.isVisible = !isEmpty
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.bottomLoader.isVisible = false

					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.noData.isVisible = false
						bind.deleteAll.isVisible = false
						notificationList.clear()
						notificationAdapter.notifyDataSetChanged()

					} else {
						bind.noInternet.isVisible = false
						bind.deleteAll.isVisible = false
						it.parse(this, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						})
					}


				}

				else -> {}
			}
		}

		viewModel.deleteNotificationRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						bind.loader.isVisible = false

						if (delPos != -1) {
							notificationList.removeAt(delPos)
							notificationAdapter.notifyItemRemoved(delPos)
							notificationAdapter.notifyItemRangeChanged(0, notificationList.size)
						} else {
							viewModel.getNotification()
						}

						delPos = -1
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(this, TAG, object : AlertClicks {
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
}