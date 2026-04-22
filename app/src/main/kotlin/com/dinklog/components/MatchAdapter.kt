package com.dinklog.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dinklog.R
import com.dinklog.databinding.ItemMatchBinding
import com.dinklog.models.Match
import com.dinklog.utils.formatMatchDate

class MatchAdapter(
    private val onMatchClick: (Long) -> Unit
) : PagingDataAdapter<Match, MatchAdapter.MatchViewHolder>(MatchDiffCallback) {

    class MatchViewHolder(
        private val binding: ItemMatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) {
            val context = binding.root.context
            binding.score.text = context.getString(
                R.string.match_score_format, match.teamScore, match.opponentScore
            )
            binding.opponents.text = buildOpponentText(match)
            binding.dateLocation.text = context.getString(
                R.string.match_date_location_format,
                match.matchDate.formatMatchDate(),
                match.locationName
            )
            val badge = binding.resultBadge
            badge.text = context.getString(
                if (match.isWin) R.string.match_result_win else R.string.match_result_loss
            )
            badge.setBackgroundResource(
                if (match.isWin) R.drawable.bg_win_badge else R.drawable.bg_loss_badge
            )
            binding.root.contentDescription = buildRowContentDescription(match)
        }

        private fun buildOpponentText(match: Match): String {
            val context = binding.root.context
            val separator = context.getString(R.string.match_opponents_separator)
            val opponents = listOfNotNull(match.opponent1Name, match.opponent2Name)
                .joinToString(separator)
            return if (match.partnerName != null) {
                context.getString(R.string.match_vs_with_partner_format, opponents, match.partnerName)
            } else {
                context.getString(R.string.match_vs_format, opponents)
            }
        }

        private fun buildRowContentDescription(match: Match): String {
            val context = binding.root.context
            val opponents = listOfNotNull(match.opponent1Name, match.opponent2Name)
                .joinToString(" and ")
            val template = if (match.isWin) R.string.cd_match_row_win
                else R.string.cd_match_row_loss
            return context.getString(
                template,
                match.teamScore, match.opponentScore,
                opponents, match.matchDate.formatMatchDate(), match.locationName
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = getItem(position) ?: return
        holder.bind(match)
        holder.itemView.setOnClickListener { onMatchClick(match.matchId) }
    }
}

private object MatchDiffCallback : DiffUtil.ItemCallback<Match>() {

    override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean =
        oldItem.matchId == newItem.matchId

    override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean =
        oldItem == newItem
}
