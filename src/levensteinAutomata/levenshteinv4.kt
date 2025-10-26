package levensteinAutomata.v4

import dictionary.TrieNode
import dictionary.TrieTree


/**
 * We should not memoize this function since the expectedWord which is independent on user
 * query will vary a lot, it will cause huge overhead in memory for little to no improvement in performance
 * based on your usage pattern
 */
private fun processCharacterForInput(
    previousOperationResult: Pair<Int, Int>,
    chiVector: List<Boolean>,
    D: Int,
): List<Pair<Int, Int>> {
    val possibleOutputs = mutableListOf<Pair<Int, Int>>()
    val expectedWordOffset = previousOperationResult.first
    val DOffset = previousOperationResult.second

    val newExpectedChi = chiVector.subList(expectedWordOffset, chiVector.size)
    val newD = D - DOffset

    require(newD >= 0, { "newD less than 0, should never be created" })

    if (newExpectedChi.isEmpty()) {
        if (newD > 0) {
            /**
             * Then the only operation is to delete the secondCharacter
             */
            possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))
        }

        /**
         * If we don't have newD budget, then there is no way to reach expectedWord
        //             */
        return possibleOutputs
    }

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
    val D: Int,
)

object ProcessCharacterCache {
    private val cache = mutableMapOf<ProcessCharacterCacheKey, List<Pair<Int, Int>>>()

    fun memoizedProcessCharacterForInput(
        previousOperationResult: Pair<Int, Int>,
        chiVector: List<Boolean>,
        D: Int,
    ): List<Pair<Int, Int>> {
        val cacheKey = ProcessCharacterCacheKey(
            expectedWordOffset = previousOperationResult.first,
            DUsed = previousOperationResult.second,
            chiVector = chiVector,
            D = D,
        )
        val cachedPossibleOutput = cache.get(cacheKey)

        if (cachedPossibleOutput != null) {
            return cachedPossibleOutput
        }

        val output = processCharacterForInput(previousOperationResult, chiVector, D)

        cache[cacheKey] = output

        return output
    }
}


class LevenshteinAutomata(val expectedWord: String, val D: Int) {

    fun characterProcessor(input: List<Pair<Int, Int>>, chiVector: List<Boolean>): List<Pair<Int, Int>> {
        val possibleOutputs = mutableListOf<Pair<Int, Int>>()


        for (previousOperationResult in input) {

            possibleOutputs.addAll(
                ProcessCharacterCache.memoizedProcessCharacterForInput(
                    previousOperationResult = previousOperationResult,
                    chiVector = chiVector,
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


    fun dfs(node: TrieNode, input: List<Pair<Int, Int>>, prefix: String, D: Int) {
        if (output.size >= maxWords) {
            return
        }

        for ((char, childNode) in node.children.entries) {

            if (output.size >= maxWords) {
                return
            }

            val chiVector = query.map { it == char }

            val result = automata.characterProcessor(input, chiVector)

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


