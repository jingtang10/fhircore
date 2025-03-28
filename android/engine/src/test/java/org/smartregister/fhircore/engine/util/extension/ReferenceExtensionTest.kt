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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test

class ReferenceExtensionTest {
  @Test
  fun testExtractIdShouldReturnEmptyStringIfNullReference() {
    val ref = Reference()
    val result = ref.extractId()

    Assert.assertEquals("", result)
  }

  @Test
  fun testExtractIdShouldReturnIdPartOnly() {
    val ref = Reference().apply { reference = "Patient/123456" }
    val result = ref.extractId()

    Assert.assertEquals("123456", result)
  }

  @Test
  fun testStringAsReferenceShouldReturnReference() {
    val ref = "123456"
    val result = ref.asReference(ResourceType.Patient)

    Assert.assertEquals("Patient/123456", result.reference)
  }

  @Test
  fun testExtractTypeShouldReturnResourceType() {
    val ref = Reference().apply { reference = "Patient/123456" }
    val result = ref.extractType()

    Assert.assertEquals(ResourceType.Patient, result)
  }

  @Test
  fun testExtractTypeShouldReturnNull() {
    val ref = Reference()
    val result = ref.extractType()

    Assert.assertEquals(null, result)
  }
}
