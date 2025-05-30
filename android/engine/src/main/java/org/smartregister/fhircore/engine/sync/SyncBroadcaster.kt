/*
 * Copyright 2021-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

/**
 * This class is used to trigger one time and periodic syncs. A new instance of this class is
 * created each time because a new instance of [ResourceParamsBasedDownloadWorkManager] is needed
 * everytime sync is triggered; this class SHOULD NOT be provided as a singleton. The
 * [SyncJobStatus] events are sent to the registered [OnSyncListener] maintained by the
 * [SyncListenerManager]
 */
class SyncBroadcaster
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val fhirEngine: FhirEngine,
  val syncListenerManager: SyncListenerManager,
  val dispatcherProvider: DispatcherProvider,
  @ApplicationContext val context: Context,
) {

  /**
   * Run one time sync. The [SyncJobStatus] will be broadcast to all the registered [OnSyncListener]
   * 's
   */
  suspend fun runOneTimeSync() = coroutineScope {
    Timber.i("Running one time sync...")
    Sync.oneTimeSync<AppSyncWorker>(context)
    val uniqueWorkName = "${AppSyncWorker::class.java.name}-oneTimeSync"
    handleSyncJobStatus(uniqueWorkName, this)
  }

  /**
   * Schedule periodic sync periodically as defined in the application config interval. The
   * [SyncJobStatus] will be broadcast to all the registered [OnSyncListener]'s
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun schedulePeriodicSync(interval: Long = 15) = coroutineScope {
    Timber.i("Scheduling periodic sync...")
    Sync.periodicSync<AppSyncWorker>(
      context = context,
      periodicSyncConfiguration =
        PeriodicSyncConfiguration(
          syncConstraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
          repeat = RepeatInterval(interval = interval, timeUnit = TimeUnit.MINUTES),
        ),
    )
    val uniqueWorkName = "${AppSyncWorker::class.java.name}-periodicSync"
    handleSyncJobStatus(uniqueWorkName, this)
  }

  // TODO This serves as a workaround to fix issue with getting SyncJobStatus.Finished
  // TODO Refactor once https://github.com/google/android-fhir/pull/2142 is merged
  private fun handleSyncJobStatus(uniqueWorkName: String, coroutineScope: CoroutineScope) {
    WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
      .asFlow()
      .flatMapConcat { it.asFlow() }
      .mapNotNull { it }
      .onEach { workInfo ->
        // PeriodSync doesn't return state. It's enqueued instead. Finish the sync as workaround.
        if (workInfo.state == WorkInfo.State.ENQUEUED) {
          syncListenerManager.onSyncListeners.forEach { onSyncListener ->
            onSyncListener.onSync(SyncJobStatus.Finished())
          }
        } else {
          val data =
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
              workInfo.outputData
            } else workInfo.progress
          data
            .takeIf { it.keyValueMap.isNotEmpty() && it.hasKeyWithValueOfType<String>("StateType") }
            ?.let {
              val state = it.getString("StateType")!!
              val stateData = it.getString("State")
              val syncJobStatus =
                Sync.gson.fromJson(stateData, Class.forName(state)) as SyncJobStatus
              syncListenerManager.onSyncListeners.forEach { onSyncListener ->
                onSyncListener.onSync(syncJobStatus)
              }
            }
        }
      }
      .catch { throwable -> Timber.e("Encountered an error during sync:", throwable) }
      .shareIn(coroutineScope, SharingStarted.Eagerly, 1)
      .launchIn(coroutineScope)
  }
}
