package com.shkurta.pickle.ui.poll

import com.shkurta.pickle.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color
import com.shkurta.pickle.ui.theme.Pickle
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.ui.components.*
import java.time.*

@Composable
fun SaveTemplateDialog(poll: Poll, existing: PollTemplate?, onDismiss: () -> Unit, onSave: (String,String,WeeklySchedule?)->Unit) {
    val local = (poll.closedAt ?: Instant.now()).atZone(ZoneId.systemDefault())
    var name by rememberSaveable { mutableStateOf(existing?.name ?: poll.question.substringBefore(':').take(80)) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "film") }
    var weekly by rememberSaveable { mutableStateOf(existing?.schedule != null || poll.closedAt != null) }
    var weekday by rememberSaveable { mutableIntStateOf(existing?.schedule?.weekday ?: local.dayOfWeek.value) }
    var time by rememberSaveable { mutableStateOf(existing?.schedule?.time ?: local.toLocalTime().withSecond(0).withNano(0).toString()) }
    var zone by rememberSaveable { mutableStateOf(existing?.schedule?.timezone ?: local.zone.id) }
    val actionsEnabled = LocalActionsEnabled.current
    val validSchedule = runCatching { LocalTime.parse(time); ZoneId.of(zone) }.isSuccess
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(existing==null) stringResource(R.string.save_as_template) else stringResource(R.string.edit_template))},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PickleTextField(name,{name=it.take(80)},placeholder=stringResource(R.string.template_name))
            Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                PollStarter.entries.forEach { starter ->
                    val title = starter.localizedTitle()
                    IconButton(onClick={icon=starter.icon},modifier=Modifier.semantics {contentDescription=title;selected=icon==starter.icon},colors=IconButtonDefaults.iconButtonColors(containerColor=if(icon==starter.icon) Pickle.colors.mustard else Color.Transparent)) {
                    TemplateIcon(starter.icon)
                } }
            }
            Row { Text(stringResource(R.string.weekly_closing),Modifier.weight(1f)); Switch(weekly,{weekly=it}) }
            if(weekly) {
                DayOfWeek.entries.chunked(4).forEach { days -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    days.forEach { day -> FilterChip(weekday==day.value,{weekday=day.value},label={Text(day.getDisplayName(java.time.format.TextStyle.SHORT, androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))}) }
                } }
                PickleTextField(time,{time=it},placeholder=stringResource(R.string.local_time_hint))
                PickleTextField(zone,{zone=it},placeholder=stringResource(R.string.timezone_hint))
                if(!validSchedule) Text(stringResource(R.string.schedule_error),color=MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.template_private),style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(enabled=actionsEnabled && name.isNotBlank() && (!weekly || validSchedule),onClick={onSave(name.trim(),icon,if(weekly) WeeklySchedule(weekday,time,zone) else null)}){Text(stringResource(R.string.save_template))}},
        dismissButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.cancel))}})
}
