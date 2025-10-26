package levensteinAutomata.v4.bitmask

import dictionary.TrieNode
import dictionary.TrieTree


/**
 * We should not memoize this function since the expectedWord which is independent on user
 * query will vary a lot, it will cause huge overhead in memory for little to no improvement in performance
 * based on your usage pattern
 */
private fun processCharacterForInput(
    previousOperationResult: Pair<Int, Int>,
    chiMask: Long,
    expectedWordLength: Int,
    D: Int,
): List<Pair<Int, Int>> {
    val possibleOutputs = mutableListOf<Pair<Int, Int>>()
    val expectedWordOffset = previousOperationResult.first
    val DOffset = previousOperationResult.second

    val remainingLength = expectedWordLength - expectedWordOffset
    val newD = D - DOffset

    require(newD >= 0) { "newD less than 0, should never be created" }

    if (remainingLength <= 0) {
        if (newD > 0) {
            possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))
        }
        return possibleOutputs
    }

    if (newD > 0) {
        possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))
        possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset + 1))
    }

    val remainingMask = chiMask ushr expectedWordOffset

    if ((remainingMask and 1L) != 0L) {
        possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset))
    }

    var shiftedMask = remainingMask ushr 1
    var additionDistance = 1

    while (shiftedMask != 0L && additionDistance <= newD && additionDistance < remainingLength) {
        if ((shiftedMask and 1L) != 0L) {
            possibleOutputs.add(Pair(expectedWordOffset + additionDistance + 1, DOffset + additionDistance))
            break
        }
        shiftedMask = shiftedMask ushr 1
        additionDistance++
    }

    return possibleOutputs
}

data class ProcessCharacterCacheKey(
    val expectedWordOffset: Int,
    val DUsed: Int,
    val chiMask: Long,
    val expectedWordLength: Int,
    val D: Int,
)

object ProcessCharacterCache {
    private val cache = mutableMapOf<ProcessCharacterCacheKey, List<Pair<Int, Int>>>()

    fun memoizedProcessCharacterForInput(
        previousOperationResult: Pair<Int, Int>,
        chiMask: Long,
        expectedWordLength: Int,
        D: Int,
    ): List<Pair<Int, Int>> {
        val cacheKey = ProcessCharacterCacheKey(
            expectedWordOffset = previousOperationResult.first,
            DUsed = previousOperationResult.second,
            chiMask = chiMask,
            expectedWordLength = expectedWordLength,
            D = D,
        )
        val cachedPossibleOutput = cache[cacheKey]

        if (cachedPossibleOutput != null) {
            return cachedPossibleOutput
        }

        val output = processCharacterForInput(previousOperationResult, chiMask, expectedWordLength, D)

        cache[cacheKey] = output

        return output
    }
}


class LevenshteinAutomata(val expectedWord: String, val D: Int) {

    private val expectedWordLength = expectedWord.length

    fun characterProcessor(input: List<Pair<Int, Int>>, chiMask: Long): List<Pair<Int, Int>> {
        val possibleOutputs = mutableListOf<Pair<Int, Int>>()

        for (previousOperationResult in input) {
            possibleOutputs.addAll(
                ProcessCharacterCache.memoizedProcessCharacterForInput(
                    previousOperationResult = previousOperationResult,
                    chiMask = chiMask,
                    expectedWordLength = expectedWordLength,
                    D = D
                )
            )
        }

        return possibleOutputs
    }


    fun isExactMatch(input: List<Pair<Int, Int>>): Boolean {
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

    require(query.length <= Long.SIZE_BITS) {
        "Query length exceeds bitmask capacity; use a larger mask type"
    }

    val chiMaskByChar = mutableMapOf<Char, Long>()

    query.forEachIndexed { index, char ->
        val existingMask = chiMaskByChar.getOrDefault(char, 0L)
        chiMaskByChar[char] = existingMask or (1L shl index)
    }

    fun dfs(node: TrieNode, input: List<Pair<Int, Int>>, prefix: String, D: Int) {

        if (output.size >= maxWords) {
            return
        }

        for ((char, childNode) in node.children.entries) {

            if (output.size >= maxWords) {
                return
            }

            val chiMask = chiMaskByChar[char] ?: 0L

            val result = automata.characterProcessor(input, chiMask)

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

    dfs(tree.root, listOf(Pair(0, 0)), "", D)


    return output
}


