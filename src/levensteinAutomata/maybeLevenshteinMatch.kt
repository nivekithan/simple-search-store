package levensteinAutomata.v2

import dictionary.TrieNode
import dictionary.TrieTree
import levensteinAutomata.v1.levenshteinCheck

/**
 * Checks if a prefix could potentially match a word within a given edit distance.
 *
 * Given a prefix string, this function determines whether it's possible to complete
 * that prefix into a full string that has Levenshtein distance ≤ D from the expected word.
 *
 * @param expectedWord The target word to match
 * @param knownTestCharacters The prefix string (characters known so far)
 * @param D Maximum allowed Levenshtein distance
 * @return true if the prefix can potentially match within distance D
 *
 * Examples:
 *
 * canMatchWithinDistance("hello", "h", 0) → true
 *   ✓ Can complete "h" → "hello" (distance = 0)
 *
 * canMatchWithinDistance("hello", "e", 0) → false
 *   ✗ Cannot complete "e" to match "hello" with distance 0
 *   (first character mismatch requires at least 1 edit)
 *
 * canMatchWithinDistance("hello", "e", 1) → true
 *   ✓ Can complete "e" → "ehello", then delete first 'e' (distance = 1)
 */
fun canMatchWithinDistance(expectedWord: String, knownTestCharacters: String, D: Int): Boolean {

    if (D < 0) {
        return false
    }

    if (expectedWord.isEmpty()) {
        /**
         * This means to convert `word2` to `word1` we will have to delete all the remaining characters
         * of `word2`. Therefore `D` must be >= word2.length
         */
        return D >= knownTestCharacters.length
    }

    if (knownTestCharacters.isEmpty()) {
        /**
         * This is where the code differs from previous implementation, since we can assume new
         * characters can be appended without reducing the D. That means this case is always true
         */
        return true
    }


    /**
     * Once we deleted firstCharacter from `word2`, we will have to two new words `word1` and `word2[1:]`
     * and if those words are within D-1 levenshtein distance then we know `word1` and `word2` is within
     * D levenshtein distance too
     */

    if (canMatchWithinDistance(expectedWord, knownTestCharacters.substring(1), D - 1)) {
        return true
    }

    /**
     * Once we added the firstCharacter from `word1` to `word2`. We know `word1[0] == word2[0]`. Therefore,
     * if these two new words `word1[1:]` and `word2` are within `D - 1` Levenshtein distance. Then we
     * can be sure that `word1` and `word2` are also within `D` Levenshtein distance
     */

    if (canMatchWithinDistance(expectedWord.substring(1), knownTestCharacters, D - 1)) {
        return true
    }

    /**
     * Once we substituted `word2[0]` with `word1[0]`, we know `word1[0] == word2[0]`. Therefore, if these
     * two need words `word1[1:]` and `word2[1:]` are within `D - 1` Levenshtein distance. Then we can
     * be sure that `word1` and `word2` also within `D` Levenshtein distance
     */
    if (canMatchWithinDistance(expectedWord.substring(1), knownTestCharacters.substring(1), D - 1)) {
        return true
    }


    /**
     * In case word1[0] == word2[0], then we can skip the first character from both words and check if
     * two new words `word1[1:]` and `word2[1:]` is within `D` Levenshtein distance. If yes, then we can
     * be sure that `word1` and `word2` also will be within `D` Levenshtein distance
     */
    if (expectedWord[0] == knownTestCharacters[0]) {
        if (canMatchWithinDistance(expectedWord.substring(1), knownTestCharacters.substring(1), D)) {
            return true
        }
    }

    return false
}

fun fuzzySearchTrieTree(tree: TrieTree, query: String, D: Int, maxWords: Int): List<String> {
    val output = mutableListOf<String>()

    fun dfs(node: TrieNode, prefixString: String, D: Int) {
        if (output.size >= maxWords) {
            return
        }

        for ((char, childNode) in node.children.entries) {

            if (output.size >= maxWords) {
                return
            }

            val knownTestCharacters = prefixString + char.toString()
            if (canMatchWithinDistance(query, knownTestCharacters, D)) {
                // Continue searching this path

                if (childNode.isTerminal) {
                    // Check if the knownTestCharacters and query is actually within the Levenshtein distance D
                    // without adding more suffix characters.
                    if (levenshteinCheck(query, knownTestCharacters, D)) {
                        output.add(knownTestCharacters)
                    }
                }

                dfs(childNode, knownTestCharacters, D)

                continue
            }
            // remove this path from searching
        }

    }

    dfs(tree.root, "", D)


    return output
}