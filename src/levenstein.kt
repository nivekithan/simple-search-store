/**
 * Checks if the two words `word1` and `word2` are within
 * levenshteinDistance of D
 */
fun levenshteinCheck(word1: String, word2: String, D: Int): Boolean {
    if (D < 0) {
        return false
    }

    if (word1.isEmpty()) {
        /**
         * This means to convert `word2` to `word1` we will have to delete all the remaining characters
         * of `word2`. Therefore `D` must be >= word2.length
         */
        return D >= word2.length
    }

    if (word2.isEmpty()) {
        /**
         * This means to convert `word2` to `word1` we will have to add all the remaining characters of
         * `word1` into `word2`. Therefore `D` must be >= word1.length
         */
        return D >= word1.length
    }


    /**
     * Once we deleted firstCharacter from `word2`, we will have to two new words `word1` and `word2[1:]`
     * and if those words are within D-1 levenshtein distance then we know `word1` and `word2` is within
     * D levenshtein distance too
     */

    if (levenshteinCheck(word1, word2.substring(1), D - 1)) {
        return true
    }

    /**
     * Once we added the firstCharacter from `word1` to `word2`. We know `word1[0] == word2[0]`. Therefore,
     * if these two new words `word1[1:]` and `word2` are within `D - 1` Levenshtein distance. Then we
     * can be sure that `word1` and `word2` are also within `D` Levenshtein distance
     */

    if (levenshteinCheck(word1.substring(1), word2, D - 1)) {
        return true
    }

    /**
     * Once we substituted `word2[0]` with `word1[0]`, we know `word1[0] == word2[0]`. Therefore, if these
     * two need words `word1[1:]` and `word2[1:]` are within `D - 1` Levenshtein distance. Then we can
     * be sure that `word1` and `word2` also within `D` Levenshtein distance
     */
    if (levenshteinCheck(word1.substring(1), word2.substring(1), D - 1)) {
        return true
    }


    /**
     * In case word1[0] == word2[0], then we can skip the first character from both words and check if
     * two new words `word1[1:]` and `word2[1:]` is within `D` Levenshtein distance. If yes, then we can
     * be sure that `word1` and `word2` also will be within `D` Levenshtein distance
     */

    if (word1[0] == word2[0]) {
        if (levenshteinCheck(word1.substring(1), word2.substring(1), D)) {
            return true
        }
    }

    return false
}
