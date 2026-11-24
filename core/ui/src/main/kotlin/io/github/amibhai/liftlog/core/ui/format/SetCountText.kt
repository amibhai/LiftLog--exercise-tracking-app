package io.github.amibhai.liftlog.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import io.github.amibhai.liftlog.core.ui.R
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

/** "12 sets", and half sets of secondary muscles count too: "12.5 sets". */
@Composable
fun setCountText(sets: Double): String = pluralStringResource(
    R.plurals.ui_set_count,
    // A part of a set reads like several: "0.5 sets".
    if (sets == 1.0) 1 else ceil(sets).toInt().coerceAtLeast(2),
    LiftLogFormat.decimal(BigDecimal.valueOf(sets).setScale(1, RoundingMode.HALF_UP)),
)
