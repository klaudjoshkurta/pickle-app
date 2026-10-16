package com.shkurta.pickle.domain.model

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class WeeklySchedule(val weekday: Int, val time: String, val timezone: String) {
    fun nextOccurrence(now: Instant): Instant {
        val zone = ZoneId.of(timezone)
        val local = now.atZone(zone)
        val date = local.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday)))
        val candidate = date.atTime(LocalTime.parse(time)).atZone(zone)
        return (if (candidate.toInstant() > now) candidate else candidate.plusWeeks(1)).toInstant()
    }
    fun label(): String = "${DayOfWeek.of(weekday).name.lowercase().replaceFirstChar(Char::uppercase)}s · ${LocalTime.parse(time).format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

data class PollTemplate(
    val id: String,
    val sourcePollId: String,
    val name: String,
    val icon: String,
    val question: String,
    val optionTexts: List<String>,
    val settings: PollSettings,
    val roundsCount: Int,
    val schedule: WeeklySchedule?,
)

data class PollDraftSeed(
    val title: String,
    val icon: String,
    val question: String,
    val options: List<String>,
    val settings: PollSettings,
    val closesAt: Instant?,
    val templateId: String? = null,
    val history: String? = null,
    val winnerText: String? = null,
    val runnerUpTexts: Set<String> = emptySet(),
    val newTexts: Set<String> = emptySet(),
)

fun PollTemplate.seed(polls: List<Poll>, now: Instant = Instant.now()): PollDraftSeed {
    val completed = polls.filter { (it.templateId == id || it.id == sourcePollId) && it.status == PollStatus.CLOSED }
        .sortedWith(compareByDescending<Poll> { it.templateRound ?: 1 }.thenByDescending { it.closedAt })
    val latest = completed.firstOrNull()
    val previous = completed.getOrNull(1)
    val texts = latest?.approvedOptions?.map { it.text } ?: optionTexts
    val outcome = latest?.toResults()?.outcome
    val winner = (outcome as? Outcome.Winner)?.option?.text
    val remaining = latest?.approvedOptions.orEmpty().filter { it.text != winner && it.voteCount > 0 }
    val second = remaining.maxOfOrNull { it.voteCount }
    val previousTexts = previous?.approvedOptions?.map { it.text.lowercase() }?.toSet()
    return PollDraftSeed(
        title = "$name #${roundsCount + 1}", icon = icon,
        question = latest?.question ?: question, options = texts,
        settings = latest?.settings ?: settings,
        closesAt = schedule?.nextOccurrence(now), templateId = id,
        history = latest?.closedAt?.atZone(ZoneId.of(schedule?.timezone ?: ZoneId.systemDefault().id))
            ?.toLocalDate()?.toString(),
        winnerText = winner,
        runnerUpTexts = if (winner == null) emptySet() else remaining.filter { it.voteCount == second }.map { it.text }.toSet(),
        newTexts = if (previousTexts == null) emptySet() else texts.filter { it.lowercase() !in previousTexts }.toSet(),
    )
}

enum class PollStarter(val id: String, val title: String, val icon: String, val question: String, val mode: VotingMode, val hours: Long) {
    FILM("film", "Film night", "film", "Film night: what are we watching?", VotingMode.SINGLE, 3),
    PIZZA("pizza", "What are we eating?", "pizza", "Pizza night: what are we ordering?", VotingMode.SINGLE, 1),
    DATE("date", "Pick a date", "date", "Which date works for everyone?", VotingMode.MULTIPLE, 48),
    PLACE("place", "Where to meet", "place", "Where should we meet?", VotingMode.SINGLE, 2),
    GAME("game", "Game night", "game", "Game night: what are we playing?", VotingMode.MULTIPLE, 0),
    DRIVER("driver", "Who’s driving?", "driver", "Who’s driving?", VotingMode.SINGLE, 1);

    fun seed(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): PollDraftSeed {
        val options = when (this) {
            FILM -> listOf("Arrival", "Lady Bird", "Spirited Away", "The Grand Budapest Hotel")
            PIZZA -> listOf("Margherita", "Pepperoni", "Vegetarian", "Four cheese")
            DATE -> (1L..7L).map { now.atZone(zone).toLocalDate().plusDays(it).format(DateTimeFormatter.ofPattern("EEE d MMM")) }
            PLACE -> listOf("At home", "Local café", "The park")
            GAME -> listOf("Board games", "Card games", "Video games", "Trivia")
            DRIVER -> listOf("", "")
        }
        val tonight = now.atZone(zone).toLocalDate().atTime(23,59).atZone(zone)
        val closes = if (this == GAME) (if (tonight.toInstant() > now) tonight else tonight.plusDays(1)).toInstant() else now.plusSeconds(hours * 3600)
        return PollDraftSeed(title, icon, question, options, PollSettings(votingMode = mode, allowSuggestions = true), closes)
    }
    val subtitle: String get() = "${if (hours == 0L) "Closes tonight" else if (hours == 48L) "2 days" else "Closes in ${hours} h"} · ${if (mode == VotingMode.SINGLE) "single" else "multi-choice"}"
}
