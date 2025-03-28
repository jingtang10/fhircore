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

package org.smartregister.fhircore.engine.domain.model

import org.junit.Assert
import org.junit.Test

class QuestionnaireTypeTest {

  @Test
  fun testDefault() {
    Assert.assertTrue(QuestionnaireType.DEFAULT.isDefault())
    Assert.assertFalse(QuestionnaireType.EDIT.isDefault())
    Assert.assertFalse(QuestionnaireType.READ_ONLY.isDefault())
  }

  @Test
  fun testEdit() {
    Assert.assertFalse(QuestionnaireType.DEFAULT.isEditable())
    Assert.assertTrue(QuestionnaireType.EDIT.isEditable())
    Assert.assertFalse(QuestionnaireType.READ_ONLY.isEditable())
  }

  @Test
  fun testReadOnly() {
    Assert.assertFalse(QuestionnaireType.DEFAULT.isReadOnly())
    Assert.assertFalse(QuestionnaireType.EDIT.isReadOnly())
    Assert.assertTrue(QuestionnaireType.READ_ONLY.isReadOnly())
  }
}
