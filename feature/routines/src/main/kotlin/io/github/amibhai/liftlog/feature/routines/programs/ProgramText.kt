package io.github.amibhai.liftlog.feature.routines.programs

import androidx.annotation.StringRes
import io.github.amibhai.liftlog.core.domain.program.TemplateDayName
import io.github.amibhai.liftlog.core.domain.program.TemplateKey
import io.github.amibhai.liftlog.feature.routines.R

@StringRes
internal fun TemplateKey.nameRes(): Int = when (this) {
    TemplateKey.PUSH_PULL_LEGS -> R.string.program_ppl
    TemplateKey.UPPER_LOWER -> R.string.program_upper_lower
    TemplateKey.FULL_BODY -> R.string.program_full_body
    TemplateKey.FIVE_BY_FIVE -> R.string.program_5x5
    TemplateKey.GZCLP -> R.string.program_gzclp
    TemplateKey.FIVE_THREE_ONE -> R.string.program_531
}

@StringRes
internal fun TemplateKey.descriptionRes(): Int = when (this) {
    TemplateKey.PUSH_PULL_LEGS -> R.string.program_ppl_body
    TemplateKey.UPPER_LOWER -> R.string.program_upper_lower_body
    TemplateKey.FULL_BODY -> R.string.program_full_body_body
    TemplateKey.FIVE_BY_FIVE -> R.string.program_5x5_body
    TemplateKey.GZCLP -> R.string.program_gzclp_body
    TemplateKey.FIVE_THREE_ONE -> R.string.program_531_body
}

@StringRes
internal fun TemplateDayName.nameRes(): Int = when (this) {
    TemplateDayName.PUSH -> R.string.program_day_push
    TemplateDayName.PULL -> R.string.program_day_pull
    TemplateDayName.LEGS -> R.string.program_day_legs
    TemplateDayName.UPPER_A -> R.string.program_day_upper_a
    TemplateDayName.LOWER_A -> R.string.program_day_lower_a
    TemplateDayName.UPPER_B -> R.string.program_day_upper_b
    TemplateDayName.LOWER_B -> R.string.program_day_lower_b
    TemplateDayName.FULL_BODY_A -> R.string.program_day_full_body_a
    TemplateDayName.FULL_BODY_B -> R.string.program_day_full_body_b
    TemplateDayName.FULL_BODY_C -> R.string.program_day_full_body_c
    TemplateDayName.WORKOUT_A -> R.string.program_day_workout_a
    TemplateDayName.WORKOUT_B -> R.string.program_day_workout_b
    TemplateDayName.WORKOUT_C -> R.string.program_day_workout_c
    TemplateDayName.WORKOUT_D -> R.string.program_day_workout_d
    TemplateDayName.PRESS -> R.string.program_day_press
    TemplateDayName.DEADLIFT -> R.string.program_day_deadlift
    TemplateDayName.BENCH -> R.string.program_day_bench
    TemplateDayName.SQUAT -> R.string.program_day_squat
}
