package com.shkurta.pickle.ui.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shkurta.pickle.R
import com.shkurta.pickle.data.remote.dto.parseDatabaseInstant
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.ui.components.*
import com.shkurta.pickle.ui.theme.Pickle
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDraftEditor(
    state: CreatePollUiState, onBack: ()->Unit, onCreate: ()->Unit,
    onQuestion:(String)->Unit={}, onOption:(Int,String)->Unit={_,_->},onAdd:()->Unit={},onRemove:(Int)->Unit={},
    onMove:(Int,Int)->Unit={_,_->},onExclude:(Boolean)->Unit={},onSettings:(PollSettings)->Unit={},
    onDeadline:(Instant?)->Unit={},onExpectedVoters:(String)->Unit={},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val c=Pickle.colors
    Scaffold(containerColor=c.canvas,topBar={
        CenterAlignedTopAppBar(title={Text("New poll",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)},
            navigationIcon={TextButton(onClick=onBack){Text("Cancel",color=c.tomatoDeep,fontWeight=FontWeight.Bold)}},
            actions={Text("From template",style=MaterialTheme.typography.labelLarge,color=c.inkMuted,modifier=Modifier.padding(end=20.dp))},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=c.canvas))
    },bottomBar={PickleButton("Create & get the link",onCreate,Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal=20.dp,vertical=16.dp),
        style=PickleButtonStyle.Teal,enabled=state.isValid)}) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            PickleCard(color=c.mustard,contentPadding=PaddingValues(horizontal=14.dp,vertical=12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("↻",style=MaterialTheme.typography.headlineSmall)
                    Text("${state.title}${state.history?.let { " — $it" }.orEmpty()}",style=MaterialTheme.typography.bodySmall.copy(fontSize=11.sp))
                }
            }
            PickleCard {
                BasicTextField(state.question,onValueChange=onQuestion,textStyle=MaterialTheme.typography.titleLarge.copy(color=c.ink,fontSize=18.sp,lineHeight=24.sp,fontWeight=FontWeight.ExtraBold),modifier=Modifier.fillMaxWidth())
            }
            val currentKeys by rememberUpdatedState(state.optionKeys)
            val currentMove by rememberUpdatedState(onMove)
            val rowHeight=with(LocalDensity.current){66.dp.toPx()}
            state.options.forEachIndexed { index,text ->
                key(state.optionKeys[index]) {
                    val optionKey=state.optionKeys[index]
                    var orderMenu by remember { mutableStateOf(false) }
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Box {
                            IconButton(onClick={orderMenu=true},modifier=Modifier.width(20.dp).pointerInput(optionKey) {
                                var distance=0f
                                detectDragGesturesAfterLongPress(onDragStart={distance=0f},onDrag={change,amount ->
                                    change.consume();distance+=amount.y
                                    if(kotlin.math.abs(distance)>=rowHeight) {
                                        val from=currentKeys.indexOf(optionKey)
                                        val to=(from+if(distance>0) 1 else -1).coerceIn(0,currentKeys.lastIndex)
                                        if(from>=0 && from!=to) currentMove(from,to)
                                        distance=0f
                                    }
                                })
                            }.semantics {
                                customActions=listOf(
                                    CustomAccessibilityAction("Move option up"){if(index>0){onMove(index,index-1);true}else false},
                                    CustomAccessibilityAction("Move option down"){if(index<state.options.lastIndex){onMove(index,index+1);true}else false})
                            }) {
                                Canvas(Modifier.size(12.dp,20.dp)) {
                                    for(x in listOf(.3f,.7f)) for(y in listOf(.3f,.5f,.7f)) drawCircle(c.inkMuted,1.3.dp.toPx(),Offset(size.width*x,size.height*y))
                                }
                            }
                            DropdownMenu(orderMenu,{orderMenu=false}) {
                                DropdownMenuItem(text={Text("Move up")},enabled=index>0,onClick={onMove(index,index-1);orderMenu=false})
                                DropdownMenuItem(text={Text("Move down")},enabled=index<state.options.lastIndex,onClick={onMove(index,index+1);orderMenu=false})
                            }
                        }
                        PickleTextField(text,{onOption(index,it)},modifier=Modifier.weight(1f),placeholder=if(state.icon=="driver") "Driver’s name" else "Option ${index+1}",
                            trailing=when {
                                text in state.runnerUpTexts -> {{InfoPill("Runner-up last time",container=c.sunken,content=c.inkMuted,borderColor=null)}}
                                text in state.newTexts || optionKey in state.newKeys -> {{InfoPill("New",container=c.tealSoft,content=c.teal,borderColor=null)}}
                                else -> null
                            })
                        IconButton(onClick={onRemove(index)},enabled=state.options.size>MIN_POLL_OPTIONS,modifier=Modifier.size(26.dp)) {
                            Icon(painterResource(R.drawable.ic_close),"Remove option ${index+1}",tint=c.inkMuted,modifier=Modifier.size(18.dp))
                        }
                    }
                }
            }
            if(state.options.size<MAX_POLL_OPTIONS) TextButton(onClick=onAdd) {Text("+ Add an option",color=c.textAccent)}
            state.winnerText?.let { winner ->
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Leave out last time’s winner",fontWeight=FontWeight.Bold)
                        Text("$winner won last time — add it back any time",style=MaterialTheme.typography.bodySmall,color=c.inkMuted)
                    }
                    Switch(checked=state.excludeWinner,onCheckedChange=onExclude,colors=pickleSwitchColors(),enabled=state.excludeWinner || state.options.size<MAX_POLL_OPTIONS)
                }
            }
            PickleCard(onClick={showSettings=true},borderColor=c.lineSoft,contentPadding=PaddingValues(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_clock),null,Modifier.size(18.dp),tint=c.inkMuted)
                    Text(draftSettingsSummary(state),style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
                    Text("✎",color=c.tomatoDeep)
                }
            }
            if(!state.isValid) Text("Use 2–10 distinct, nonblank options to create your poll.",style=MaterialTheme.typography.bodySmall,color=c.inkMuted)
            Spacer(Modifier.height(20.dp))
        }
    }
    if(showSettings) DraftSettingsSheet(state,{showSettings=false},onSettings,onDeadline,onExpectedVoters)
}

