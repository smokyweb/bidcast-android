package io.bidswipe.app.model

import org.json.JSONArray
import org.json.JSONObject

data class PollModel(
    var pollId: Int?,
    var roomId: String?,
    var question: String?,
    var options: List<PollOption>,
    var totalVotes: Int,
    var remainingTime: String?,
    var isActive: Boolean,
    var userVotedOption: Int? = null // Index of option user voted for
) {
    data class PollOption(
        var text: String,
        var voteCount: Int = 0,
        var percentage: Int = 0,
        var isSelected: Boolean = false
    )

    companion object {
        fun fromJson(json: JSONObject): PollModel {
            // Handle pollId as Int (can be null if not present)
            val pollId = if (json.has("pollId")) {
                json.optInt("pollId")
            } else {
                null
            }
            
            val roomId = json.optString("roomId", null)
            val question = json.optString("question", null)
            val remainingTime = json.optString("remainingTime", null)
            val isActive = json.optBoolean("isActive", true)
            val totalVotes = json.optInt("totalVotes", 0)
            val optionsArray = json.optJSONArray("options") ?: JSONArray()

            val options = mutableListOf<PollOption>()
            for (i in 0 until optionsArray.length()) {
                val optionObj = optionsArray.optJSONObject(i)
                if (optionObj != null) {
                    val optionText = optionObj.optString("text", "")
                    val voteCount = optionObj.optInt("voteCount", 0)
                    val percentage = optionObj.optInt("percentage", 0)

                    options.add(
                        PollOption(
                            text = optionText,
                            voteCount = voteCount,
                            percentage = percentage,
                        )
                    )
                }
            }

            return PollModel(
                pollId = pollId,
                roomId = roomId,
                question = question,
                options = options,
                totalVotes = totalVotes,
                remainingTime = remainingTime,
                isActive = isActive
            )
        }
    }
}

