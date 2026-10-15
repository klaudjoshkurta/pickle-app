package com.shkurta.pickle.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.pickle.R
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.ui.components.*
import com.shkurta.pickle.ui.theme.Pickle
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onPollClick: (String)->Unit = {}, onCreateClick: ()->Unit = {},
    onNotificationsClick: ()->Unit = {}, onMenuClick: ()->Unit = {},
    onTemplatesClick: ()->Unit = {}, onStarterClick: (String)->Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action,viewModel::retry) {
        HomeContent(state,{ if(it==HomeFilter.TEMPLATES) onTemplatesClick() else viewModel.selectFilter(it) },onPollClick,onCreateClick,onMenuClick,onStarterClick,loadError=action.message)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun HomeContent(state: HomeUiState,onFilterChange:(HomeFilter)->Unit,onPollClick:(String)->Unit,onCreateClick:()->Unit,onMenuClick:()->Unit,onStarterClick:(String)->Unit,loadError:String?=null) {
    val firstRun=state.hasLoaded && !state.hasAnyPolls
    val context=LocalContext.current
    Scaffold(containerColor=Pickle.colors.canvas,topBar={
        HomeTopBar(avatarUrl=state.avatarUrl,displayName=state.displayName,hasUnreadNotifications=state.hasUnreadNotifications,
            showCreate=!firstRun,onCreateClick=onCreateClick,onMenuClick=onMenuClick)
    },bottomBar={
        if(firstRun) PickleButton("Start your first poll",onCreateClick,Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=20.dp,vertical=16.dp),
            style=PickleButtonStyle.Teal,leadingIcon=R.drawable.ic_add)
    }) { padding ->
        if(!state.hasLoaded) {
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center) { if(loadError!=null) Text("Your polls couldn’t be loaded. Use Retry below.",modifier=Modifier.padding(24.dp),color=Pickle.colors.inkMuted,textAlign=TextAlign.Center) else CircularProgressIndicator(color=Pickle.colors.teal) }
        } else LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(state.isDemo) item { Text("Offline demo · Sign in to use live polls",style=MaterialTheme.typography.labelSmall,color=Pickle.colors.inkMuted) }
            if(firstRun) {
                item {
                    val first=state.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
                    Text("${if(first==null) "Hi there." else "Hi $first."} Your first poll is three taps away.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,color=Pickle.colors.ink)
                }
                item { FirstPollSteps() }
                item { Text("Start from a template",style=MaterialTheme.typography.labelLarge,color=Pickle.colors.inkMuted) }
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        for(pair in listOf(listOf(PollStarter.PIZZA,PollStarter.FILM),listOf(PollStarter.DATE,PollStarter.PLACE))) {
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                pair.forEach { starter -> StarterChip(starter, {onStarterClick(starter.id)}) }
                            }
                        }
                        PickleButton("Blank poll",onCreateClick,style=PickleButtonStyle.Outline,leadingIcon=R.drawable.ic_add,height=38.dp)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().dashedBorder(Pickle.colors.lineStrong,18.dp).padding(vertical=22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("✧",style=MaterialTheme.typography.headlineSmall,color=Pickle.colors.inkMuted)
                        Text("Your polls will live here",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=Pickle.colors.inkMuted)
                        Text("Live ones float to the top with a pulse.",style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
                    }
                }
            } else {
                item { PickleHeadline(text=greeting(state.displayName),style=MaterialTheme.typography.headlineSmall) }
                item { SegmentedToggle(options=HomeFilter.entries,selected=state.filter,onSelect=onFilterChange,label={it.label},modifier=Modifier.horizontalScroll(rememberScrollState())) }
                if(state.filter==HomeFilter.MY_POLLS) {
                    if(state.polls.isNotEmpty()) item { HomeSectionLabel("Live now") }
                    items(state.polls,key={it.id}) { poll -> PollCard(poll,showSuggestionBadge=!state.isDemo,onClick={onPollClick(poll.id)},onShareClick={if(!state.isDemo) sharePoll(context,poll)}) }
                    if(state.polls.isEmpty()) item { Text("No live polls right now. Start a new one or use a template.",color=Pickle.colors.inkMuted,style=MaterialTheme.typography.bodyMedium) }
                    if(state.settledPolls.isNotEmpty()) {
                        item { HomeSectionLabel("Settled") }
                        item { SettledPolls(state.settledPolls,onPollClick) }
                    }
                } else {
                    item { HomeSectionLabel("Settled") }
                    if(state.polls.isEmpty()) item { Text("Nothing settled yet. Closed polls and their winners land here.",color=Pickle.colors.inkMuted) }
                    else item { SettledPolls(state.polls,onPollClick) }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionLabel(text:String) { Text(text,style=MaterialTheme.typography.labelLarge,color=Pickle.colors.inkMuted) }

@Composable
private fun FirstPollSteps() {
    PickleCard {
        Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
            listOf("Ask the question" to "Pizza? Film? Where to meet?", "Add 2–10 options" to "Or let a template fill them in", "Share the link" to "Friends vote — no sign-up, no app").forEachIndexed { index,pair ->
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(color=if(index==0) Pickle.colors.mustard else Pickle.colors.surface,shape=CircleShape,border=BorderStroke(1.5.dp,Pickle.colors.lineStrong)) {
                        Box(Modifier.size(30.dp),contentAlignment=Alignment.Center) { Text("${index+1}",fontWeight=FontWeight.Bold) }
                    }
                    Column { Text(pair.first,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold); Text(pair.second,style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted) }
                }
            }
        }
    }
}

@Composable
private fun StarterChip(starter:PollStarter,onClick:()->Unit) {
    Surface(onClick=onClick,shape=CircleShape,color=Pickle.colors.surface,border=BorderStroke(1.5.dp,Pickle.colors.lineStrong)) {
        Row(Modifier.padding(horizontal=13.dp,vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            TemplateIcon(starter.icon,Modifier.size(14.dp))
            Text(if(starter==PollStarter.PIZZA) "Pizza night" else starter.title,style=MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SettledPolls(polls:List<Poll>,onClick:(String)->Unit) {
    PickleCard(contentPadding=PaddingValues(vertical=6.dp)) {
        polls.forEach { poll ->
            Surface(onClick={onClick(poll.id)},color=Pickle.colors.surface) {
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(color=Pickle.colors.mustard,shape=CircleShape,border=BorderStroke(1.5.dp,Pickle.colors.lineStrong)) {
                        Box(Modifier.size(34.dp),contentAlignment=Alignment.Center) { Icon(painterResource(if(poll.toResults().outcome is Outcome.Winner) R.drawable.ic_trophy else R.drawable.ic_tie),null,Modifier.size(18.dp),tint=Pickle.colors.ink) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(poll.question,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                        val summary=when(val result=poll.toResults().outcome) {
                            is Outcome.Winner -> "${result.option.text} · ${result.option.voteCount} of ${poll.totalVotes} ${if(poll.settings.votingMode==VotingMode.MULTIPLE) "voters" else "votes"}"
                            is Outcome.Tie -> "Tie · ${result.options.joinToString(" & ") { it.text }}"
                            Outcome.NoVotes -> "Nobody voted"
                        }
                        Text(summary,style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
                    }
                    Text((poll.closedAt?:poll.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE")),style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(modifier:Modifier=Modifier,avatarUrl:String?=null,displayName:String?=null,hasUnreadNotifications:Boolean=false,showCreate:Boolean=true,onCreateClick:()->Unit={},onNotificationsClick:()->Unit={},onMenuClick:()->Unit={}) {
    TopAppBar(modifier=modifier,title={PickleBrand()},actions={
        if(showCreate) PickleButton("New poll",onCreateClick,style=PickleButtonStyle.Outline,leadingIcon=R.drawable.ic_add,height=38.dp)
        IconButton(onClick=onMenuClick) {
            BadgedBox(badge={if(hasUnreadNotifications) Badge(containerColor=Pickle.colors.tomato)}) { Avatar(url=avatarUrl,fallbackInitial=displayName,size=34.dp) }
        }
        Spacer(Modifier.width(8.dp))
    },colors=TopAppBarDefaults.topAppBarColors(containerColor=Pickle.colors.canvas))
}

private fun greeting(name:String?):String {
    val time=when(LocalTime.now().hour) { in 5..11 -> "Morning"; in 12..17 -> "Afternoon"; else -> "Evening" }
    val first=name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    return if(first==null) "Your polls" else "$time, $first"
}
