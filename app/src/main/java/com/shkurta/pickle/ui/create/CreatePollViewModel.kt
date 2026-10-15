package com.shkurta.pickle.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.data.remote.dto.parseDatabaseInstant
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.*
import com.shkurta.pickle.domain.usecases.poll.*
import com.shkurta.pickle.domain.usecases.notification.RefreshNotificationsUseCase
import com.shkurta.pickle.ui.components.ActionRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

const val MAX_POLL_OPTIONS=10
enum class Deadline(val label:String,val hours:Long?) {
    NONE("No deadline",null), ONE_HOUR("In 1 hour",1), ONE_DAY("1 day",24), THREE_DAYS("3 days",72), ONE_WEEK("1 week",168),
}

data class CreatePollUiState(
    val question:String="", val options:List<String> = List(MIN_POLL_OPTIONS){""},
    val settings:PollSettings=PollSettings(), val deadline:Deadline=Deadline.NONE, val expectedVoters:String="",
    val optionKeys:List<String> = options.map { UUID.randomUUID().toString() },
    val title:String?=null, val templateId:String?=null, val icon:String="film", val history:String?=null,
    val customClosesAt:String?=null, val winnerText:String?=null, val excludeWinner:Boolean=false,
    val runnerUpTexts:Set<String> = emptySet(), val newTexts:Set<String> = emptySet(), val newKeys:Set<String> = emptySet(),
) {
    val filledOptions:Int get()=distinctOptionTexts(options).size
    val isValid:Boolean get()=isValidPollInput(question,options) && options.size<=MAX_POLL_OPTIONS && options.all {it.isNotBlank()} && distinctOptionTexts(options).size==options.size
    val isTemplate:Boolean get()=title!=null
}

