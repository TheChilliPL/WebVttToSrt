package me.patrykanuszczyk.webvtttosrt

import java.time.Duration

val Long.hours get() = Duration.ofHours(this)
val Long.minutes get() = Duration.ofMinutes(this)
val Long.seconds get() = Duration.ofSeconds(this)
val Long.millis get() = Duration.ofMillis(this)