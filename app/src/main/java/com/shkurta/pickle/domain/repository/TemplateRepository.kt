package com.shkurta.pickle.domain.repository

import com.shkurta.pickle.domain.model.PollTemplate
import com.shkurta.pickle.domain.model.WeeklySchedule
import kotlinx.coroutines.flow.StateFlow

interface TemplateRepository {
    val templates: StateFlow<List<PollTemplate>>
    suspend fun refresh()
    suspend fun save(pollId: String, name: String, icon: String, schedule: WeeklySchedule?): String
}