@HiltViewModel
class CreatePollViewModel @Inject constructor(
    private val savedStateHandle:SavedStateHandle,
    private val createPollUseCase:CreatePollUseCase,
    private val refreshPolls:RefreshPollsUseCase,
    private val refreshNotifications:RefreshNotificationsUseCase,
    private val templates:TemplateRepository,
    private val polls:PollRepository,
):ViewModel() {
    private val actions=ActionRunner(viewModelScope)
    val actionState=actions.state
    private fun strings(key:String)=savedStateHandle.get<ArrayList<String>>(key)?.toList()
    private val restoredOptions=strings("options")?:List(MIN_POLL_OPTIONS){""}
    private val _uiState=MutableStateFlow(CreatePollUiState(
        question=savedStateHandle["question"]?:"",options=restoredOptions,
        optionKeys=strings("optionKeys")?.takeIf { it.size==restoredOptions.size }?:restoredOptions.map { UUID.randomUUID().toString() },
        settings=PollSettings(
            votingMode=enumOrDefault(savedStateHandle["votingMode"],VotingMode.SINGLE),
            allowSuggestions=savedStateHandle["allowSuggestions"]?:false,
            requireApproval=savedStateHandle["requireApproval"]?:true,
            showResultsBeforeVoting=savedStateHandle["showResultsBeforeVoting"]?:false,
            allowVoteChange=savedStateHandle["allowVoteChange"]?:true),
        deadline=enumOrDefault(savedStateHandle["deadline"],Deadline.NONE),expectedVoters=savedStateHandle["expectedVoters"]?:"",
        title=savedStateHandle["draftTitle"],templateId=savedStateHandle["draftTemplateId"],icon=savedStateHandle["icon"]?:"film",
        history=savedStateHandle["history"],customClosesAt=savedStateHandle["customClosesAt"],winnerText=savedStateHandle["winnerText"],
        excludeWinner=savedStateHandle["excludeWinner"]?:false,runnerUpTexts=strings("runnerUpTexts")?.toSet().orEmpty(),newTexts=strings("newTexts")?.toSet().orEmpty(),newKeys=strings("newKeys")?.toSet().orEmpty(),
    ))
    val uiState=_uiState.asStateFlow()
    private val created=Channel<String>(Channel.BUFFERED)
    val createdPollId=created.receiveAsFlow()
    private var delivered=false

    init {
        persist(uiState.value)
        if(savedStateHandle.get<String>("savedPollId")!=null) retry()
        else if(savedStateHandle.get<Boolean>("draftInitialized")!=true) {
            val starterId=savedStateHandle.get<String>("starterId")
            val templateId=savedStateHandle.get<String>("templateId")
            if(starterId!=null || templateId!=null) actions.run {
                val seed=if(starterId!=null) PollStarter.entries.first { it.id==starterId }.seed() else {
                    templates.refresh(); polls.refresh()
                    templates.templates.value.first { it.id==templateId }.seed(polls.polls.value)
                }
                val exclude=seed.winnerText!=null && seed.options.size>2
                val options=seed.options.filterNot { exclude && it==seed.winnerText }
                persist(CreatePollUiState(question=seed.question,options=options,settings=seed.settings,title=seed.title,templateId=seed.templateId,
                    icon=seed.icon,history=seed.history,customClosesAt=seed.closesAt?.toString(),winnerText=seed.winnerText,excludeWinner=exclude,
                    runnerUpTexts=seed.runnerUpTexts,newTexts=seed.newTexts))
                savedStateHandle["draftInitialized"]=true
            }
        }
    }

    private fun persist(state:CreatePollUiState) {
        savedStateHandle["question"]=state.question; savedStateHandle["options"]=ArrayList(state.options); savedStateHandle["optionKeys"]=ArrayList(state.optionKeys)
        savedStateHandle["allowSuggestions"]=state.settings.allowSuggestions; savedStateHandle["requireApproval"]=state.settings.requireApproval
        savedStateHandle["showResultsBeforeVoting"]=state.settings.showResultsBeforeVoting; savedStateHandle["allowVoteChange"]=state.settings.allowVoteChange
        savedStateHandle["votingMode"]=state.settings.votingMode.name; savedStateHandle["deadline"]=state.deadline.name; savedStateHandle["expectedVoters"]=state.expectedVoters
        savedStateHandle["draftTitle"]=state.title; savedStateHandle["draftTemplateId"]=state.templateId; savedStateHandle["icon"]=state.icon; savedStateHandle["history"]=state.history
        savedStateHandle["customClosesAt"]=state.customClosesAt; savedStateHandle["winnerText"]=state.winnerText; savedStateHandle["excludeWinner"]=state.excludeWinner
        savedStateHandle["runnerUpTexts"]=ArrayList(state.runnerUpTexts); savedStateHandle["newTexts"]=ArrayList(state.newTexts); savedStateHandle["newKeys"]=ArrayList(state.newKeys)
        _uiState.value=state
    }
    private fun update(state:CreatePollUiState) {
        if(actionState.value.busy || savedStateHandle.get<String>("savedPollId")!=null) return
        if(state!=uiState.value) savedStateHandle.remove<String>("requestId")
        persist(state)
    }
    fun setQuestion(value:String)=update(uiState.value.copy(question=value))
    fun setOption(index:Int,value:String) {
        if(index !in uiState.value.options.indices) return
        update(uiState.value.copy(options=uiState.value.options.mapIndexed { i,text -> if(i==index) value else text }))
    }
    fun addOption() {
        val state=uiState.value
        if(state.options.size>=MAX_POLL_OPTIONS) return
        val key=UUID.randomUUID().toString()
        update(state.copy(options=state.options+"",optionKeys=state.optionKeys+key,newKeys=state.newKeys+key))
    }
    fun removeOption(index:Int) {
        val state=uiState.value
        if(state.options.size<=MIN_POLL_OPTIONS || index !in state.options.indices) return
        update(state.copy(options=state.options.filterIndexed { i,_->i!=index },optionKeys=state.optionKeys.filterIndexed { i,_->i!=index }))
    }
    fun moveOption(from:Int,to:Int) {
        val state=uiState.value
        if(from !in state.options.indices || to !in state.options.indices || from==to) return
        val texts=state.options.toMutableList();texts.add(to,texts.removeAt(from))
        val keys=state.optionKeys.toMutableList();keys.add(to,keys.removeAt(from))
        update(state.copy(options=texts,optionKeys=keys))
    }
    fun setExcludeWinner(exclude:Boolean) {
        val state=uiState.value;val winner=state.winnerText?:return
        if(exclude==state.excludeWinner) return
        if(exclude) {
            val kept=state.options.indices.filter { state.options[it]!=winner }
            update(state.copy(options=kept.map { state.options[it] },optionKeys=kept.map { state.optionKeys[it] },excludeWinner=true))
        } else if(state.options.size<MAX_POLL_OPTIONS) update(state.copy(options=state.options+winner,optionKeys=state.optionKeys+UUID.randomUUID().toString(),excludeWinner=false))
    }
    fun setSettings(value:PollSettings)=update(uiState.value.copy(settings=value))
    fun setDeadline(value:Deadline)=update(uiState.value.copy(deadline=value,customClosesAt=null))
    fun setCustomDeadline(value:Instant?)=update(uiState.value.copy(customClosesAt=value?.toString(),deadline=Deadline.NONE))
    fun setExpectedVoters(value:String)=update(uiState.value.copy(expectedVoters=value.filter(Char::isDigit).take(4)))
    private suspend fun deliver(id:String) { if(!delivered) { delivered=true;created.send(id) } }
    fun retry() {
        if(delivered) return
        val id=savedStateHandle.get<String>("savedPollId")
        if(id!=null) actions.run { refreshPolls();refreshNotifications();deliver(id) }
        else if(uiState.value.isValid) createPoll() else actions.retry { }
    }
    fun createPoll() {
        if(delivered || actionState.value.busy) return
        if(savedStateHandle.get<String>("savedPollId")!=null) {retry();return}
        val state=uiState.value;if(!state.isValid) return
        val request=savedStateHandle.get<String>("requestId")?:UUID.randomUUID().toString().also {savedStateHandle["requestId"]=it}
        actions.run {
            val poll=try {createPollUseCase(state.question,state.options,state.settings,
                state.customClosesAt?.let(::parseDatabaseInstant)?:state.deadline.hours?.let {Instant.now().plusSeconds(it*3600)},state.expectedVoters.toIntOrNull(),request,state.templateId)
            } catch(e:SavedRefreshException) {savedStateHandle["savedPollId"]=e.savedId;throw e}
            if(poll!=null) {savedStateHandle["savedPollId"]=poll.id;deliver(poll.id)}
        }
    }
}
