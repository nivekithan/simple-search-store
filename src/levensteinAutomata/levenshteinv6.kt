package levensteinAutomata.v6

import dictionary.TrieNode
import dictionary.TrieTree


private fun normalizeOutputs(offset: Int, possibleOutputs: List<Pair<Int, Int>>): List<Pair<Int, Int>> {

    return possibleOutputs.map { Pair(it.first - offset, it.second) }
}

private fun denormalizeOutputs(offset: Int, possibleOutputs: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    return possibleOutputs.map { Pair(it.first + offset, it.second) }
}

/**
 * We should not memoize this function since the expectedWord which is independent on user
 * query will vary a lot, it will cause huge overhead in memory for little to no improvement in performance
 * based on your usage pattern
 */
private fun processCharacterForInput(
    /**
     * All of these results are normalized by their minimum offset
     */
    previousOperationResult: Pair<Int, Int>,
    chiVector: List<Boolean>, // maximum size of 3D + 1
    D: Int,
): List<Pair<Int, Int>> {
    val possibleOutputs = mutableListOf<Pair<Int, Int>>()

    val expectedWordOffset = previousOperationResult.first
    val DOffset = previousOperationResult.second

    val newExpectedChi = chiVector.subList(expectedWordOffset, chiVector.size)
    val newD = D - DOffset

    require(newD >= 0) { "newD less than 0, should never be created" }

    if (newD > 0) {
        // deletion
        possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))

        // substitution
        possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset + 1))
    }

    // matching letters

    if (newExpectedChi[0]) {
        possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset))
    }

    // Addition

    for ((i, chiValue) in newExpectedChi.withIndex()) {
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

        if (chiValue) {
            possibleOutputs.add(Pair(expectedWordOffset + i + 1, DOffset + i))
            break
        }
    }

    return possibleOutputs
}

data class ProcessCharacterCacheKey(
    val expectedWordOffset: Int,
    val DUsed: Int,
    val chiVector: List<Boolean>,
)

class ProcessCharacterCache(val D: Int) {
    private val cache = mutableMapOf<ProcessCharacterCacheKey, List<Pair<Int, Int>>>()

    fun memoizedProcessCharacterForInput(
        previousOperationResult: Pair<Int, Int>,
        chiVector: List<Boolean>,
    ): List<Pair<Int, Int>> {
        val cacheKey = ProcessCharacterCacheKey(
            expectedWordOffset = previousOperationResult.first,
            DUsed = previousOperationResult.second,
            chiVector = chiVector,
        )
        val cachedPossibleOutput = cache[cacheKey]

        if (cachedPossibleOutput != null) {
            return cachedPossibleOutput
        }

        val output = processCharacterForInput(previousOperationResult, chiVector, D)

        cache[cacheKey] = output

        return output
    }
}


class LevenshteinAutomata(val D: Int) {
    val MAX_CHI_WIDTH = (3 * D) + 1
    val cache = ProcessCharacterCache(D)

    fun characterProcessor(input: List<Pair<Int, Int>>, expectedWord: String, character: Char): List<Pair<Int, Int>> {
        if (input.isEmpty()) {
            return emptyList()
        }

        val possibleOutputs = mutableListOf<Pair<Int, Int>>()

        val minOffset = input.minOfOrNull { it.first }

        require(minOffset != null) { "[UNREACHABLE] State we are handling input.isEmpty() condition way before" }


        val chiVector = MutableList(MAX_CHI_WIDTH) { false }

        for (i in 0 until MAX_CHI_WIDTH) {
            val charFromExpectedWord = expectedWord.getOrNull(i + minOffset)

            chiVector[i] = when (charFromExpectedWord) {
                null -> false
                else -> charFromExpectedWord == character
            }
        }

        val normalizedInput = normalizeOutputs(minOffset, input)
        for (previousOperationResult in normalizedInput) {

            possibleOutputs.addAll(
                cache.memoizedProcessCharacterForInput(
                    previousOperationResult = previousOperationResult,
                    chiVector = chiVector,
                )
            )
        }

        return denormalizeOutputs(minOffset, possibleOutputs)
    }


    fun isExactMatch(input: List<Pair<Int, Int>>, expectedWord: String): Boolean {
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


fun fuzzySearchTrieTree(tree: TrieTree, query: String, automata: LevenshteinAutomata, maxWords: Int): List<String> {
    val output = mutableListOf<String>()


    fun dfs(node: TrieNode, input: List<Pair<Int, Int>>, prefix: String) {
        if (output.size >= maxWords) {
            return
        }

        for ((char, childNode) in node.children.entries) {

            if (output.size >= maxWords) {
                return
            }


            val result = automata.characterProcessor(input, query, char)

            if (result.isNotEmpty()) {
                val newPrefix = prefix + char.toString()
                // Continue searching this path

                if (childNode.isTerminal) {

                    if (automata.isExactMatch(result, query)) {
                        output.add(newPrefix)
                    }
                }

                dfs(childNode, result, newPrefix)
                continue
            }
            // remove this path from searching
        }

    }

    dfs(tree.root, listOf(Pair(0, 0)), "")


    return output
}


