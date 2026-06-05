package com.example

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class RarityColorTest {

  @Test
  fun `rarity color returns expected colors`() {
    assertEquals(Color(0xFFFF8500), getRarityColor("Legendary"))
    assertEquals(Color(0xFFCE00FF), getRarityColor("Epic"))
    assertEquals(Color(0xFF00A2FF), getRarityColor("Rare"))
    assertEquals(Color(0xFFA6FF00), getRarityColor("Uncommon"))
    assertEquals(Color(0xFF90A4AE), getRarityColor("Common"))
  }

  @Test
  fun `rarity color is case-insensitive`() {
    assertEquals(Color(0xFFCE00FF), getRarityColor("EPIC"))
    assertEquals(Color(0xFFCE00FF), getRarityColor("epic"))
    assertEquals(Color(0xFFCE00FF), getRarityColor("Epic"))
  }

  @Test
  fun `unknown rarity defaults to common color`() {
    assertEquals(Color(0xFF90A4AE), getRarityColor("Mythic"))
    assertEquals(Color(0xFF90A4AE), getRarityColor(""))
  }
}
