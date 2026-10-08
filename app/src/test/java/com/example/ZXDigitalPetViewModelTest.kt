package com.example

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import com.example.ui.viewmodel.BugGame
import com.example.ui.viewmodel.ZXDigitalPetView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZXDigitalPetViewModelTest {

  @Test
  fun `bug game tick preserves a squish made during the previous tick`() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val viewModelStore = ViewModelStore()

    try {
      val repository = ZRepository(database.petDao(), database.logDao(), database.achievementDao())
      repository.createPet(
        PetEntity(
          name = "TestBuddy",
          species = "Owl",
          rarity = "Common",
          isShiny = false,
          debugging = 50,
          patience = 50,
          chaos = 50,
          wisdom = 50,
          snark = 50
        )
      )
      val viewModel = ZXDigitalPetView(repository)
      viewModelStore.put("viewModel", viewModel)
      val activePetJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.activePet.collect()
      }
      runCurrent()

      viewModel.startBugGame()
      runCurrent()
      advanceTimeBy(1000)
      runCurrent()

      val playing = viewModel.bugGame.value as BugGame.Playing
      val bugIndex = playing.bugs.indexOfFirst { it }
      assertTrue("The game should have spawned a bug", bugIndex >= 0)
      viewModel.squishBug(bugIndex)
      advanceTimeBy(1000)
      runCurrent()

      assertEquals(1, (viewModel.bugGame.value as BugGame.Playing).score)
      activePetJob.cancel()
    } finally {
      viewModelStore.clear()
      database.close()
      Dispatchers.resetMain()
    }
  }
}
