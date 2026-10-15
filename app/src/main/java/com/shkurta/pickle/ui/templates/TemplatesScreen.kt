package com.shkurta.pickle.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.pickle.R
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.*
import com.shkurta.pickle.ui.components.*
import com.shkurta.pickle.ui.theme.Pickle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    val polls: PollRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val actions = ActionRunner(viewModelScope)
    val actionState = actions.state
    val templates = repository.templates
    init { auth.session.onEach { actions.cancel(); if(it.initialized) refresh() }.launchIn(viewModelScope) }
    fun refresh() = actions.run { repository.refresh(); polls.refresh() }
    fun retry() = actions.retry(::refresh)
}

@Composable
fun TemplatesScreen(onBack: () -> Unit, onUse: (String?,String?) -> Unit, viewModel: TemplatesViewModel = hiltViewModel()) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val polls by viewModel.polls.polls.collectAsStateWithLifecycle()
    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action,viewModel::retry) {
        TemplatesContent(templates,polls,onBack,onUse)
    }
}

@Composable
fun TemplatesContent(templates: List<PollTemplate>, polls: List<Poll>, onBack: () -> Unit, onUse: (String?,String?) -> Unit) {
    Scaffold(containerColor=Pickle.colors.canvas, topBar={
        TopAppBar(title={Text("Templates",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)},navigationIcon={CircleIconButton(R.drawable.ic_arrow_back,"Back",onBack)},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=Pickle.colors.canvas))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Same question every Friday? Start from last time.",style=MaterialTheme.typography.bodyMedium,color=Pickle.colors.inkMuted)
            Text("Your templates",style=MaterialTheme.typography.labelLarge,color=Pickle.colors.inkMuted)
            if(templates.isEmpty()) Text("Save one of your polls as a template to start a tradition.",style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
            templates.forEach { template ->
                PickleCard {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(color=Pickle.colors.mustard,shape=MaterialTheme.shapes.medium,border=androidx.compose.foundation.BorderStroke(1.5.dp,Pickle.colors.lineStrong)) { Box(Modifier.padding(10.dp)) { TemplateIcon(template.icon) } }
                        Column(Modifier.weight(1f)) {
                            Text("${template.name}  ↻",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                            Text("Saved from ${template.roundsCount} ${if(template.roundsCount==1) "poll" else "polls"} · ${template.seed(polls).options.size} options${template.schedule?.let { " · ${it.label()}" }.orEmpty()}",style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
                        }
                        PickleButton("Use",{onUse(null,template.id)},height=40.dp,style=PickleButtonStyle.Teal)
                    }
                }
            }
            Text("Starters",style=MaterialTheme.typography.labelLarge,color=Pickle.colors.inkMuted)
            PollStarter.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    pair.forEach { starter ->
                        PickleCard(modifier=Modifier.weight(1f),onClick={onUse(starter.id,null)}) {
                            TemplateIcon(starter.icon)
                            Spacer(Modifier.height(14.dp))
                            Text(starter.title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(starter.subtitle,style=MaterialTheme.typography.bodySmall,color=Pickle.colors.inkMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
