package com.example

import com.example.data.model.CalfRegistration
import com.example.data.repository.AuthRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPinHashing() {
    val hash1 = AuthRepository.hashPin("1234")
    val hash2 = AuthRepository.hashPin("1234")
    val hashDifferent = AuthRepository.hashPin("9999")

    assertEquals(hash1, hash2)
    assertNotEquals(hash1, hashDifferent)
    assertTrue(hash1.isNotEmpty())
  }

  @Test
  fun testStandardBreedsList() {
    val breeds = CalfRegistration.STANDARD_BREEDS
    assertTrue(breeds.contains("Bonsmara"))
    assertTrue(breeds.contains("Brahman"))
    assertTrue(breeds.contains("Nguni"))
    assertTrue(breeds.contains("Angus"))
  }
}

