package io.github.amibhai.liftlog.core.data.routine

import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType
import io.github.amibhai.liftlog.core.domain.model.Stage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The `rule_params` column: the settings of all rules, of which each uses its own. */
@Serializable
private data class RuleParams(
    val incrementKg: Double? = null,
    val missesBeforeDeload: Int? = null,
    val deloadPercent: Int? = null,
    val stages: List<StageParams> = emptyList(),
    val resetPercent: Int? = null,
    val trainingMaxKg: Double? = null,
    val since: Long? = null,
)

@Serializable
private data class StageParams(val sets: Int, val reps: Int)

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Null for rules without settings. */
internal fun ProgressionRule.toParams(): String? = when (this) {
    is ProgressionRule.Linear ->
        RuleParams(incrementKg, missesBeforeDeload = missesBeforeDeload, deloadPercent = deloadPercent)

    is ProgressionRule.Stages ->
        RuleParams(incrementKg, stages = stages.map { StageParams(it.sets, it.reps) }, resetPercent = resetPercent)

    is ProgressionRule.FiveThreeOne -> RuleParams(incrementKg, trainingMaxKg = trainingMaxKg, since = since)

    ProgressionRule.DoubleProgression, ProgressionRule.None -> null
}?.let { json.encodeToString(it) }

/** What is missing or unreadable in [params] takes its default. */
internal fun progressionRule(type: ProgressionRuleType, params: String?): ProgressionRule {
    val stored = params?.let(::parse) ?: RuleParams()
    return when (type) {
        ProgressionRuleType.DOUBLE_PROGRESSION -> ProgressionRule.DoubleProgression

        ProgressionRuleType.LINEAR -> ProgressionRule.Linear(
            incrementKg = stored.incrementKg,
            missesBeforeDeload = stored.missesBeforeDeload ?: ProgressionRule.DEFAULT_MISSES,
            deloadPercent = stored.deloadPercent ?: ProgressionRule.DEFAULT_DELOAD_PERCENT,
        )

        ProgressionRuleType.STAGES -> ProgressionRule.Stages(
            stages = stored.stages.map { Stage(it.sets, it.reps) },
            incrementKg = stored.incrementKg,
            resetPercent = stored.resetPercent ?: ProgressionRule.DEFAULT_RESET_PERCENT,
        )

        ProgressionRuleType.FIVE_THREE_ONE -> ProgressionRule.FiveThreeOne(
            trainingMaxKg = stored.trainingMaxKg,
            incrementKg = stored.incrementKg ?: ProgressionRule.DEFAULT_ROUND_INCREMENT_KG,
            since = stored.since ?: 0,
        )

        ProgressionRuleType.NONE -> ProgressionRule.None
    }
}

private fun parse(params: String): RuleParams? = try {
    json.decodeFromString<RuleParams>(params)
} catch (_: SerializationException) {
    null
}
