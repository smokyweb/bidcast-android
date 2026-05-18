package io.bidswipe.app.ui.more

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSalesTaxExemptionBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream

class SalesTaxExemptionFragment : BaseFragment<MoreViewModel, FragmentSalesTaxExemptionBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSalesTaxExemptionBinding.inflate(inflater, view, false)

    private var certUri: Uri? = null
    private var certPart: MultipartBody.Part? = null

    private val pickCert = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            certUri = uri
            val name = getFileName(uri) ?: "certificate.pdf"
            Alerts.success(mCtx, "Certificate selected: $name")
            certPart = buildFilePart(uri, name)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        val userData = App.profileResponse.value
        bind.userName.text = userData?.name?.asCapital()
        bind.bio.text = userData?.bio
        bind.userImage.loadUrl(mCtx, userData?.profileImage ?: "", draw.placeholder_user)

        // #41: "Apply Now" opens the submission form dialog
        bind.publish.setHapticClickListener {
            showTaxExemptionFormDialog()
        }

        // "Learn More" opens a help page (no-op placeholder)
        bind.saveDraft.setHapticClickListener {
            Alerts.success(mCtx, "Sales tax exemption allows qualifying buyers to purchase without tax. Upload your exemption certificate to apply.")
        }

        viewModel.applyTaxExemptionRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    Alerts.success(mCtx, it.value.message ?: "Tax exemption application submitted successfully.")
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }
                else -> {}
            }
        }
    }

    private fun showTaxExemptionFormDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tax_exemption_form, null, false)
        val stateInput = dialogView.findViewById<AutoCompleteTextView>(R.id.stateField)
        val typeInput = dialogView.findViewById<AutoCompleteTextView>(R.id.exemptionTypeField)
        val uploadBtn = dialogView.findViewById<View>(R.id.uploadCertBtn)
        val certLabel = dialogView.findViewById<android.widget.TextView>(R.id.certFileName)

        val states = resources.getStringArray(R.array.us_states)
        stateInput.setAdapter(ArrayAdapter(mCtx, android.R.layout.simple_dropdown_item_1line, states))

        val types = arrayOf("Resale", "Non-Profit", "Government", "Educational", "Agricultural", "Other")
        typeInput.setAdapter(ArrayAdapter(mCtx, android.R.layout.simple_dropdown_item_1line, types))

        uploadBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
            }
            pickCert.launch(Intent.createChooser(intent, "Select exemption certificate"))
        }

        certUri?.let {
            certLabel.text = getFileName(it) ?: "Certificate selected"
        }

        MaterialAlertDialogBuilder(mCtx)
            .setTitle("Apply for Tax Exemption")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val state = stateInput.text.toString().trim()
                val type = typeInput.text.toString().trim()
                when {
                    state.isEmpty() -> Alerts.error(mCtx, "Please select a state")
                    type.isEmpty() -> Alerts.error(mCtx, "Please select an exemption type")
                    certPart == null -> Alerts.error(mCtx, "Please upload your exemption certificate")
                    else -> {
                        dialog.dismiss()
                        bind.loader.isVisible = true
                        viewModel.applyTaxExemption(
                            state.request(),
                            type.request(),
                            certPart
                        )
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getFileName(uri: Uri): String? {
        return mCtx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun buildFilePart(uri: Uri, fileName: String): MultipartBody.Part? {
        return try {
            val inputStream = mCtx.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(mCtx.cacheDir, fileName)
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            val mimeType = mCtx.contentResolver.getType(uri) ?: "application/octet-stream"
            Utils.imagePart("certificate", fileName, tempFile)
        } catch (e: Exception) {
            log("buildFilePart error: ${e.message}")
            null
        }
    }
}
