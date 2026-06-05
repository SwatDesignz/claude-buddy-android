package com.example

import com.example.data.db.PetEntity
import com.example.data.model.lifecycle
import org.junit.Assert.*
import org.junit.Test

class PetLifecycleTest {

  private fun makePet(level: Int) = PetEntity(
    name = "TestBuddy",
    species = "Owl",
    rarity = "Common",
    isShiny = false,
    level = level,
    debugging = 50,
    patience = 50,
    chaos = 50,
    wisdom = 50,
    snark = 50
  )

  @Test
  fun `level 1 to 9 is Baby`() {
    for (lv in 1..9) {
      assertEquals("Baby", makePet(lv).lifecycle)
    }
  }

  @Test
  fun `level 10 to 19 is Teen`() {
    for (lv in 10..19) {
      assertEquals("Teen", makePet(lv).lifecycle)
    }
  }

  @Test
  fun `level 20 and above is Adult`() {
    for (lv in 20..30) {
      assertEquals("Adult", makePet(lv).lifecycle)
    }
  }
}
