package com.example

import com.example.data.model.SpeciesData
import org.junit.Assert.*
import org.junit.Test

class SpeciesDataTest {

  @Test
  fun `list contains 19 species`() {
    assertEquals(19, SpeciesData.list.size)
  }

  @Test
  fun `getByName returns Owl by default for unknown name`() {
    val spec = SpeciesData.getByName("nonexistent")
    assertEquals("Owl", spec.name)
  }

  @Test
  fun `getByName is case-insensitive`() {
    assertEquals("Duck", SpeciesData.getByName("duck").name)
    assertEquals("Duck", SpeciesData.getByName("DUCK").name)
    assertEquals("Duck", SpeciesData.getByName("Duck").name)
  }

  @Test
  fun `all species have valid frame1 and frame2`() {
    for (spec in SpeciesData.list) {
      assertTrue("${spec.name} frame1 is empty", spec.frame1.isNotBlank())
      assertTrue("${spec.name} frame2 is empty", spec.frame2.isNotBlank())
      assertNotEquals("${spec.name} frame1 and frame2 should differ", spec.frame1, spec.frame2)
    }
  }

  @Test
  fun `all species have evolution frames for Teen and Adult stages`() {
    for (spec in SpeciesData.list) {
      assertTrue("${spec.name} frame3 (Teen) is empty", spec.frame3.isNotBlank())
      assertTrue("${spec.name} frame4 (Teen) is empty", spec.frame4.isNotBlank())
      assertTrue("${spec.name} frame5 (Adult) is empty", spec.frame5.isNotBlank())
      assertTrue("${spec.name} frame6 (Adult) is empty", spec.frame6.isNotBlank())
      assertNotEquals("${spec.name} frame3 and frame4 should differ", spec.frame3, spec.frame4)
      assertNotEquals("${spec.name} frame5 and frame6 should differ", spec.frame5, spec.frame6)
    }
  }

  @Test
  fun `all species have stats in valid range`() {
    for (spec in SpeciesData.list) {
      assertTrue("${spec.name} baseDebugging out of range", spec.baseDebugging in 5..100)
      assertTrue("${spec.name} basePatience out of range", spec.basePatience in 5..100)
      assertTrue("${spec.name} baseChaos out of range", spec.baseChaos in 5..100)
      assertTrue("${spec.name} baseWisdom out of range", spec.baseWisdom in 5..100)
      assertTrue("${spec.name} baseSnark out of range", spec.baseSnark in 5..100)
    }
  }

  @Test
  fun `all species have unique names`() {
    val names = SpeciesData.list.map { it.name.lowercase() }
    assertEquals(names.toSet().size, names.size)
  }

  @Test
  fun `all species have non-empty descriptions`() {
    for (spec in SpeciesData.list) {
      assertTrue("${spec.name} description is empty", spec.description.isNotBlank())
    }
  }

  @Test
  fun `getByName returns correct species`() {
    val spec = SpeciesData.getByName("Elephant")
    assertEquals("Elephant", spec.name)
    assertEquals(80, spec.baseDebugging)
    assertEquals(95, spec.baseWisdom)
    assertEquals(10, spec.baseSnark)
  }
}
