package io.github.amibhai.liftlog.core.data.di

import javax.inject.Qualifier

/** A scope that lives as long as the process (for DataStore and work that must outlive a screen). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The dispatcher for blocking I/O. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
