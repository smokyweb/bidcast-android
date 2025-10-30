package io.bidswipe.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class StreamModel(
	var roomId : String ,
	var streamId : String
) : Parcelable