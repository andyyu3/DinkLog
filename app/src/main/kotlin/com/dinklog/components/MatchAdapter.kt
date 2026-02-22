package com.dinklog.components

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dinklog.R
import com.dinklog.databinding.ItemMatchBinding
import com.dinklog.models.Match
import com.dinklog.utils.formatMatchDate

class MatchAdapter : ListAdapter<Match, MatchAdapter.MatchViewHolder>(MatchDiffCallback) {

    class MatchViewHolder(
        private val binding: ItemMatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) {
            binding.score.text = "${match.teamScore} - ${match.opponentScore}"
            binding.opponents.text = buildOpponentText(match)
            binding.dateLocation.text = "${match.matchDate.formatMatchDate()} · ${match.locationName}"

            val badge = binding.resultBadge
            val color = if (match.isWin) {
                badge.context.getColor(R.color.win_green)
            } else {
                badge.context.getColor(R.color.loss_red)
            }
            badge.text = if (match.isWin) "W" else "L"
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            badge.background = bg
        }

        private fun buildOpponentText(match: Match): String {
            val opponents = listOfNotNull(match.opponent1Name, match.opponent2Name)
                .joinToString(" & ")
            val partner = match.partnerName?.let { "w/ $it" } ?: ""
            return if (partner.isNotBlank()) "vs $opponents $partner" else "vs $opponents"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object MatchDiffCallback : DiffUtil.ItemCallback<Match>() {

    override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean =
        oldItem.matchId == newItem.matchId

    override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean =
        oldItem == newItem
}
