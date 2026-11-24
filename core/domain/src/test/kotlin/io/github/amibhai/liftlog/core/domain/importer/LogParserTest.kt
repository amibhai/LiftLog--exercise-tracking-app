package io.github.amibhai.liftlog.core.domain.importer

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LogParserTest {

    @Test
    fun `hevy exports are read`() {
        val log = parseLog(
            """
            title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe
            "Pull","23 Mar 2026, 13:54","23 Mar 2026, 14:42","Felt good","Lat Pulldown (Cable)",,"Slow",0,"warmup",27.2,12,,,
            "Pull","23 Mar 2026, 13:54","23 Mar 2026, 14:42","Felt good","Lat Pulldown (Cable)",,"Slow",1,"normal",31.8,10,,,8.5
            "Pull","23 Mar 2026, 13:54","23 Mar 2026, 14:42","Felt good","Bicep Curl (Cable)",0,"",0,"failure",25,12,,,
            "Pull","23 Mar 2026, 13:54","23 Mar 2026, 14:42","Felt good","Face Pull",0,"",0,"dropset",15,15,,,
            "Cardio","24 Mar 2026, 08:00","24 Mar 2026, 08:30","","Running",,"",0,"normal",,,5.2,1500,
            """.trimIndent(),
        )!!

        assertThat(log.source).isEqualTo(LogSource.HEVY)
        assertThat(log.unit).isEqualTo(WeightUnit.KG)
        val pull = log.workouts.first()
        assertThat(pull.name).isEqualTo("Pull")
        assertThat(pull.startedAt).isEqualTo(LocalDateTime.of(2026, 3, 23, 13, 54))
        assertThat(pull.endedAt).isEqualTo(LocalDateTime.of(2026, 3, 23, 14, 42))
        assertThat(pull.notes).isEqualTo("Felt good")
        assertThat(pull.exercises.map { it.name })
            .containsExactly("Lat Pulldown (Cable)", "Bicep Curl (Cable)", "Face Pull").inOrder()
        val pulldown = pull.exercises.first()
        assertThat(pulldown.notes).isEqualTo("Slow")
        assertThat(pulldown.sets.map { it.type }).containsExactly(SetType.WARMUP, SetType.WORKING).inOrder()
        assertThat(pulldown.sets.last().weight).isEqualTo(31.8)
        assertThat(pulldown.sets.last().rpe).isEqualTo(8.5)
        assertThat(pull.exercises.map { it.superset }).containsExactly(null, "0", "0").inOrder()
        assertThat(pull.exercises.drop(1).map { it.sets.single().type })
            .containsExactly(SetType.FAILURE, SetType.DROP).inOrder()
        val run = log.workouts.last().exercises.single().sets.single()
        assertThat(run.distance).isEqualTo(5.2)
        assertThat(run.distanceUnit).isEqualTo(DistanceUnit.KILOMETER)
        assertThat(run.seconds).isEqualTo(1500)
        assertThat(run.weight).isNull()
    }

    @Test
    fun `hevy exports in pounds and miles say so`() {
        val log = parseLog(
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type," +
                "weight_lbs,reps,distance_miles,duration_seconds,rpe\n" +
                "Run,\"1 Jan 2025, 09:00\",,,Running,,,0,normal,,,3.1,1800,\n",
        )!!

        assertThat(log.unit).isEqualTo(WeightUnit.LB)
        assertThat(log.workouts.single().exercises.single().sets.single().distanceUnit).isEqualTo(DistanceUnit.MILE)
    }

    @Test
    fun `strong exports are read`() {
        val log = parseLog(
            """
            Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE
            2024-11-02 18:22:13,"Lower, heavy",1h 5m,Squat (Barbell),W,60.0,10,0,0,"","Good day",
            2024-11-02 18:22:13,"Lower, heavy",1h 5m,Squat (Barbell),1,100.0,5.0,0,0,"Belt","Good day",8
            2024-11-02 18:22:13,"Lower, heavy",1h 5m,Squat (Barbell),Rest Timer,0,0,0,180,"","",
            2024-11-02 18:22:13,"Lower, heavy",1h 5m,Squat (Barbell),D,80.0,8,0,0,"","Good day",
            2024-11-02 18:22:13,"Lower, heavy",1h 5m,Plank,1,0,0,0,60,"","Good day",
            2024-11-04,Upper,42:15,Chin Up,1,0,8,0,0,"","",
            """.trimIndent(),
        )!!

        assertThat(log.source).isEqualTo(LogSource.STRONG)
        assertThat(log.unit).isNull()
        val lower = log.workouts.first()
        assertThat(lower.name).isEqualTo("Lower, heavy")
        assertThat(lower.endedAt).isEqualTo(LocalDateTime.of(2024, 11, 2, 19, 27, 13))
        assertThat(lower.notes).isEqualTo("Good day")
        val squat = lower.exercises.first()
        assertThat(squat.notes).isEqualTo("Belt")
        assertThat(squat.sets.map { it.type })
            .containsExactly(SetType.WARMUP, SetType.WORKING, SetType.DROP).inOrder()
        assertThat(squat.sets[1].reps).isEqualTo(5)
        assertThat(lower.exercises.last().sets.single().seconds).isEqualTo(60)
        val upper = log.workouts.last()
        assertThat(upper.startedAt).isEqualTo(LocalDateTime.of(2024, 11, 4, 12, 0))
        assertThat(upper.endedAt).isEqualTo(LocalDateTime.of(2024, 11, 4, 12, 42, 15))
        assertThat(upper.exercises.single().sets.single().weight).isNull()
    }

    @Test
    fun `older strong exports with semicolons and units are read`() {
        val log = parseLog(
            "\"Date\";\"Workout Name\";\"Exercise Name\";\"Set Order\";\"Weight\";\"Weight Unit\";\"Reps\";\"RPE\";" +
                "\"Distance\";\"Distance Unit\";\"Seconds\";\"Notes\";\"Workout Notes\";\"Workout Duration\"\n" +
                "\"2021-03-05 18:22:13\";\"A\";\"Bench Press (Barbell)\";\"1\";\"82,5\";\"kg\";\"8\";\"\";\"\";\"\";" +
                "\"0\";\"\";\"\";\"45m\"\n",
        )!!

        assertThat(log.unit).isEqualTo(WeightUnit.KG)
        val set = log.workouts.single().exercises.single().sets.single()
        assertThat(set.weight).isEqualTo(82.5)
        assertThat(set.reps).isEqualTo(8)
    }

    @Test
    fun `rows without a readable date or an exercise are counted`() {
        val log = parseLog(
            "Date,Workout Name,Exercise Name,Set Order,Weight,Reps\nsoon,A,Squat,1,100,5\n2024-01-01,A,,1,100,5\n" +
                "2024-01-01,A,Squat,1,100,5\n",
        )!!

        assertThat(log.unreadableRows).isEqualTo(2)
        assertThat(log.workouts).hasSize(1)
    }

    @Test
    fun `other files are not recognized`() {
        assertThat(parseLog("a,b\n1,2")).isNull()
        assertThat(parseLog("")).isNull()
        assertThat(parseLog("{\"format\":\"liftlog-backup\"}")).isNull()
    }

    @Test
    fun `the most frequent exercises come first`() {
        val log = parseLog(
            "Date,Workout Name,Exercise Name,Set Order,Weight,Reps\n2024-01-01,A,Squat,1,100,5\n" +
                "2024-01-01,A,Row,1,60,5\n2024-01-03,A,Row,1,60,5\n",
        )!!

        assertThat(log.exerciseNames).containsExactly("Row", "Squat").inOrder()
    }

    @Test
    fun `durations come in many forms`() {
        assertThat(parseLogDuration("1h 5m")).isEqualTo(3_900)
        assertThat(parseLogDuration("52s")).isEqualTo(52)
        assertThat(parseLogDuration("42:15")).isEqualTo(2_535)
        assertThat(parseLogDuration("1:02:03")).isEqualTo(3_723)
        assertThat(parseLogDuration("3600")).isEqualTo(3_600)
        assertThat(parseLogDuration("long")).isNull()
    }

    @Test
    fun `times come in many forms`() {
        val expected = LocalDateTime.of(2025, 3, 8, 17, 29)

        assertThat(parseLogTime("8 Mar 2025, 17:29")).isEqualTo(expected)
        assertThat(parseLogTime("08 Mar 2025, 17:29")).isEqualTo(expected)
        assertThat(parseLogTime("8 Mar 2025, 5:29 PM")).isEqualTo(expected)
        assertThat(parseLogTime("Mar 8, 2025, 5:29 PM")).isEqualTo(expected)
        assertThat(parseLogTime("2025-03-08 17:29")).isEqualTo(expected)
        assertThat(parseLogTime("yesterday")).isNull()
    }
}
