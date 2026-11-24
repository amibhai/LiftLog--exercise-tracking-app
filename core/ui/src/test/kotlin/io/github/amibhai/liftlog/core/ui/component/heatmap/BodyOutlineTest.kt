package io.github.amibhai.liftlog.core.ui.component.heatmap

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Muscle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The picture covers every muscle of the taxonomy; a missing one would silently never light up. */
@RunWith(RobolectricTestRunner::class)
class BodyOutlineTest {

    private val shown = (parseOutlines(FRONT_MUSCLES) + parseOutlines(BACK_MUSCLES)).mapNotNull { it.muscle }.toSet()

    @Test
    fun everyMuscleIsOnTheFigure() {
        assertThat(shown).containsExactlyElementsIn(Muscle.entries)
    }

    @Test
    fun musclesAreNamedCorrectlyAndMirrored() {
        val front = parseOutlines(FRONT_MUSCLES)
        assertThat(front.count { it.muscle == Muscle.CHEST }).isEqualTo(2)
        assertThat(front.count { it.muscle == Muscle.NECK }).isEqualTo(1)
        assertThat(parseOutlines(BODY_PARTS).map { it.muscle }).containsExactlyElementsIn(List(8) { null })
    }
}
