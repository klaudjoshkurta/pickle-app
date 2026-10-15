package com.shkurta.pickle.ui.poll

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
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(existing==null) "Save as template" else "Edit template")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PickleTextField(name,{name=it.take(80)},placeholder="Template name")
            Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                PollStarter.entries.forEach { starter -> IconButton(onClick={icon=starter.icon},modifier=Modifier.semantics {contentDescription=starter.title;selected=icon==starter.icon},colors=IconButtonDefaults.iconButtonColors(containerColor=if(icon==starter.icon) Pickle.colors.mustard else Color.Transparent)) {
                    TemplateIcon(starter.icon)
                } }
            }
            Row { Text("Weekly closing time",Modifier.weight(1f)); Switch(weekly,{weekly=it}) }
            if(weekly) {
                DayOfWeek.entries.chunked(4).forEach { days -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    days.forEach { day -> FilterChip(weekday==day.value,{weekday=day.value},label={Text(day.name.take(3))}) }
                } }
                PickleTextField(time,{time=it},placeholder="Local time (HH:mm)")
                PickleTextField(zone,{zone=it},placeholder="Timezone, e.g. Europe/Athens")
                if(!validSchedule) Text("Enter a valid time and IANA timezone.",color=MaterialTheme.colorScheme.error)
            }
            Text("Each use opens an editable draft. Your template is private.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(enabled=actionsEnabled && name.isNotBlank() && (!weekly || validSchedule),onClick={onSave(name.trim(),icon,if(weekly) WeeklySchedule(weekday,time,zone) else null)}){Text("Save template")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