fun draftSettingsSummary(state:CreatePollUiState):String {
    val closes=state.customClosesAt?.let {parseDatabaseInstant(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE h:mm a"))}
    val time=closes?.let {"Closes $it"}?:state.deadline.label
    return "$time · ${if(state.settings.votingMode==VotingMode.SINGLE) "single choice" else "multi-choice"} · suggestions ${if(state.settings.allowSuggestions) "on" else "off"}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftSettingsSheet(state:CreatePollUiState,onDismiss:()->Unit,onSettings:(PollSettings)->Unit,onDeadline:(Instant?)->Unit,onExpected:(String)->Unit) {
    var showDate by remember {mutableStateOf(false)}
    var showTime by remember {mutableStateOf(false)}
    var selectedDate by remember {mutableStateOf(LocalDate.now())}
    var dateError by remember {mutableStateOf(false)}
    val zone=ZoneId.systemDefault()
    val current=state.customClosesAt?.let(::parseDatabaseInstant)?.atZone(zone)?:ZonedDateTime.now(zone).plusHours(1)
    val datePicker=rememberDatePickerState(initialSelectedDateMillis=current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val timePicker=rememberTimePickerState(current.hour,current.minute)
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Pickle.colors.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Poll settings",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            SegmentedToggle(VotingMode.entries,state.settings.votingMode,{onSettings(state.settings.copy(votingMode=it))},{if(it==VotingMode.SINGLE) "Single choice" else "Multi-choice"})
            Text("Closing time",fontWeight=FontWeight.Bold)
            listOf(listOf(1L,3L,24L),listOf(48L,72L,168L)).forEach { hours ->
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    hours.forEach {hour -> AssistChip(onClick={onDeadline(Instant.now().plusSeconds(hour*3600))},label={Text(if(hour>=24) "${hour/24} days" else "$hour h")})}
                }
            }
            Row {TextButton(onClick={showDate=true}){Text("Choose date & time")};TextButton(onClick={onDeadline(null)}){Text("No deadline")}}
            Text(draftSettingsSummary(state),style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
            SettingSwitch("Allow suggestions",state.settings.allowSuggestions){onSettings(state.settings.copy(allowSuggestions=it))}
            SettingSwitch("Approve suggestions first",state.settings.requireApproval){onSettings(state.settings.copy(requireApproval=it))}
            SettingSwitch("Show results before voting",state.settings.showResultsBeforeVoting){onSettings(state.settings.copy(showResultsBeforeVoting=it))}
            SettingSwitch("Allow vote changes",state.settings.allowVoteChange){onSettings(state.settings.copy(allowVoteChange=it))}
            PickleTextField(state.expectedVoters,onExpected,placeholder="Expected voters (optional)",keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            PickleButton("Done",onDismiss,Modifier.fillMaxWidth(),style=PickleButtonStyle.Teal)
            Spacer(Modifier.height(20.dp))
        }
    }
    if(showDate) DatePickerDialog(onDismissRequest={showDate=false},confirmButton={TextButton(onClick={
        datePicker.selectedDateMillis?.let {selectedDate=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate();showDate=false;showTime=true}
    }){Text("Next")}},dismissButton={TextButton(onClick={showDate=false}){Text("Cancel")}}) {DatePicker(datePicker)}
    if(showTime) AlertDialog(onDismissRequest={showTime=false},title={Text("Closing time")},text={Column {
        TimePicker(timePicker)
        if(dateError) Text("Choose a time in the future.",color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton(onClick={
        val instant=selectedDate.atTime(timePicker.hour,timePicker.minute).atZone(zone).toInstant()
        if(instant>Instant.now()){onDeadline(instant);showTime=false;dateError=false}else dateError=true
    }){Text("Set time")}},dismissButton={TextButton(onClick={showTime=false}){Text("Cancel")}})
}

@Composable
private fun SettingSwitch(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(checked,onChange,colors=pickleSwitchColors())}
}
