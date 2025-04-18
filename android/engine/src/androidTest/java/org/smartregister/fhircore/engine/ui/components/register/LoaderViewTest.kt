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

package org.smartregister.fhircore.engine.ui.components.register

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R

class LoaderViewTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testLoaderDialogViewSyncingDown() {
    composeRule.setContent { LoaderDialog() }
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_BAR_TAG).assertExists()
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_BAR_TAG).assertIsDisplayed()

    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG).assertExists()
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG).assertIsDisplayed()
    composeRule
      .onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<Application>().getString(R.string.syncing_down),
      )
  }

  @Test
  fun testLoaderDialogViewSyncingUp() {
    composeRule.setContent { LoaderDialog(isSyncUploadFlow = flowOf(true)) }
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_BAR_TAG).assertExists()
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_BAR_TAG).assertIsDisplayed()

    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG).assertExists()
    composeRule.onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG).assertIsDisplayed()
    composeRule
      .onNodeWithTag(LOADER_DIALOG_PROGRESS_MSG_TAG)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<Application>().getString(R.string.syncing_up),
      )
  }
}
