package io.github.amibhai.liftlog.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.id.RandomUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Singleton

/** What core:domain, which knows nothing about dependency injection, works with. */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideDispatchers() = AppDispatchers(default = Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSequentialWork(@ApplicationScope scope: CoroutineScope) = SequentialWork(scope)

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideIdGenerator(): IdGenerator = RandomUuidGenerator()
}
