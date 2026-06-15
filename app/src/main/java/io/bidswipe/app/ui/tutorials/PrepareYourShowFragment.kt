package io.bidswipe.app.ui.tutorials

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowAdapter
import io.bidswipe.app.databinding.FragmentPrepareYourShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetShowDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.ui.scheduleShow.ShowDetailsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toLiveRehearsal
import io.bidswipe.app.utils.toScheduleShow
import okhttp3.MultipartBody
import java.io.File

@SuppressLint("NotifyDataSetChanged")
class PrepareYourShowFragment : BaseFragment<DashViewModel, FragmentPrepareYourShowBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPrepareYourShowBinding.inflate(inflater, view, false)

	var imagePartList = mutableListOf<MultipartBody.Part?>()
	private var pendingContextShow: GetShowDetailsResponse.Data? = null

	private var scheduleShowLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {

				if (result.data != null) {
					log(
						"DATA : ${result.data?.getStringExtra("date").toString()}"
					)

					viewModel.showDate = result.data?.getStringExtra("date").toString()
					viewModel.showTime = result.data?.getStringExtra("time").toString()
					viewModel.currentStep = 1
					viewModel.showList[0]?.status = "completed"
					viewModel.showList[1]?.status = "locked"
					bind.recycler.adapter?.notifyDataSetChanged()
				}

			}

		}

	private var rehearsalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				viewModel.currentStep = 3
				viewModel.showList.getOrNull(2)?.status = "completed"
				viewModel.showList.getOrNull(3)?.status = "locked"
				bind.recycler.adapter?.notifyDataSetChanged()
			}
		}

	// Basecamp #9986427172: in show-context mode the user returning from LiveRehearsalActivity
	// via back (or any means) should mark step 3 complete — we don't gate on RESULT_OK because
	// LiveRehearsalActivity only sets RESULT_OK when the explicit "End" button is tapped, which
	// means pressing the system back button silently discards progress in training mode but must
	// advance the step when a show context is already present.
	private var rehearsalContextLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
			markRehearsedForCurrentShow()
			viewModel.currentStep = 3
			viewModel.showList.getOrNull(2)?.status = "completed"
			viewModel.showList.getOrNull(3)?.status = "locked"
			bind.recycler.adapter?.notifyDataSetChanged()
		}

	// Basecamp #9986427172: launcher for ShowDetailsActivity in promote mode (step 4, show-context).
	// Mark step 4 complete only after ShowDetailsActivity reports a successful promotion.
	private var promoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			viewModel.currentStep = 4
			viewModel.showList.getOrNull(3)?.status = "completed"
			viewModel.showList.getOrNull(4)?.status = "locked"
			bind.recycler.adapter?.notifyDataSetChanged()
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		val adapter = ShowAdapter(mList = viewModel.showList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				if (status == null) {

					bind.stepProgress.progress = pos + 1

					viewModel.showList.forEachIndexed { index, showModel ->
						showModel?.selected = index == pos
					}

					bind.recycler.adapter?.notifyDataSetChanged()

				} else {
					if (!isStepEnabled(pos)) {
						Alerts.error(mCtx, "Please complete previous step first")
						return
					}
					val hasShowContext = viewModel.showId.isNotBlank()

					when (pos) {
						0 -> {
							// Step 1 "Schedule": in show-context mode the show already has a
							// date/time (prefilled as complete). Guard added just in case the
							// seller taps it anyway — open ScheduleShow normally; in
							// show-context the step is already complete so this branch is
							// typically unreachable.
							scheduleShowLauncher.launch(mCtx.toScheduleShow(from = "tutorial"))
						}

						1 -> {
							findNavController().navigate(ids.goToShowTipsFragment, bundleOf("type" to "showTips"))
						}

						2 -> {
							// Step 3 "Rehearse going live":
							// - Training mode (no showId): standard launcher — advances only on RESULT_OK.
							// - Show-context mode: use rehearsalContextLauncher so ANY return
							//   (including system back) marks the step complete and advances.
							// Basecamp #9986427172.
							if (hasShowContext) {
								rehearsalContextLauncher.launch(mCtx.toLiveRehearsal())
							} else {
								rehearsalLauncher.launch(mCtx.toLiveRehearsal())
							}
						}

						3 -> {
							// Step 4 "Bring in buyers":
							// - Show-context mode: the show already exists — do NOT call
							//   storeScheduleShow (that's the QA error). Instead open
							//   ShowDetailsActivity with autoPromote=true so the promote sheet
							//   opens automatically. On return, promoteLauncher marks step 4
							//   complete and advances to step 5. Basecamp #9986427172.
							// - Training mode: existing storeScheduleShow path unchanged.
							if (hasShowContext) {
								promoteLauncher.launch(
									Intent(mCtx, ShowDetailsActivity::class.java)
										.putExtra("showId", viewModel.showId)
										.putExtra("autoPromote", true)
								)
							} else {
								val mData = viewModel.showData.value

								imagePartList.add(
									Utils.imagePart(
										"thumbnail[]",
										mData?.thumbnail.toString(),
										File(mData?.thumbnail?:"")
									)
								)

								if (mData?.productIds?.isEmpty() == true) {
									Alerts.error(mCtx, "Please select products")
									return
								}

								val productIds = mData?.productIds?.split(",") ?: mutableListOf()

								bind.loader.isVisible = true

								viewModel.storeScheduleShow(
									mData?.showTitle?.request(),
									viewModel.showDate.request(),
									viewModel.showTime.request(),
									mData?.categoryId?.request(),
									mData?.subCategoryId?.request(),
									viewModel.discoverability.request(),
									mData?.actionId?.request(),
									imagePartList,
									productIds.map { it.toInt() },
									mData?.repeatMode?.request(),
									mData?.repeatType?.request(),
									mData?.primaryLanguage?.request(),
									mData?.explicitContent?.request()
								)
							}
						}

						4 -> {
							// Step 5 "Preview show and go live":
							// - Show-context mode: open ShowDetailsActivity(showId). "Start Show"
							//   lives there. Flow ends — finish the TutorialsActivity so back
							//   returns the user to ShowDetailsActivity directly.
							//   Basecamp #9986427172.
							// - Training mode: existing behaviour (navigate to ShowDetailsActivity
							//   with the newly-created showId, or pop if none).
							val sid = viewModel.showId
							if (sid.isNotEmpty()) {
								startActivity(
									Intent(mCtx, ShowDetailsActivity::class.java)
										.putExtra("showId", sid)
								)
								activity?.finish()
							} else {
								findNavController().popBackStack()
							}
						}

					}
				}
			}

		})

		bind.recycler.adapter = adapter

		bind.loader.isVisible = false

		if (viewModel.getPrepareStepRepo.value == null) {
			viewModel.getPrepareStep()
		}

		viewModel.getPrepareStepRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					viewModel.showList.clear()

					mData?.forEach { data ->

						viewModel.showList.add(data)

					}

					bind.stepProgress.max = viewModel.showList.size
					bind.stepProgress.progress = 1

					applyShowContextIfReady()
					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
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

		viewModel.showData.observe(viewLifecycleOwner) {
			if (viewModel.showList.isNotEmpty()) {
				it
				log("SHOW DATA :$it")


				when (viewModel.currentStep) {
					1 -> {
						viewModel.showList[0]?.status = "completed"
						viewModel.showList[1]?.status = "locked"

					}

					2 -> {
						viewModel.showList[1]?.status = "completed"
						viewModel.showList[2]?.status = "locked"
					}

					3 -> {
						viewModel.showList[2]?.status = "completed"
						viewModel.showList[3]?.status = "locked"
					}

					4 -> {
						viewModel.showList[3]?.status = "completed"
						viewModel.showList[4]?.status = "locked"
					}

				}

				bind.recycler.adapter?.notifyDataSetChanged()
			}

		}

		viewModel.storeScheduleShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.storeScheduleShowRepo.value = null
					bind.loader.isVisible = false

					val mData = it.value.data

					viewModel.currentShowData = mData

					viewModel.showId = mData?.id.toString()

					findNavController().navigate(
						ids.goToShowTipsFragment,
						bundleOf("type" to "bringInBuyers", "showId" to mData?.id.toString())
					)

				}

				is Resource.Error -> {
					// Basecamp #9986427172 (Bug B): clear stale error so that
					// re-entering PrepareYourShowFragment after back+reopen does
					// not immediately re-fire this error dialog.
					viewModel.storeScheduleShowRepo.value = null
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
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

		// Basecamp #9991479337 / #9986427172: when a showId is present (entry from
		// ShowDetailsActivity "Let's Prepare"), fetch show details and prefill step
		// completion — mirror the PWA's fetchShowContext() in
		// sellerTrainingLetsPrepareShow.blade.php.
		// Without showId the behaviour is unchanged (manual progression, training mode).
		val contextShowId = viewModel.showId.takeIf { it.isNotBlank() }
		if (contextShowId != null) {
			bind.loader.isVisible = true
			viewModel.getShowDetails(contextShowId)
		}

		viewModel.getShowDetailsRepo.observe(viewLifecycleOwner) { res ->
			when (res) {
				is Resource.Success -> {
					viewModel.getShowDetailsRepo.value = null
					bind.loader.isVisible = false
					val show = res.value.data ?: return@observe
					pendingContextShow = show

					// Show title (visible when show context is active).
					val title = show.title?.takeIf { it.isNotBlank() }
					if (title != null) {
						bind.showContextTitle.text = title
						bind.showContextTitle.isVisible = true
					}

					applyShowContextIfReady()
				}
				is Resource.Error -> {
					viewModel.getShowDetailsRepo.value = null
					bind.loader.isVisible = false
					// Silently ignore — fall through to training-mode behaviour
				}
				else -> {}
			}
		}

	}

	private fun isStepEnabled(position: Int): Boolean {
		if (position == 0) return true
		return (0 until position).all { index ->
			viewModel.showList.getOrNull(index)?.status == "completed"
		}
	}

	private fun markRehearsedForCurrentShow() {
		val sid = viewModel.showId.takeIf { it.isNotBlank() } ?: return
		mCtx.getSharedPreferences("bidcast_prepare", android.content.Context.MODE_PRIVATE)
			.edit()
			.putBoolean("rehearsed_$sid", true)
			.apply()
}

	private fun hasRehearsedCurrentShow(): Boolean {
		val sid = viewModel.showId.takeIf { it.isNotBlank() } ?: return false
		return mCtx.getSharedPreferences("bidcast_prepare", android.content.Context.MODE_PRIVATE)
			.getBoolean("rehearsed_$sid", false)
	}

	private fun applyShowContextIfReady() {
		val show = pendingContextShow ?: return
		if (viewModel.showId.isBlank() || viewModel.showList.isEmpty()) return

		val hasSchedule = !show.date.isNullOrBlank() && !show.time.isNullOrBlank()
		val hasProducts = (show.productIds?.filterNotNull()?.size ?: 0) >= 1
		val hasRehearsed = hasRehearsedCurrentShow()
		val isPromoted = when (val raw = show.isPromoted) {
			is Boolean -> raw
			is Number -> raw.toInt() != 0
			is String -> raw.equals("true", true) || raw == "1"
			else -> show.promoteShowId != null
		}
		val isLive = show.isLive == true
		val flags = listOf(hasSchedule, hasProducts, hasRehearsed, isPromoted, isLive)
		val derivedComplete = flags.takeWhile { it }.size.coerceAtMost(viewModel.showList.size)

		viewModel.showList.forEachIndexed { index, model ->
			model?.status = when {
				index < derivedComplete -> "completed"
				index == derivedComplete -> "locked"
				else -> "locked"
			}
		}
		viewModel.currentStep = derivedComplete
		bind.stepProgress.progress = (derivedComplete + 1).coerceAtMost(viewModel.showList.size)
		bind.recycler.adapter?.notifyDataSetChanged()
	}

	}
