package com.shkurta.pickle.di

import com.shkurta.pickle.domain.repository.NotificationRepository
import com.shkurta.pickle.domain.repository.PollRepository
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.data.auth.AuthRepositoryImpl
import com.shkurta.pickle.data.remote.SupabaseNotificationRepository
import com.shkurta.pickle.data.remote.SupabasePollRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: SupabaseNotificationRepository,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindPollRepository(
        impl: SupabasePollRepository,
    ): PollRepository
}
