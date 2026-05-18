package io.bidswipe.app.ui.more

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.NotificationAdapter
import io.bidswipe.app.databinding.ActivityNotificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.ui.custom.AlertType
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

	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			when (status) {
				// #36: "open" means the user tapped the notification row — the adapter
				// already updated the visual state; nothing extra to do here yet.
				// TODO: call viewModel.markNotificationSeen(id) once the backend endpoint
				// api/notification/seen is available so the seen state persists server-side.
				"open" -> { /* visual read state handled in adapter */ }

				"delete" -> {
					AppBottomSheet(
						this@NotificationActivity,
						R.drawable.trash,
						"Delete!",
						"Are you sure you want to delete?",
						primaryBtnText = "Yes",
						secondaryBtnText = "No",
						canCancel = true,
						showSecondary = true,
						iconPadding = 16,
						alertType = AlertType.ERROR,
						clicks = object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								bind.loader.isVisible = true
								delPos = pos
								val id = notificationList[pos]?.id
								viewModel.deleteNotification(id.toString().request())
							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						}

					).show()

				}
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
		
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0,system.top,0, system.bottom)
			CONSUMED
		}
		
		bind.header.onBackClick {
			finish()
		}

		bind.notificationRec.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.notificationRec.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (notificationList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.getNotification(
							page.toString()
						)
					}
				}
			}
		})

		notificationAdapter = NotificationAdapter(notificationList, mClick)
		bind.notificationRec.adapter = notificationAdapter

		bind.deleteAll.setHapticClickListener {
			bind.loader.isVisible = true
			viewModel.deleteNotification("".request())
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			bind.swipeRefreshLayout.isRefreshing = false
			page = 1
			viewModel.getNotification(page.toString())
		}

		bind.noInternet.onClick {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = true
			page  = 1
			viewModel.getNotification(page.toString())
		}

		bind.noData.onClick {
			bind.noData.isVisible = false
			bind.loader.isVisible = true
			page  = 1
			viewModel.getNotification(page.toString())
		}

		bind.loader.isVisible = true
		viewModel.getNotification(page.toString())
		viewModel.getNotificationRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						bind.noInternet.isVisible = false
						bind.bottomLoader.isVisible = false
						bind.swipeRefreshLayout.isRefreshing = false
						bind.loader.isVisible = false

						val mData = it.value.data

						if (page== 1){
							notificationList.clear()
						}

						if (mData != null) {
							notificationList.addAll(mData)
						}

						notificationAdapter.notifyDataSetChanged()

						log("NotificationList : ${notificationList.size}")

						isLoading = page >= (it.value.totalPage ?: 0)

						bind.noData.isVisible = notificationList.isEmpty() == true
						bind.deleteAll.isVisible = notificationList.isNotEmpty() == true


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

							val isEmpty = notificationList.isNullOrEmpty()
							bind.noData.isVisible = isEmpty
							bind.notificationRec.isVisible = !isEmpty
							bind.deleteAll.isVisible = !isEmpty

						} else {
							page = 1
							viewModel.getNotification(page.toString())
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