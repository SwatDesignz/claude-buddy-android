package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZRepositoryTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: ZRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ZRepository(database.petDao(), database.logDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `createPet deactivates others and activates new pet`() = runBlocking {
    val pet1 = createTestPet("Buddy1")
    val pet2 = createTestPet("Buddy2")

    repository.createPet(pet1)
    repository.createPet(pet2)

    val activePet = repository.getActivePetDirect()
    assertEquals("Buddy2", activePet?.name)
  }

  @Test
  fun `selectPet switches active pet`() = runBlocking {
    val pet1 = createTestPet("Buddy1")
    val id1 = repository.createPet(pet1)

    val pet2 = createTestPet("Buddy2")
    repository.createPet(pet2)

    repository.selectPet(id1.toInt())

    val activePet = repository.getActivePetDirect()
    assertEquals("Buddy1", activePet?.name)
  }

  @Test
  fun `deletePet removes pet from database`() = runBlocking {
    val pet = createTestPet("Buddy")
    repository.createPet(pet)

    val allPets = repository.allPets.first()
    val savedPet = allPets.first()

    repository.deletePet(savedPet)

    val afterDelete = repository.allPets.first()
    assertTrue(afterDelete.isEmpty())
  }

  @Test
  fun `updatePet persists changes`() = runBlocking {
    val pet = createTestPet("Buddy")
    repository.createPet(pet)

    val saved = repository.getActivePetDirect()!!
    val updated = saved.copy(hunger = 75, energy = 30)
    repository.updatePet(updated)

    val reloaded = repository.getActivePetDirect()!!
    assertEquals(75, reloaded.hunger)
    assertEquals(30, reloaded.energy)
  }

  @Test
  fun `addLog and clearLogs work`() = runBlocking {
    repository.addLog("TestPet", "TEST", "Hello, world!")
    repository.addLog("TestPet", "ERROR", "Something broke!")

    val logs = repository.recentLogs.first()
    assertEquals(2, logs.size)
    assertEquals("Something broke!", logs[0].message) // most recent first
    assertEquals("TEST", logs[1].tag)

    repository.clearLogs()
    val afterClear = repository.recentLogs.first()
    assertTrue(afterClear.isEmpty())
  }

  @Test
  fun `allPets returns pets ordered by birthday descending`() = runBlocking {
    val pet1 = createTestPet("First").copy(birthday = 100)
    val pet2 = createTestPet("Second").copy(birthday = 200)
    val pet3 = createTestPet("Third").copy(birthday = 300)

    repository.createPet(pet1)
    repository.createPet(pet2)
    repository.createPet(pet3)

    val allPets = repository.allPets.first()
    assertEquals("Third", allPets[0].name)
    assertEquals("First", allPets[2].name)
  }

  private fun createTestPet(name: String) = PetEntity(
    name = name,
    species = "Owl",
    rarity = "Common",
    isShiny = false,
    debugging = 50,
    patience = 50,
    chaos = 50,
    wisdom = 50,
    snark = 50
  )
}
