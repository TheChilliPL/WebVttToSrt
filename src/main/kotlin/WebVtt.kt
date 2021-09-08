package me.patrykanuszczyk.webvtttosrt

import java.time.Duration

class WebVtt (
    var header: String? = null,
    val components: MutableList<Component> = mutableListOf()
) {
    sealed class Component

    @Suppress("UNUSED_PARAMETER")
    class NoteComponent(value: String): Component()

    @Suppress("UNUSED_PARAMETER")
    class StyleComponent(value: String): Component()

    class CueComponent(
        val start: Duration,
        val end: Duration,
        val payload: String,
        val identifier: String? = null,
        val settings: Map<String, String> = emptyMap()
    ): Component()

    fun toSrt(position: Boolean): String {
        var i = 1

        fun convertPayload(component: CueComponent): String {
            val allowed = listOf("i", "b")

            var subtitle = component.payload.replace(Regex("<(/?(\\w+)).*?>")) {
                if(it.groupValues[2].lowercase() in allowed)
                    "<" + it.groupValues[1] + ">"
                else ""
            }

            if(position) {
                val align = when(component.settings["align"]) {
                    in setOf("start", "left") -> 0
                    "center" -> 1
                    in setOf("end", "right") -> 2

                    else -> 1
                }
                val vertical = if(component.settings.contains("line")) {
                    val line = component.settings["line"]!!
                    if(line.endsWith("%")) {
                        val lineValue = line.dropLast(1).toFloatOrNull()

                        if(lineValue == null || lineValue > 60) 0
                        else if(lineValue >= 40) 2
                        else 1
                    } else {
                        val lineValue = line.toIntOrNull()

                        if(lineValue == null || lineValue < 0) 0
                        else 1
                    }
                } else 0
                val alignment = 1 + align + 4*vertical

                if(alignment != 2) subtitle = "{\\a${alignment}}" + subtitle
            }

            return subtitle
        }

        fun formatDuration(duration: Duration): String {
            val hh = duration.toHours().toString().padStart(2, '0')
            val mm = duration.toMinutesPart().toString().padStart(2, '0')
            val ss = duration.toSecondsPart().toString().padStart(2, '0')
            val fff = duration.toMillisPart().toString().padStart(3, '0')

            return "${hh}:${mm}:${ss}.${fff}"
        }

        fun convertCue(cueComponent: CueComponent): String {
            return "${i++}\n${formatDuration(cueComponent.start)} --> ${formatDuration(cueComponent.end)}" +
                "\n${convertPayload(cueComponent)}"
        }

        return components.mapNotNull {
            if(it !is CueComponent) null
            else convertCue(it)
        }.joinToString("\n\n")
    }

    companion object {
        private val cueTimestampLineRegex = Regex("^(?:(\\d+):)?(\\d+):(\\d+).(\\d+) --> (?:(\\d+):)?(\\d+):(\\d+).(\\d+)(.*)\$")

        fun parse(string: String): WebVtt {
            val componentStrings = string
                .split(Regex("(?m)^\\s*$"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val headerLine = componentStrings[0]

            if(!headerLine.startsWith("WEBVTT")) {
                throw WebVttParsingException("No valid WebVTT header found.")
            }

            val header = headerLine.substring(6).trimStart().ifEmpty { null }

            val components = componentStrings.drop(1).map {
                when {
                    it.startsWith("NOTE") -> {
                        NoteComponent(it.substring(4).trimStart())
                    }
                    it.startsWith("STYLE") -> {
                        StyleComponent(it.substring(5).trimStart())
                    }
                    else -> {
                        if(it.contains("-->")) {
                            val lines = it.lines()

                            val hasIdentifier = !lines[0].contains("-->")
                            val identifier = if(hasIdentifier) lines[0] else null
                            val timestampsLine = if(hasIdentifier) lines[1] else lines[0]
                            val rest = if(hasIdentifier) lines.drop(2) else lines.drop(1)

                            val match: MatchResult = cueTimestampLineRegex.matchEntire(timestampsLine)
                                ?: throw WebVttParsingException("Timestamp cue line invalid.")

                            val o = match.groupValues[9]
                            val (h0, m0, s0, f0, h1, m1, s1, f1) = match.groups.drop(1).dropLast(1).map {
                                it?.value?.toLong() ?: 0
                            }

                            val startTimestamp = h0.hours + m0.minutes + s0.seconds + f0.millis
                            val endTimestamp = h1.hours + m1.minutes + s1.seconds + f1.millis

                            val options = o.split(" ")
                                .filter { it.isNotEmpty() }
                                .associate {
                                    val option = it.split(":", limit = 2)
                                    option[0] to option[1]
                                }

                            CueComponent(
                                startTimestamp,
                                endTimestamp,
                                rest.joinToString("\n") { it.trim() },
                                identifier,
                                options
                            )
                        } else throw WebVttParsingException("Invalid component found.\n${it}")
                    }
                }
            }.toMutableList()
            return WebVtt(header, components)
        }
    }
}