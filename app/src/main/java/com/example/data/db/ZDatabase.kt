package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val species: String,
    val rarity: String,
    val isShiny: Boolean,
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 50,
    val energy: Int = 100,
    val debugging: Int,
    val patience: Int,
    val chaos: Int,
    val wisdom: Int,
    val snark: Int,
    val birthday: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val petName: String,
    val tag: String, // "SYSTEM" | "COMMAND" | "AI" | "BLE"
    val message: String
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String, // e.g. "first_hatch", "level_10", "shiny_catch"
    val unlockedAt: Long = 0L, // 0 = locked
    val progress: Int = 0,
    val maxProgress: Int = 1 // 1 for single-shot, N for progressive
)

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY birthday DESC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE isActive = 1 LIMIT 1")
    fun getActivePetFlow(): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePetDirect(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Query("UPDATE pets SET isActive = 0")
    suspend fun deactivateAllPets()

    @Query("UPDATE pets SET isActive = 1 WHERE id = :petId")
    suspend fun activatePet(petId: Int)

    @Delete
    suspend fun deletePet(pet: PetEntity)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM logs")
    suspend fun clearLogs()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Query("SELECT COUNT(*) FROM achievements WHERE unlockedAt > 0")
    fun getUnlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievements")
    fun getTotalCount(): Flow<Int>
}

@Database(entities = [PetEntity::class, LogEntity::class, AchievementEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun logDao(): LogDao
    abstract fun achievementDao(): AchievementDao
}

class ZRepository(private val petDao: PetDao, private val logDao: LogDao, private val achievementDao: AchievementDao) {
    val allPets: Flow<List<PetEntity>> = petDao.getAllPets()
    val activePet: Flow<PetEntity?> = petDao.getActivePetFlow()
    val recentLogs: Flow<List<LogEntity>> = logDao.getRecentLogs()

    suspend fun getActivePetDirect(): PetEntity? = petDao.getActivePetDirect()

    suspend fun createPet(pet: PetEntity): Long {
        petDao.deactivateAllPets()
        val newPet = pet.copy(isActive = true)
        return petDao.insertPet(newPet)
    }

    suspend fun updatePet(pet: PetEntity) {
        petDao.updatePet(pet)
    }

    suspend fun selectPet(petId: Int) {
        petDao.deactivateAllPets()
        petDao.activatePet(petId)
    }

    suspend fun deletePet(pet: PetEntity) {
        petDao.deletePet(pet)
    }

    suspend fun addLog(petName: String, tag: String, message: String) {
        logDao.insertLog(LogEntity(petName = petName, tag = tag, message = message))
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    // ── Achievements ──────────────────────────────────────────────────────
    val allAchievements: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    val unlockedCount: Flow<Int> = achievementDao.getUnlockedCount()
    val totalCount: Flow<Int> = achievementDao.getTotalCount()

    suspend fun getAchievement(id: String): AchievementEntity? = achievementDao.getAchievement(id)

    suspend fun upsertAchievement(achievement: AchievementEntity) {
        achievementDao.upsertAchievement(achievement)
    }

    suspend fun unlockAchievement(id: String, progress: Int = 1, maxProgress: Int = 1) {
        val existing = achievementDao.getAchievement(id)
        if (existing == null || existing.unlockedAt == 0L) {
            achievementDao.upsertAchievement(
                AchievementEntity(id = id, unlockedAt = System.currentTimeMillis(), progress = progress, maxProgress = maxProgress)
            )
        }
    }

    suspend fun progressAchievement(id: String, currentProgress: Int, maxProgress: Int = 1) {
        val existing = achievementDao.getAchievement(id)
        if (existing != null && existing.unlockedAt > 0L) return // already unlocked
        achievementDao.upsertAchievement(
            AchievementEntity(
                id = id,
                unlockedAt = if (currentProgress >= maxProgress) System.currentTimeMillis() else 0L,
                progress = currentProgress.coerceAtMost(maxProgress),
                maxProgress = maxProgress
            )
        )
    }
}
