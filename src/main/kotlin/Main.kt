package me.patrykanuszczyk.webvtttosrt

import me.patrykanuszczyk.webvtttosrt.parameters.parameters
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.createFile
import kotlin.io.path.name
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    val patterns = mutableListOf<String>()
    var startingPath = "./"
    var deleteOriginal = false
    var recursive = false
    parameters {
        fact("?", "h", "help") {
            println("Use -i for inputs.")
            stop()
        }
        string("s", "starting-path") {
            startingPath = it
        }
        string("i", "input") {
            patterns.add(it)
        }
        fact("D", "delete-original") {
            deleteOriginal = true
        }
        fact("r", "recursive") {
            recursive = true
        }
    }.parse(args).also { if(it.stopped) return@main }

    if(patterns.isEmpty()) {
        println("You need to pass at least one file.")
        return
    }

    val wildcardMultiple = Regex("(?<!\\\\)\\*")
    val wildcardSingle = Regex("(?<!\\\\)\\?")
    val regexes = patterns.map {
        Pattern.quote(it)
            .replace(wildcardMultiple, "\\\\E.*\\\\Q")
            .replace(wildcardSingle, "\\\\E.\\\\Q")
            .toRegex()
    }

//    val files = patterns.flatMap {
//        if(it.contains(wildcardMultiple) or it.contains(wildcardSingle))
//            File(startingPath).listFiles()!!.filter { file ->
//                regexes.any { file.name.matches(it) }
//            }
//        else
//            listOf(File(it))
//    }.distinct()
    val wildcardPatterns = mutableListOf<String>()
    val files = mutableSetOf<File>()
    val toCheck: Queue<File> = LinkedList()
    toCheck.add(File(startingPath))

    for(pattern in patterns) {
        if(pattern.contains(wildcardSingle) or pattern.contains(wildcardMultiple)) {
            wildcardPatterns.add(pattern)
        } else {
            files.add(File(pattern))
        }
    }

    do {
        val cwd = toCheck.poll() ?: break

        val (dirs, children) = cwd.listFiles()?.partition { it.isDirectory } ?: continue

        toCheck.addAll(dirs)

        files.addAll(children.filter { file -> regexes.any { file.name.matches(it) } })
    } while (recursive)

    if(files.isEmpty()) {
        println("Found no such files.")
        return
    }

    for(file in files) {
        if(!file.exists()) {
            println("File $file doesn't exist!")
            continue
        }

        println("Reading $file...")
        val text = file.readText()
        val webVtt = WebVtt.parse(text)

        val srt = webVtt.toSrt(true)

        val path = Path.of(file.parent, file.nameWithoutExtension + ".srt")
        if(path.toFile().exists()) path.toFile().delete()
        val newFile = path.createFile()
        newFile.writeText(srt)
        if(deleteOriginal) {
            file.delete()
            println("Created ${newFile.name} and removed ${file.name}!")
        } else {
            println("Created ${newFile.name}!")
        }
    }
}