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

package org.smartregister.fhircore.engine.task

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.referenceValue

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-11-2022. */
@HiltAndroidTest
class FhirTaskExpireWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()
  private val fhirEngine: FhirEngine = mockk(relaxed = true)
  private val defaultRepository: DefaultRepository = mockk(relaxed = true)
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @BindValue
  var fhirResourceUtil: FhirResourceUtil =
    FhirResourceUtil(
      ApplicationProvider.getApplicationContext(),
      defaultRepository,
      configurationRegistry,
    )
  private lateinit var fhirTaskExpireWorker: FhirTaskExpireWorker
  private lateinit var tasks: List<SearchResult<Task>>

  @Before
  fun setup() {
    hiltRule.inject()
    initializeWorkManager()
    fhirTaskExpireWorker =
      TestListenableWorkerBuilder<FhirTaskExpireWorker>(ApplicationProvider.getApplicationContext())
        .setWorkerFactory(FhirTaskExpireJobWorkerFactory())
        .build()

    tasks =
      listOf(
        SearchResult(
          resource =
            Task().apply {
              id = UUID.randomUUID().toString()
              status = Task.TaskStatus.READY
              executionPeriod =
                Period().apply {
                  start = Date().plusMonths(-1)
                  end = Date()
                }
              restriction =
                Task.TaskRestrictionComponent().apply {
                  period = Period().apply { end = DateTime().plusDays(-2).toDate() }
                }
            },
          null,
          null,
        ),
      )

    coEvery { defaultRepository.fhirEngine } returns fhirEngine
    coEvery { fhirEngine.search<Task>(any()) } returns tasks
  }

  @Test
  fun doWorkShouldFetchTasksAndMarkAsExpired() {
    val result = runBlocking { fhirTaskExpireWorker.doWork() }

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `FhirTaskExpireWorker doWork task expires when past end no reference to careplan`() {
    coEvery { defaultRepository.update(any()) } just runs
    val result = fhirTaskExpireWorker.startWork().get()
    coVerify { defaultRepository.update(any()) }
    assertEquals(result, (ListenableWorker.Result.success()))
    assertEquals(Task.TaskStatus.CANCELLED, tasks.first().resource.status)
  }

  @Test
  fun `FhirTaskExpireWorker doWork task expires when past end found CarePlan was not found`() {
    tasks
      .map { it.resource }
      .forEach { it.basedOn = listOf(Reference().apply { reference = "CarePlan/123" }) }

    coEvery { defaultRepository.update(any()) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, any()) } throws IllegalArgumentException()
    val result = fhirTaskExpireWorker.startWork().get()
    coVerify { defaultRepository.update(any()) }
    assertEquals(result, (ListenableWorker.Result.success()))
    assertEquals(Task.TaskStatus.CANCELLED, tasks.first().resource.status)
  }

  @Test
  fun `FhirTaskExpireWorker doWork task expires when past end found CarePlan no reference to current task ID`() {
    val carePlanId = "123"
    val carePlan =
      CarePlan().apply {
        id = carePlanId
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference = listOf(tasks.first().resource.asReference())
            },
          )
        status = CarePlan.CarePlanStatus.ACTIVE
      }
    tasks
      .map { it.resource }
      .forEach { it.addBasedOn(Reference().apply { reference = carePlan.referenceValue() }) }
    coEvery { defaultRepository.update(any()) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    val result = fhirTaskExpireWorker.startWork().get()
    coVerify { defaultRepository.update(any()) }
    assertEquals(result, (ListenableWorker.Result.success()))
    assertEquals(Task.TaskStatus.CANCELLED, tasks.first().resource.status)
    assertNotEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status)
  }

  @Test
  fun `FhirTaskExpireWorker doWork task expires when past end found CarePlan and this is last task`() {
    val carePlanId = "123"
    val carePlan =
      CarePlan().apply {
        id = carePlanId
        activity =
          listOf(
            CarePlan.CarePlanActivityComponent().apply {
              outcomeReference =
                listOf(Reference().apply { reference = tasks.first().resource.referenceValue() })
            },
          )
      }
    tasks.map { it.resource }.forEach { it.basedOn = listOf(carePlan.asReference()) }
    val resource = tasks.map { it.resource }.first()
    coEvery { defaultRepository.update(resource) } just runs
    coEvery { fhirEngine.get(ResourceType.CarePlan, carePlanId) } returns carePlan
    coEvery { defaultRepository.update(carePlan) } just runs
    val result = fhirTaskExpireWorker.startWork().get()
    coVerify { fhirEngine.get(ResourceType.CarePlan, carePlanId) }
    coVerify { defaultRepository.update(carePlan) }
    coVerify { defaultRepository.update(resource) }
    assertEquals(result, (ListenableWorker.Result.success()))
    assertEquals(Task.TaskStatus.CANCELLED, tasks.first().resource.status)
    assertEquals(CarePlan.CarePlanStatus.COMPLETED, carePlan.status)
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(
      ApplicationProvider.getApplicationContext(),
      config,
    )
  }

  inner class FhirTaskExpireJobWorkerFactory : WorkerFactory() {
    override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
    ): ListenableWorker {
      return FhirTaskExpireWorker(
        context = appContext,
        workerParams = workerParameters,
        defaultRepository = defaultRepository,
        fhirResourceUtil = fhirResourceUtil,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider,
      )
    }
  }
}
