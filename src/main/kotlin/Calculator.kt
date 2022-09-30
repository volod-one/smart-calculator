package calculator

import java.math.BigInteger
import kotlin.system.exitProcess

class Calculator {
    private var variables = mutableMapOf<String, String>()

    fun start() {
        while (true) {
            val inputController = InputController()
            inputController.readAndCategorize()
            inputController.validateAndCalculate()
        }
    }

    private inner class InputController {
        lateinit var inputType: String
        var input = ""
        var isInputValid = false

        fun readAndCategorize() {
            input = readln().trim()
            for (enum in InputTypes.values()) {
                if (input.matches(enum.regex)) {
                    inputType = enum.name
                    return
                }
            }
            inputType = "UNKNOWN_INPUT"
        }

        fun validateAndCalculate() {
            isInputValid = false
            when (inputType) {
                InputTypes.VARIABLE_ASSIGNMENT.toString() -> {
                    val key = input.substringBefore("=").trim()
                    val value = input.substringAfter("=").trim()
                    if (!key.matches(ValidatorRegex.KEY.regex)) {
                        InputErrors().wrongKey()
                        return
                    }
                    if (!value.matches(ValidatorRegex.VALUE.regex)) {
                        InputErrors().wrongValue()
                        return
                    }
                    if (variables.containsKey(value)) {
                        variables[key] = variables.getValue(value)
                    } else {
                        if (value.matches(ValidatorRegex.NUMERAL_VALUE.regex)) {
                            variables[key] = value

                        } else {
                            InputErrors().unknownVariable()
                            return
                        }
                    }
                    isInputValid = true
                }

                InputTypes.VARIABLE_CALL.toString() -> {
                    val variable = input
                    if (!variable.matches(ValidatorRegex.KEY.regex)) {
                        InputErrors().wrongKey()
                        return
                    }
                    if (variable !in variables) {
                        println("Unknown variable")
                        return
                    }
                    println(variables[variable])
                    isInputValid = true
                }

                InputTypes.VALUE.toString() -> {
                    println(input)
                    isInputValid = true
                }

                InputTypes.EXPRESSION.toString() -> {
                    if (!isValidExpression(input)) {
                        InputErrors().invalidExpression()
                        return
                    }
                    var inputList = splitExpression(input)
                    inputList = checkVariablesAndAssign(inputList) ?: return
                    inputList = removeRedundantOperators(inputList)

                    val postFixExpression = expressionToPostfix(inputList)
                    val result = calculatePostfix(postFixExpression)

                    println(result)
                    isInputValid = true
                }

                InputTypes.COMMAND.toString() -> runCommand()
                InputTypes.EMPTY.toString() -> return
            }
        }

        private fun runCommand() {
            for (enum in Commands.values()) {
                if (input == enum.command) {
                    isInputValid = true
                    enum.method()
                }
            }
            if (!isInputValid) println("Unknown command")
        }

        private fun splitExpression(input: String): MutableList<String> {
            val inputList = input.toMutableList().filter { !it.isWhitespace() }
            val outputList = mutableListOf<String>()
            var temp = ""
            for (i in inputList.indices) {
                if (inputList[i].isDigit()) {
                    temp += inputList[i]
                } else {
                    outputList.add(temp)
                    temp = inputList[i].toString()
                    outputList.add(temp)
                    temp = ""
                }
            }
            outputList.add(temp)
            return outputList.filter { it.isNotBlank() }.toMutableList()
        }

        private fun isValidExpression(string: String): Boolean {
            if (string.contains(ValidatorRegex.EXPRESSION.regex)) {
                return false
            }

            var openCount = 0
            var closeCount = 0
            for (i in string) {
                if (i == '(') openCount++
                if (i == ')') closeCount++
            }
            if (openCount != closeCount) {
                return false
            }

            return true
        }

        private fun checkVariablesAndAssign(input: MutableList<String>): MutableList<String>? {
            for (i in input) {
                if (i.matches("[a-zA-Z]+".toRegex())) {
                    if (variables.containsKey(i)) {
                        input[input.indexOf(i)] = variables.getValue(i).toString()
                    } else {
                        InputErrors().unknownVariable()
                        return null
                    }
                }
            }
            return input
        }

        private fun removeRedundantOperators(input: MutableList<String>): MutableList<String> {
            val outList = mutableListOf<String>()
            var temp = ""
            for (i in input.size - 1 downTo 0) {
                if (input[i] !in "+-") {
                    outList.add(input[i])
                } else {
                    if (input[i] == "+" && input[i - 1] !in "+-") outList.add(input[i])
                    if (input[i] == "-") {
                        if (input[i - 1] != "-") {
                            if (temp.length % 2 != 0) {
                                outList.add("+")
                            } else {
                                if (outList.last() != "(") {
                                    val last = outList.removeLast()
                                    outList.add("-${last}")
                                    outList.add("+")
                                } else {
                                    outList.add("-")
                                }
                            }
                            temp = ""
                        } else {
                            temp += input[i]
                        }
                    }
                }
            }
            return outList.reversed().toMutableList()
        }

        private fun expressionToPostfix(expression: MutableList<String>): MutableList<String> {
            val queue = mutableListOf<String>()
            val stack = mutableListOf<String>()
            val postFix = mutableListOf<String>()

            for (i in expression) {
                if (i.matches("-?\\d+".toRegex())) queue.add(i)
                if (i in "+-*/^") {
                    if (stack.isNotEmpty()) {
                        when (i) {
                            "+", "-" -> {
                                for (j in stack.size - 1 downTo 0) {
                                    if (stack[j] !in "+-*/") break
                                    queue.add(stack.removeLast())
                                }
                                stack.add(i)
                            }

                            "*", "/" -> {
                                for (j in stack.size - 1 downTo 0) {
                                    if (stack[j] !in "*/") break
                                    queue.add(stack.removeLast())
                                }
                                stack.add(i)
                            }

                            "^" -> {
                                queue.add(i)
                            }
                        }
                    } else {
                        stack.add(i)
                    }

                }
                if (i == "(") stack.add(i)
                if (i == ")") {
                    var removed = ""
                    while (removed != "(") {
                        removed = stack.removeLast()
                        queue.add(removed)
                    }
                }
            }
            for (j in stack.size - 1 downTo 0) {
                queue.add(stack.removeLast())
            }
            postFix.addAll(queue.filter { it != "(" })

            return postFix
        }

        private fun calculatePostfix(postfix: MutableList<String>): BigInteger {
            val stack = mutableListOf<String>()
            for (i in postfix) {
                if (i in "+-*/^") {
                    val num2 = stack.removeLast().toBigInteger()
                    val num1 = stack.removeLast().toBigInteger()
                    when (i) {
                        "+" -> stack.add((num1 + num2).toString())
                        "-" -> stack.add((num1 - num2).toString())
                        "*" -> stack.add((num1 * num2).toString())
                        "/" -> stack.add((num1 / num2).toString())
                        "^" -> stack.add((num1.toBigDecimal().pow(num2.toInt()).toString()))
                    }

                } else {
                    stack.add(i)
                }
            }
            return stack.sumOf { it.toBigInteger() }
        }
    }

