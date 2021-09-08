package me.patrykanuszczyk.webvtttosrt.parameters

import me.patrykanuszczyk.webvtttosrt.discard
import java.util.*

sealed class Parameter(vararg val names: String)
class FactParameter(vararg names: String, val func: ParameterReceiver.() -> Unit): Parameter(*names)
class StringParameter(vararg names: String, @Suppress("MemberVisibilityCanBePrivate") val func: ParameterReceiver.(String) -> Unit): Parameter(*names)

class ParameterReceiver {
    var stopped = false
        private set
    fun stop() { stopped = true }
    val remaining = mutableListOf<String>()
}

class Parameters {
    val parameters: MutableList<Parameter> = mutableListOf()

    fun fact(vararg names: String, func: ParameterReceiver.() -> Unit) {
        parameters.add(FactParameter(*names, func=func))
    }

    fun string(vararg names: String, func: ParameterReceiver.(String) -> Unit) {
        parameters.add(StringParameter(*names, func=func))
    }

    fun parse(arguments: Array<String>): ParameterReceiver {
        val receiver = ParameterReceiver()

        val valuedLeft: Queue<StringParameter> = LinkedList()
        var processDashes = true

        fun findParameter(name: String) = parameters.find { name in it.names }

        fun dealWith(parameter: Parameter) = when(parameter) {
            is FactParameter -> parameter.func(receiver)
            is StringParameter -> valuedLeft.add(parameter).discard()
        }

        for(argument in arguments) {
            if(processDashes) {
                if(argument.startsWith("-")) {
                    if(argument.startsWith("--")) {
                        if(argument == "--") {
                            processDashes = false
                            continue
                        }

                        val parameterName = argument.substring(2)
                        val parameter = findParameter(parameterName)
                        if(parameter != null)
                            dealWith(parameter)
                        continue
                    }

                    for(letter in argument.substring(1)) {
                        val parameter = findParameter(letter.toString())
                        if(parameter != null)
                            dealWith(parameter)
                    }
                    continue
                }
            }

            val parameter = valuedLeft.poll()

            if(parameter != null) {
                parameter.func(receiver, argument)
                continue
            }

            receiver.remaining.add(argument)
        }

        return receiver
    }
}

fun parameters(func: Parameters.() -> Unit): Parameters {
    return Parameters().apply(func)
}