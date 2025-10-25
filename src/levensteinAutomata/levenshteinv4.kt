package levensteinAutomata.v4

import dictionary.TrieNode
import dictionary.TrieTree
import kotlin.math.abs

/**
 * Read levenshteinv3.draftv1.kt before file
 */

/**
 * We can combine both firstCharacterProcessor and secondCharacterProcessor into a common
 * character processor function.
 *
 * Then instead of having an outer function and calling another function with closure. We will create
 * class for it
 */
class LevenshteinAutomata(val expectedWord: String, val D: Int) {
    fun characterProcessor(input: Set<Pair<Int, Int>>, character: Char): Set<Pair<Int, Int>> {
        val possibleOutputs = mutableSetOf<Pair<Int, Int>>()

        for (previousOperationResult in input) {
            val expectedWordOffset = previousOperationResult.first
            val DOffset = previousOperationResult.second

            val newExpectedWord = expectedWord.substring(expectedWordOffset)
            val newD = D - DOffset

            require(newD >= 0, { "newD less than 0, should never be created" })

            if (newExpectedWord.isEmpty()) {
                if (newD > 0) {
                    /**
                     * Then the only operation is to delete the secondCharacter
                     */
                    possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))
                }

                /**
                 * If we don't have newD budget, then there is no way to reach expectedWord
                 */
                continue
            }

            if (newD > 0) {
                // deletion
                possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))

                // substitution
                possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset + 1))
            }

            // matching letters

            if (newExpectedWord[0] == character) {
                possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset))
            }

            // Addition

            for ((i, char) in newExpectedWord.withIndex()) {
                /**
                 * We are trying to a find character in expectedWord which is equal to firstCharacter and the number of
                 * addition operations required must be less than D
                 *
                 */
                if (i == 0) {
                    /**
                     * Skip this entry, since if firstCharacter == char in this condition then it is same as matching
                     * letter operation
                     */
                    continue
                }

                if (i > newD) {
                    /**
                     * We could not find such a character in expectedWord, therefore addition operation is never possible
                     */
                    break
                }

                if (char == character) {
                    possibleOutputs.add(Pair(expectedWordOffset + i + 1, DOffset + i))
                    break
                }
            }
        }

        val onlyUserStates = mutableSetOf<Pair<Int, Int>>()

        possibleOutputs.filterTo(onlyUserStates, fun(
            cur: Pair<Int, Int>
        ): Boolean {

            for (otherOutput in possibleOutputs) {
                if (otherOutput == cur) {
                    continue
                }

                /**
                 * Examples
                 *
                 * cur = (4, 5)
                 * otherOutput = (1, 2)
                 *
                 * otherOutput implies cur therefore we can ignore `cur`
                 *
                 * Example
                 *
                 * cur = (4, 5)
                 * otherOutput = (0, 2)
                 *
                 * we can't ignore cur
                 *
                 * Example
                 *
                 * cur = (1, 2)
                 * otherOutput = (4,5)
                 *
                 * we can't ignore cur
                 *
                 *
                 */
                val differenceInDUsage = cur.second - otherOutput.second

                if (differenceInDUsage < 0) {
                    // our current output has used less edits than otherOutput
                    // therefore there is no chance, our currentOutput is dominated by
                    // otherOutput
                    continue
                }

                val numberOfOffsetsToMakeupFor = abs(cur.first - otherOutput.first)


                if (differenceInDUsage >= numberOfOffsetsToMakeupFor) {
                    return false
                }
            }

            return true
        })

        return onlyUserStates

    }


    fun isExactMatch(input: Set<Pair<Int, Int>>): Boolean {
        for (previousState in input) {
            val newExpectedWord = expectedWord.substring(previousState.first)
            val newD = D - previousState.second

            /**
             * The user query is finished, so we have to check whether any of these states. Allows
             * entier newExpectedWord to be deleted
             */

            if (newD >= newExpectedWord.length) {
                return true
            }

        }
        return false
    }
}


fun fuzzySearchTrieTree(tree: TrieTree, query: String, D: Int, maxWords: Int): List<String> {
    val output = mutableListOf<String>()
    val automata = LevenshteinAutomata(query, D)

    fun dfs(node: TrieNode, input: Set<Pair<Int, Int>>, prefix: String, D: Int) {
        if (output.size >= maxWords) {
            return
        }

        for ((char, childNode) in node.children.entries) {

            if (output.size >= maxWords) {
                return
            }


            val result = automata.characterProcessor(input, char)

            if (result.isNotEmpty()) {
                val newPrefix = prefix + char.toString()
                // Continue searching this path

                if (childNode.isTerminal) {

                    if (automata.isExactMatch(result)) {
                        output.add(newPrefix)
                    }
                }

                dfs(childNode, result, newPrefix, D)
                continue
            }
            // remove this path from searching
        }

    }

    dfs(tree.root, setOf(Pair(0, 0)), "", D)


    return output
}


