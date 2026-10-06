package com.metro.training.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Lightweight history row — avoids loading full workouts (and their sets) for the history list. */
data class WorkoutSummaryRow(
    val id: String,
    val routineId: String?,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val exerciseCount: Int,
    val workSetCount: Int,
)

@Dao
interface TrainingDao {

    // ---- exercises -------------------------------------------------------

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name")
    suspend fun exercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun exercise(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises")
    suspend fun allExercisesIncludingArchived(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises WHERE builtIn = 0")
    suspend fun customExerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(entity: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(entities: List<ExerciseEntity>)

    @Query("UPDATE exercises SET archived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archiveExercise(id: String, now: Long)

    // ---- routines --------------------------------------------------------

    @Query("SELECT * FROM routines WHERE archived = 0 ORDER BY createdAt")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun routine(id: String): RoutineEntity?

    @Query("SELECT * FROM routine_exercises ORDER BY routineId, `order`")
    suspend fun allRoutineExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routines WHERE archived = 0 ORDER BY createdAt LIMIT 1")
    suspend fun firstRoutine(): RoutineEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY `order`")
    suspend fun routineExercises(routineId: String): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(entity: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercises(entities: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: String)

    @Query("UPDATE routines SET archived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archiveRoutine(id: String, now: Long)

    // ---- workouts --------------------------------------------------------

    @Query("SELECT * FROM workouts WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun inProgressWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeInProgressWorkout(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY startedAt DESC")
    fun observeCompletedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workout(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: String): Flow<WorkoutEntity?>

    /**
     * Completed-workout summaries for the history list. Aggregates in SQL and limits rows so the
     * UI never loads full workouts (or their sets) just to render a list.
     */
    @Query(
        "SELECT w.id AS id, w.routineId AS routineId, w.routineName AS routineName, " +
            "w.startedAt AS startedAt, w.finishedAt AS finishedAt, " +
            "(SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id) AS exerciseCount, " +
            "(SELECT COUNT(*) FROM workout_sets ws " +
            "   INNER JOIN workout_exercises we2 ON we2.id = ws.workoutExerciseId " +
            "   WHERE we2.workoutId = w.id AND ws.completed = 1) AS workSetCount " +
            "FROM workouts w WHERE w.status = 'COMPLETED' " +
            "ORDER BY w.startedAt DESC LIMIT :limit",
    )
    fun observeCompletedWorkoutSummaries(limit: Int): Flow<List<WorkoutSummaryRow>>

    @Query(
        "SELECT w.id AS id, w.routineId AS routineId, w.routineName AS routineName, " +
            "w.startedAt AS startedAt, w.finishedAt AS finishedAt, " +
            "(SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id) AS exerciseCount, " +
            "(SELECT COUNT(*) FROM workout_sets ws " +
            "   INNER JOIN workout_exercises we2 ON we2.id = ws.workoutExerciseId " +
            "   WHERE we2.workoutId = w.id AND ws.completed = 1) AS workSetCount " +
            "FROM workouts w WHERE w.status = 'COMPLETED' " +
            "ORDER BY w.startedAt DESC LIMIT :limit",
    )
    suspend fun completedWorkoutSummaries(limit: Int): List<WorkoutSummaryRow>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order`")
    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY `order`")
    suspend fun workoutExercises(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun workoutExercise(id: String): WorkoutExerciseEntity?

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: String)

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex")
    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex")
    suspend fun sets(workoutExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun set(id: String): WorkoutSetEntity?

    /**
     * Recent completed exposures for one exercise (newest first). The progression repository trims
     * this to a small comparable window.
     */
    @Query(
        "SELECT we.* FROM workout_exercises we " +
            "INNER JOIN workouts w ON w.id = we.workoutId " +
            "WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED' " +
            "ORDER BY w.startedAt DESC LIMIT :limit",
    )
    suspend fun recentExposuresForExercise(exerciseId: String, limit: Int): List<WorkoutExerciseEntity>

    @Query(
        "SELECT we.* FROM workout_exercises we " +
            "INNER JOIN workouts w ON w.id = we.workoutId " +
            "WHERE w.status = 'COMPLETED' AND w.startedAt >= :since",
    )
    suspend fun completedExposuresSince(since: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun allWorkoutExercisesForExercise(exerciseId: String): List<WorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(entity: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutExercises(entities: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(entity: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)

    // ---- recommendations -------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendations(entities: List<ProgressionRecommendationEntity>)

    @Query("SELECT * FROM progression_recommendations WHERE workoutId = :workoutId")
    fun observeRecommendationsForWorkout(workoutId: String): Flow<List<ProgressionRecommendationEntity>>

    @Query("SELECT * FROM progression_recommendations WHERE workoutId = :workoutId ORDER BY rowid")
    suspend fun recommendationsForWorkout(workoutId: String): List<ProgressionRecommendationEntity>

    @Query(
        "SELECT * FROM progression_recommendations WHERE exerciseId = :exerciseId " +
            "ORDER BY generatedAt DESC LIMIT :limit",
    )
    suspend fun recentRecommendationsForExercise(
        exerciseId: String,
        limit: Int,
    ): List<ProgressionRecommendationEntity>

    @Query("DELETE FROM progression_recommendations WHERE workoutId = :workoutId")
    suspend fun deleteRecommendationsForWorkout(workoutId: String)

    @Update
    suspend fun updateRecommendation(entity: ProgressionRecommendationEntity)
}
