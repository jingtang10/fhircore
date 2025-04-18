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

package org.smartregister.fhircore.quest.integration.ui.report.measure.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPagingSource
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportListScreen
import org.smartregister.fhircore.quest.ui.report.measure.screens.PLEASE_WAIT_TEST_TAG
import org.smartregister.fhircore.quest.ui.report.measure.screens.SHOW_PROGRESS_INDICATOR_TAG

class MeasureReportListScreenTest {

  @get:Rule(order = 0) val composeTestRule = createComposeRule()
  private val navController: NavController = mockk(relaxUnitFun = true)
  private val dataList =
    Pager(PagingConfig(10)) {
        MeasureReportPagingSource(
          measureReportConfiguration = mockk(),
          registerConfiguration = mockk(),
          registerRepository = mockk(),
          resourceDataRulesExecutor = mockk(),
        )
      }
      .flow

  @Test
  fun testMeasureReportListScreenShowsCorrectTitle() {
    composeTestRule.setContent {
      MeasureReportListScreen(
        navController = navController,
        dataList = dataList,
        onReportMeasureClicked = {},
      )
    }

    composeTestRule.onNodeWithText("Reports").assertExists().assertIsDisplayed()
  }

  @Test
  fun testMeasureReportListScreenDisplaysProgressIndicatorWhenShowProgressIndicatorIsTrue() {
    composeTestRule.setContent {
      MeasureReportListScreen(
        navController = navController,
        dataList = dataList,
        onReportMeasureClicked = {},
        showProgressIndicator = true,
      )
    }

    composeTestRule
      .onNodeWithTag(SHOW_PROGRESS_INDICATOR_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag(PLEASE_WAIT_TEST_TAG, useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()

    composeTestRule.onNodeWithText("Please wait…").assertExists().assertIsDisplayed()
  }
}
