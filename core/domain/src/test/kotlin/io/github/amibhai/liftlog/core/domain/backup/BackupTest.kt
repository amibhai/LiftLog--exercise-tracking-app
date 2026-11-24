package io.github.amibhai.liftlog.core.domain.backup

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.csv.parseCsv
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.NOW
import io.github.amibhai.liftlog.core.domain.workout.exercise
import io.github.amibhai.liftlog.core.domain.workout.set
import io.github.amibhai.liftlog.core.domain.workout.values
import io.github.amibhai.liftlog.core.domain.workout.workout
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class BackupTest {

    @Test
    fun `only the newest backups stay, and other files are never touched`() {
        val names = listOf(
            "liftlog-backup-2026-11-22-080000.json",
            "holiday.jpg",
            "liftlog-backup-2026-11-24-080000.json",
            "liftlog-backup-2026-11-23-080000.json",
            "liftlog-backup-notes.txt",
        )

        assertThat(backupsToDelete(names, keep = 2)).containsExactly("liftlog-backup-2026-11-22-080000.json")
        assertThat(backupsToDelete(names, keep = 5)).isEmpty()
    }

    @Test
    fun `backup names sort by time`() {
        assertThat(backupFileName(NOW, ZoneOffset.UTC)).isEqualTo("liftlog-backup-2023-11-14-221320.json")
    }

    @Test
    fun `the next automatic backup follows one period after the last one`() {
        val daily = BackupSettings(folderUri = "content://folder")
        val day = TimeUnit.DAYS.toMillis(1)

        assertThat(daily.nextBackupIn(NOW)).isEqualTo(day)
        assertThat(daily.copy(lastBackupAt = NOW - 1_000).nextBackupIn(NOW)).isEqualTo(day - 1_000)
        assertThat(daily.copy(lastBackupAt = NOW - 3 * day).nextBackupIn(NOW)).isEqualTo(0)
        assertThat(daily.copy(frequency = BackupFrequency.WEEKLY).nextBackupIn(NOW)).isEqualTo(7 * day)
        assertThat(daily.copy(frequency = BackupFrequency.OFF).nextBackupIn(NOW)).isNull()
        assertThat(BackupSettings().nextBackupIn(NOW)).isNull()
    }

    @Test
    fun `backups run by themselves once there is a folder`() {
        assertThat(BackupSettings().isAutomatic).isFalse()
        assertThat(BackupSettings(folderUri = "content://folder").isAutomatic).isTrue()
        assertThat(BackupSettings(folderUri = "x", frequency = BackupFrequency.OFF).isAutomatic).isFalse()
    }

    @Test
    fun `every completed set is a row`() {
        val workout = workout(
            exercise(
                "bench",
                set("w", SetType.WARMUP, target = values(20.0, 10), done = true),
                set("1", target = values(82.5, 8), done = true).copy(rpe = 8.5),
                set("2", target = values(82.5, 8)),
                group = 1,
            ).copy(notes = "Paused, slow"),
            exercise("row", set("3", target = values(60.0, 10), done = true), group = 1),
        ).copy(name = "Push \"A\"", endedAt = NOW, isInProgress = false)

        val table = parseCsv(setsCsv(listOf(workout), WeightUnit.KG, ZoneOffset.UTC))

        assertThat(table.rows).hasSize(3)
        val set = table.rows[1]
        assertThat(set["Workout"]).isEqualTo("Push \"A\"")
        assertThat(set["Started"]).isEqualTo("2023-11-14 22:12:20")
        assertThat(set["Exercise"]).isEqualTo("bench")
        assertThat(set["Superset"]).isEqualTo("A")
        assertThat(set["Set"]).isEqualTo("2")
        assertThat(set["Type"]).isEqualTo("working")
        assertThat(set["Weight (kg)"]).isEqualTo("82.5")
        assertThat(set["Reps"]).isEqualTo("8")
        assertThat(set["RPE"]).isEqualTo("8.5")
        assertThat(set["Exercise notes"]).isEqualTo("Paused, slow")
        assertThat(table.rows[0]["Type"]).isEqualTo("warmup")
    }

    @Test
    fun `weights are written in the unit of the user`() {
        val workout = workout(exercise("bench", set("1", target = values(100.0, 5), done = true)))

        val table = parseCsv(setsCsv(listOf(workout), WeightUnit.LB, ZoneOffset.UTC))

        assertThat(table.rows.single()["Weight (lb)"]).isEqualTo("220.46")
    }
}
