package com.shkurta.pickle.ui

import androidx.lifecycle.SavedStateHandle
import com.shkurta.pickle.domain.usecases.auth.*
import com.shkurta.pickle.testing.*
import com.shkurta.pickle.ui.auth.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val auth = FakeAuthRepository()
    private fun login(handle: SavedStateHandle = SavedStateHandle()) = LoginViewModel(handle, SignInUseCase(auth))
    private fun register(handle: SavedStateHandle = SavedStateHandle()) = RegisterViewModel(handle, SignUpUseCase(auth))
    private fun fill(vm: RegisterViewModel) {
        vm.setName(" Alex "); vm.setEmail(" alex@example.com "); vm.setPassword("secret")
    }

    @Test fun signupPassesSupportedEmailLanguageWithEnglishFallback() = runTest {
        mapOf("el" to "el", "el-GR" to "el", "el_CY" to "el", "en" to "en", "de-DE" to "en", "" to "en").forEach { (language, expected) ->
            val vm = register()
            fill(vm)
            vm.nextStep()
            vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
            vm.submit(language)
            runCurrent()
            assertEquals(language, expected, auth.lastLanguage)
            assertEquals(AuthStatus.Success, vm.uiState.value.status)
        }
    }

    @Test fun loginValidatesOnBlurAndAttemptThenTrimsAndRetries() = runTest {
        val vm = login()
        assertFalse(vm.uiState.value.emailError)
        vm.touch(AuthField.Email)
        assertTrue(vm.uiState.value.emailError)
        vm.setEmail("a@")
        vm.submit()
        assertTrue(vm.uiState.value.passwordError)
        assertEquals(0, auth.signInCalls)
        vm.setEmail(" alex@example.com "); vm.setPassword("secret")
        auth.result = Result.failure(IOException("private backend error"))
        vm.submit()
        assertTrue(vm.uiState.value.isLoading)
        runCurrent()
        assertEquals("alex@example.com", auth.lastEmail)
        assertEquals(AuthStatus.Error, vm.uiState.value.status)
        vm.touch(AuthField.Email)
        assertEquals(AuthStatus.Error, vm.uiState.value.status)
        auth.result = Result.success(Unit)
        vm.submit(); runCurrent()
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
    }

    @Test fun registrationRequiresDetailsThenAvatarAndPreservesDetailsOnBack() = runTest {
        val vm = register()
        vm.nextStep()
        assertEquals(RegistrationStep.Details, vm.uiState.value.step)
        assertTrue(vm.uiState.value.nameError)
        fill(vm)
        vm.setPassword("short"); vm.nextStep()
        assertTrue(vm.uiState.value.passwordError)
        vm.setPassword("secret"); vm.nextStep()
        assertEquals(RegistrationStep.Avatar, vm.uiState.value.step)
        vm.submit()
        assertTrue(vm.uiState.value.avatarAttempted)
        assertEquals(0, auth.signUpCalls)
        val seed = vm.uiState.value.avatarSeeds.first()
        vm.selectAvatar(seed)
        vm.previousStep()
        assertEquals(" Alex ", vm.uiState.value.name)
        assertEquals("secret", vm.uiState.value.password)
        vm.nextStep(); vm.submit(); runCurrent()
        assertEquals("Alex", auth.lastName)
        assertEquals("alex@example.com", auth.lastEmail)
        assertEquals(DiceBear.avatarUrl(seed), auth.lastAvatar)
        assertEquals("en", auth.lastLanguage)
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
    }

    @Test fun restoreKeepsNonSensitiveDetailsAndReturnsToDetailsWithoutPassword() {
        val handle = SavedStateHandle()
        val vm = register(handle)
        fill(vm); vm.nextStep(); vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
        val restored = register(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(RegistrationStep.Details, restored.uiState.value.step)
        assertEquals(vm.uiState.value.name, restored.uiState.value.name)
        assertEquals(vm.uiState.value.email, restored.uiState.value.email)
        assertEquals(vm.uiState.value.avatarSeeds, restored.uiState.value.avatarSeeds)
        assertEquals(vm.uiState.value.selectedSeed, restored.uiState.value.selectedSeed)
        assertEquals("", restored.uiState.value.password)
        assertFalse(handle.keys().contains("password"))
        val loginHandle = SavedStateHandle()
        val login = login(loginHandle)
        login.setEmail("a@example.com"); login.setPassword("secret")
        assertEquals("", login(loginHandle).uiState.value.password)
        assertFalse(loginHandle.keys().contains("password"))
    }

    @Test fun pendingRequestsBlockDuplicateSubmissionAndFormMutation() = runTest {
        auth.gate = CompletableDeferred()
        val vm = register()
        fill(vm); vm.nextStep(); vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
        vm.submit()
        val pending = vm.uiState.value
        vm.submit(); vm.setName("Other"); vm.setEmail("other@example.com"); vm.setPassword("otherpass")
        vm.shuffleAvatars(); vm.previousStep(); vm.nextStep(); vm.selectAvatar(pending.avatarSeeds.last())
        assertEquals(pending, vm.uiState.value)
        runCurrent()
        assertEquals(1, auth.signUpCalls)
        auth.gate!!.complete(Unit); runCurrent()
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
    }

    @Test fun loginLoadingGuardAndRegistrationFailureRetry() = runTest {
        val login = login()
        login.setEmail("a@example.com"); login.setPassword("secret"); login.submit()
        val pending = login.uiState.value
        login.submit(); login.setEmail("other@example.com"); login.setPassword("other")
        assertEquals(pending, login.uiState.value)
        runCurrent(); assertEquals(1, auth.signInCalls)
        val vm = register()
        fill(vm); vm.nextStep(); vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
        auth.failure = IOException("sensitive")
        vm.submit(); runCurrent()
        assertEquals(AuthStatus.Error, vm.uiState.value.status)
        assertEquals(RegistrationStep.Avatar, vm.uiState.value.step)
        auth.failure = null
        vm.submit(); runCurrent()
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
    }

    @Test fun shuffleClearsSelectionAndRejectsUnknownSeeds() {
        val vm = register()
        vm.selectAvatar("unknown")
        assertNull(vm.uiState.value.selectedSeed)
        vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
        val old = vm.uiState.value.avatarSeeds
        vm.shuffleAvatars()
        assertNull(vm.uiState.value.selectedSeed)
        assertNotEquals(old, vm.uiState.value.avatarSeeds)
    }
}

