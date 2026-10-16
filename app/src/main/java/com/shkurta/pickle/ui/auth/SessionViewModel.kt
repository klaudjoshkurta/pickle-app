package com.shkurta.pickle.ui.auth

import androidx.lifecycle.ViewModel
import com.shkurta.pickle.domain.usecases.auth.ObserveAuthenticationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(observeAuthentication: ObserveAuthenticationUseCase) : ViewModel() {
    val session = observeAuthentication.session
}
