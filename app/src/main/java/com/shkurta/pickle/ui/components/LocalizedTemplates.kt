package com.shkurta.pickle.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shkurta.pickle.R
import com.shkurta.pickle.domain.model.PollDraftSeed
import com.shkurta.pickle.domain.model.PollStarter
import com.shkurta.pickle.domain.model.WeeklySchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import javax.inject.Inject

private val PollStarter.titleRes: Int get() = when (this) {
    PollStarter.FILM -> R.string.starter_film
    PollStarter.PIZZA -> R.string.starter_pizza
    PollStarter.DATE -> R.string.starter_date
    PollStarter.PLACE -> R.string.starter_place
    PollStarter.GAME -> R.string.starter_game
    PollStarter.DRIVER -> R.string.starter_driver
}

private val PollStarter.questionRes: Int get() = when (this) {
    PollStarter.FILM -> R.string.starter_film_question
    PollStarter.PIZZA -> R.string.pizza_question
    PollStarter.DATE -> R.string.starter_date_question
    PollStarter.PLACE -> R.string.starter_place_question
    PollStarter.GAME -> R.string.starter_game_question
    PollStarter.DRIVER -> R.string.starter_driver
}

@Composable
fun PollStarter.localizedTitle(): String = stringResource(titleRes)

@Composable
fun PollStarter.localizedSubtitle(): String {
    val closing = when (hours) {
        0L -> stringResource(R.string.closes_tonight)
        48L -> pluralStringResource(R.plurals.days, 2, 2)
        else -> stringResource(R.string.closes_hours, hours)
    }
    return stringResource(R.string.starter_subtitle, closing,
        stringResource(if (mode == com.shkurta.pickle.domain.model.VotingMode.SINGLE) R.string.single_choice else R.string.multi_choice))
}

@Composable
fun WeeklySchedule.localizedLabel(): String {
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val day = java.time.DayOfWeek.of(weekday).getDisplayName(TextStyle.FULL, locale)
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale,
        if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm")
    val clock = LocalTime.parse(time).format(DateTimeFormatter.ofPattern(pattern, locale))
    return stringResource(R.string.weekly_schedule, day, clock)
}

@Composable
fun localizedDateTime(instant: java.time.Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val skeleton = if (android.text.format.DateFormat.is24HourFormat(context)) "EEEHm" else "EEEhm"
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return instant.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, locale))
}

@Composable
fun resultCountLabel(selected: Int, total: Int, multiple: Boolean): String =
    pluralStringResource(if (multiple) R.plurals.result_voter_count else R.plurals.result_vote_count, total, selected, total)

@Composable
fun voterSelectionLabel(voters: Int, selections: Int): String = stringResource(
    R.string.voter_selection_summary,
    pluralStringResource(R.plurals.voter_count, voters, voters),
    pluralStringResource(R.plurals.selection_count, selections, selections),
)

@Composable
fun percentageLabel(value: Int): String =
    java.text.NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(value / 100.0)

@Composable
fun carriedOptionsLabel(history: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val date = runCatching {
        java.time.LocalDate.parse(history).format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    }.getOrElse { history.removePrefix("Options carried over from ") }
    return stringResource(R.string.carried_options, date)
}

/** Resolve built-in draft content once, before it becomes editable user content. */
fun interface StarterLocalization {
    fun seed(starter: PollStarter): PollDraftSeed
}

class AndroidStarterLocalization @Inject constructor(@param:ApplicationContext private val context: Context) : StarterLocalization {
    override fun seed(starter: PollStarter): PollDraftSeed {
        val seed = starter.seed()
        val options = when (starter) {
            PollStarter.PIZZA -> context.resources.getStringArray(R.array.pizza_options).toList()
            PollStarter.PLACE -> context.resources.getStringArray(R.array.place_options).toList()
            PollStarter.GAME -> context.resources.getStringArray(R.array.game_options).toList()
            PollStarter.DATE -> {
                val locale = context.resources.configuration.locales[0]
                (1L..7L).map { java.time.LocalDate.now().plusDays(it).format(DateTimeFormatter.ofPattern("EEE d MMM", locale)) }
            }
            else -> seed.options
        }
        return seed.copy(title = context.getString(starter.titleRes), question = context.getString(starter.questionRes), options = options)
    }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class StarterLocalizationModule {
    @dagger.Binds
    abstract fun bindStarterLocalization(implementation: AndroidStarterLocalization): StarterLocalization
}
