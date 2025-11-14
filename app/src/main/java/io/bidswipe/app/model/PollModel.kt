package io.bidswipe.app.model

import org.json.JSONArray
import org.json.JSONObject

data class PollModel(
    var pollId: String?,
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
            var pollId = json.optString("poll_id", null)
            var roomId = json.optString("room_id", null)
            var question = json.optString("question", null)
            var remainingTime = json.optString("remaining_time", null)
            var isActive = json.optBoolean("is_active", true)
            var userVotedOption = if (json.has("user_voted_option")) {
                json.optInt("user_voted_option", -1)
            } else null

            var optionsArray = json.optJSONArray("options") ?: JSONArray()
            var votesArray = json.optJSONArray("votes") ?: JSONArray()
            var totalVotes = json.optInt("total_votes", 0)

            val options = mutableListOf<PollOption>()
            for (i in 0 until optionsArray.length()) {
                val optionText = optionsArray.optString(i, "")
                val voteCount = if (i < votesArray.length()) {
                    votesArray.optInt(i, 0)
                } else 0

                val percentage = if (totalVotes > 0) {
                    (voteCount * 100 / totalVotes)
                } else 0

                options.add(
                    PollOption(
                        text = optionText,
                        voteCount = voteCount,
                        percentage = percentage,
                        isSelected = userVotedOption == i
                    )
                )
            }

            return PollModel(
                pollId = pollId,
                roomId = roomId,
                question = question,
                options = options,
                totalVotes = totalVotes,
                remainingTime = remainingTime,
                isActive = isActive,
                userVotedOption = userVotedOption
            )
        }
    }
}