    private class InputErrors {
        fun wrongKey() = println("Invalid identifier")
        fun wrongValue() = println("Invalid assignment")
        fun invalidExpression() = println("Invalid expression")
        fun unknownVariable() = println("Unknown variable")
    }

    private enum class Commands(val command: String, val method: () -> Unit) {
        EXIT("/exit", {
            println("Bye!")
            exitProcess(200)
        }),
        HELP("/help", {
            println("The program calculates numbers")
        })
    }

    private enum class InputTypes(val regex: Regex) {
        VARIABLE_ASSIGNMENT(".+=.+".toRegex()),
        VARIABLE_CALL("\\b([a-zA-Z]+\\d+.*|\\d+[a-zA-Z]+.*|\\b[a-zA-Z]+|\\p{L}+)".toRegex()),
        VALUE("\\d+".toRegex()),
        EXPRESSION("(((\\(\\b)|\\b\\w+)+\\s*[-+*/()^]\\s*)+.*".toRegex()),
        COMMAND("/\\b[a-zA-Z]+".toRegex()),
        EMPTY("^$".toRegex()),
    }

    private enum class ValidatorRegex(val regex: Regex) {
        KEY("[a-zA-Z]+".toRegex()),
        VALUE("([a-zA-Z]+|-?\\d+)".toRegex()),
        NUMERAL_VALUE("\\d+".toRegex()),
        EXPRESSION("([*/]{2,})".toRegex())
    }

}