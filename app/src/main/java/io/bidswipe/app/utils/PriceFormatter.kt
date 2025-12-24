package io.bidswipe.app.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText

class PriceFormatter(priceInput: TextInputEditText) : TextWatcher {
    private var current: String = ""
    private val price = priceInput

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
        if (s.toString() != current && s.isNotEmpty()) {
            price.removeTextChangedListener(this)

            // Clean the string (remove non-numeric characters except for the dot)
            var cleanString: String = s.replace("""[^0-9.]""".toRegex(), "")

            // Check if the string contains a dot, and allow only one dot and up to two digits after it
            val decimalIndex = cleanString.indexOf(".")
            if (decimalIndex != -1) {
                // Split the string by the dot and limit to two digits after the dot
                val integerPart = cleanString.substring(0, decimalIndex)
                val decimalPart = cleanString.substring(decimalIndex + 1)
                val limitedDecimalPart = if (decimalPart.length > 2) decimalPart.substring(0, 2) else decimalPart
                cleanString = "$integerPart.$limitedDecimalPart"
            }

            // Ensure the string is still a valid number after modifications
            val parsed = if (cleanString.isNotEmpty() && cleanString != ".") cleanString.toDoubleOrNull() ?: 0.0 else 0.0

            // Format the number without the dollar sign (just numeric value, with two decimal places)
            val formatted = String.format("%.2f", parsed)

            current = formatted

            // Get the current cursor position
            val cursorPosition = price.selectionStart

            // Set the formatted value
            price.setText(formatted)

            // Only set the cursor position if it's valid
            if (cursorPosition <= formatted.length) {
                price.setSelection(cursorPosition)
            } else {
                // If cursor position is beyond the text length, set it to the end of the text
                price.setSelection(formatted.length)
            }

            price.addTextChangedListener(this)
        }
    }

}
