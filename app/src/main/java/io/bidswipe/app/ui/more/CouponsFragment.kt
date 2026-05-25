package io.bidswipe.app.ui.more

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CouponsAdapter
import io.bidswipe.app.databinding.CreateSellerCouponSheetBinding
import io.bidswipe.app.databinding.FragmentCouponsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.request.CreateSellerCouponRequest
import io.bidswipe.app.network.response.GetCouponsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.value
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CouponsFragment : BaseFragment<MoreViewModel, FragmentCouponsBinding>() {
    override fun getModel() = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentCouponsBinding.inflate(inflater, view, false)

    private var couponList = mutableListOf<GetCouponsResponse.Data?>()
    private var createSheet: BottomSheetDialog? = null
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        // MC cmph7xsgw00g2ms8pmtc6xgz5 (Trey 2026-05-22): + icon on the
        // header opens a Create Coupon bottom sheet. POSTs to the new
        // seller-coupons endpoint and refreshes the list on success.
        bind.header.onMorePrimaryClick {
            showCreateCouponSheet()
        }

        bind.couponRecycler.adapter = CouponsAdapter(couponList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })

        bind.loader.isVisible = true
        viewModel.getCoupon()
        viewModel.getCouponRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    couponList.clear()

                    if (mData?.isNotEmpty() == true) {
                        bind.noData.isVisible = false
                        bind.couponRecycler.isVisible = true
                        couponList.addAll(mData)
                    } else {
                        bind.noData.isVisible = true
                    }

                    bind.couponRecycler.adapter?.notifyDataSetChanged()

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

        // Observe seller-coupon create result.
        viewModel.createSellerCouponRepo.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    createSheet?.findViewById<View>(io.bidswipe.app.R.id.loader)?.isVisible = false
                    createSheet?.findViewById<View>(io.bidswipe.app.R.id.saveBtn)?.isEnabled = true
                    successToast(res.value.message ?: "Coupon created.")
                    createSheet?.dismiss()
                    bind.loader.isVisible = true
                    viewModel.getCoupon()
                }
                is Resource.Error -> {
                    createSheet?.findViewById<View>(io.bidswipe.app.R.id.loader)?.isVisible = false
                    createSheet?.findViewById<View>(io.bidswipe.app.R.id.saveBtn)?.isEnabled = true
                    res.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }
                else -> {}
            }
        }

    }

    private fun showCreateCouponSheet() {
        val sheetBind = CreateSellerCouponSheetBinding.inflate(layoutInflater)
        createSheet = Alerts.appBottomSheet(mCtx, true, sheetBind)

        // Default the type toggle to Percent.
        sheetBind.typeToggle.check(io.bidswipe.app.R.id.typePercent)

        // Wire date pickers (the EditTexts are read-only / click-only).
        val cal = Calendar.getInstance()
        val openDate = { onPicked: (String) -> Unit ->
            DatePickerDialog(
                mCtx,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    onPicked(dateFmt.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        sheetBind.startDateInput.setOnClickListener {
            openDate { picked -> sheetBind.startDateInput.setText(picked) }
        }
        sheetBind.expDateInput.setOnClickListener {
            openDate { picked -> sheetBind.expDateInput.setText(picked) }
        }

        sheetBind.close.setOnClickListener { createSheet?.dismiss() }

        sheetBind.saveBtn.setOnClickListener {
            val code = sheetBind.codeInput.value().trim()
            val type = when (sheetBind.typeToggle.checkedButtonId) {
                io.bidswipe.app.R.id.typeFlat -> "flat"
                else -> "percentage"
            }
            val valueStr = sheetBind.valueInput.value().trim()
            val startDate = sheetBind.startDateInput.value().trim()
            val expDate = sheetBind.expDateInput.value().trim()
            val productIdsRaw = sheetBind.productIdsInput.value().trim()

            // Client-side validation. Server validates again.
            if (code.isEmpty()) { Alerts.error(mCtx, "Enter a coupon code"); return@setOnClickListener }
            val value = valueStr.toDoubleOrNull()
            if (value == null || value <= 0.0) { Alerts.error(mCtx, "Enter a valid discount amount"); return@setOnClickListener }
            if (type == "percentage" && value > 100.0) { Alerts.error(mCtx, "Percent discount cannot exceed 100"); return@setOnClickListener }
            if (startDate.isEmpty()) { Alerts.error(mCtx, "Pick a start date"); return@setOnClickListener }
            if (expDate.isEmpty()) { Alerts.error(mCtx, "Pick an expiry date"); return@setOnClickListener }
            if (expDate < startDate) { Alerts.error(mCtx, "Expiry must be on or after start"); return@setOnClickListener }
            val productIds = productIdsRaw
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            if (productIds.isEmpty()) {
                Alerts.error(mCtx, "Enter at least one product ID (comma-separated)")
                return@setOnClickListener
            }

            sheetBind.loader.isVisible = true
            sheetBind.saveBtn.isEnabled = false

            viewModel.createSellerCoupon(
                CreateSellerCouponRequest(
                    name = code,
                    type = type,
                    value = value,
                    startDate = startDate,
                    expDate = expDate,
                    productIds = productIds
                )
            )
        }
    }

}
