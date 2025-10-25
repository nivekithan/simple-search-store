package levensteinAutomata.v3.draftv1

fun isPossibleMatch(expectedWord: String, query: String, D: Int): Boolean {

    fun firstCharacterProcessor(firstCharacter: Char): List<Pair<Int, Int>> {
        /**
         * The goal is to do these things
         *
         *  1. consume the firstCharacter by one of these operations
         *      - Deletion
         *      - Addition
         *      - Substitution
         *      - Matching letters
         *
         * 2. Figure out type for firstCharacterProcessor output such that it can be an input to the
         * secondCharacterProcessor function, so that secondCharacterProcessor function can continue operation
         * without knowing about firstCharacter
         *
         *
         * For now let's focus on deletion operation
         *
         * We know
         *
         * - it consumes the firstCharacter
         * - it does not consume any character expectedWord
         * - it decrease the D by 1
         *
         *  Substitution operation
         *
         *  - it consumes the firstCharacter
         *  - it consumes the firstCharacter from expectedWord
         *  - it decreases the D by 1
         *
         *  Matching letters
         *
         *  - This is valid operation only when expectedWord == firstCharacter
         *  - it consumes firstCharacter
         *  - it consumes the firstCharacter from expectedWord
         *  - it leaves the D unchanged
         *
         * So we can see the output of firstCharacterProcessor can be represented as a tuple of (boolean, boolean)
         *
         * - the first boolean tells whether to consume the first character from expectedWord
         * - the second boolean tells whether to decrease the D by one
         *
         * So these operations can be represented as
         *
         * - Deletion - (false, true)
         * - Substitution  - (true, true)
         * - matching letters - (true, false)
         *
         * But as you can see we have not included the Addition operation here. Why is that ?
         *
         * Well, let's write down what does Addition operation does
         *
         * - it does not consume the firstCharacter
         * - it consumes the `firstCharacter` from expectedWord
         * - it decreases the D by 1
         *
         * The issue is our function `firstCharacterProcessor` has to consume firstCharacter, otherwise we cannot
         * pass our result and let `secondCharacterProcessor` continue.
         *
         * Therefore in case of addition, we have to follow it up with another operation which does consume
         * the `firstCharacter`
         *
         * So let's consider the case `addition` followed by `deletion`
         *
         * This will consume
         *
         * 1. Consume firstCharacter (because of deletion operation)
         * 2. Consume the firstCharacter from expectedWord (because of addition operation)
         * 3. Decrease the D by 2 (because of both addition and deletion operation)
         *
         * You might notice, that this is an inefficient way to consume both firstCharacter and firstCharacter from
         * expectedWord. Since the `substitution` consumes both of these and only decreases D by 1.
         *
         * Therefore, we can ignore `addition` followed by `deletion`
         *
         * So now let's consider the case 'addition' followed by 'substitution'
         *
         * This will
         *
         * 1. Consume firstCharacter (because of substitution)
         * 2. Consumes both firstCharacter and secondCharacter from expected. FirstCharacter will be consume by addition
         * and second character will be consumed by substitution
         * 3. Decreases the D by 2
         *
         * You might notice, this is equivalent to `substitution` followed by `addition`
         *
         * Therefore, we can ignore `addition` followed by `substitution` too
         *
         * So now let's consider the case `addition` followed by `matching letters`
         *
         * This will
         *
         * 1. Consumes fistCharacter (because of matching letters)
         * 2. Consumes both firstCharacter and secondCharacter from expectedWord. FirstCharacter will be consumed by
         * addition and secondCharacter will be consumed by `matching letters`
         * 3. It decreases the D by only 1
         * 4. For this to be valid the firstCharacter == expectedWord[1], only then matchingLetter condition will be
         * valid
         *
         * So now let's consider the case 'addition' followed by yet another 'addition'. We can have as much as
         * `addition` followed by another `addition`. But all of these only will be valid under these condition
         *
         * 1. if these sequence of addition ends with a matching letters operation so that firstCharacter get's consumed
         * 2. The number of `addition` operations is equal or less than D
         *
         * This will consume
         *
         * 1. Consumes firstCharacter (because of matching letters)
         * 2. Consumes as many as addition operation possible + 1 (because of matching letter operation at the end)
         * letters from expectedWord
         * 3. Decreases as many as addition operation we have considered from D
         *
         *
         * So to represent the last point in our type system we will change the return type from (Boolean, Boolean)
         * to (Int, Int).
         *
         * The first Int represents number of letters to consume in expectedWord and second Int number of value to
         * decrease D by
         *
         * Therefore
         *
         * 1. Deletion operation can be represented as -> (0, 1)
         * 2. Substitution operation can be represented as -> (1, 1)
         * 3. Matching letters can be represented as -> (1, 0)
         * 4. Addition operation will be a variable
         *
         */

        val possibleOutputs = mutableListOf<Pair<Int, Int>>()

        // Deletion operation

        possibleOutputs.add(Pair(0, 1))

        // Substitution operation
        possibleOutputs.add(Pair(1, 1))

        // Matching letters

        if (expectedWord[0] == firstCharacter) {
            possibleOutputs.add(Pair(1, 0))
        }

        // Addition operation

        /**
         * The condition for addition operation is that it has to be
         *
         * 1. end with matching letters operation
         * 2. The number of addition letters in the sequence must be less than D
         */

        for ((i, char) in expectedWord.withIndex()) {
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

            if (i >= D) {
                /**
                 * We could not find such a character in expectedWord, therefore addition operation is never possible
                 */
                break
            }

            if (char == firstCharacter) {
                possibleOutputs.add(Pair(i + 1, i))
                break
            }
        }

        return possibleOutputs
    }

    fun secondCharacterProcessor(input: List<Pair<Int, Int>>, secondCharacter: Char?): List<Pair<Int, Int>> {
        val possibleOutputs = mutableListOf<Pair<Int, Int>>()

        for (previousOperationResult in input) {
            val expectedWordOffset = previousOperationResult.first
            val DOffset = previousOperationResult.second

            val newExpectedWord = expectedWord.substring(expectedWordOffset)
            val newD = D - DOffset

            if (newD < 0) {
                // The previous state is invalid since it already went into negative
                break
            }

            if (newExpectedWord.isEmpty()) {
                /**
                 * Then the only operation is to delete the secondCharacter
                 */
                possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))
                break
            }

            // deletion
            possibleOutputs.add(Pair(expectedWordOffset, DOffset + 1))


            // substitution
            possibleOutputs.add(Pair(expectedWordOffset + 1, DOffset + 1))

            // matching letters

            if (newExpectedWord[0] == secondCharacter) {
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

                if (i >= DOffset) {
                    /**
                     * We could not find such a character in expectedWord, therefore addition operation is never possible
                     */
                    break
                }

                if (char == secondCharacter) {
                    possibleOutputs.add(Pair(expectedWordOffset + i + 1, DOffset + i))
                    break
                }
            }
        }

        return possibleOutputs
    }

    return false
}

//fun fuzzySearchTrieTree(tree: TrieTree, query: String, D: Int, maxWords: Int): List<String> {
//    val output = mutableListOf<String>()
//
//    fun dfs(node: TrieNode, prefixString: String, D: Int) {
//        if (output.size >= maxWords) {
//            return
//        }
//
//        for ((char, childNode) in node.children.entries) {
//
//            if (output.size >= maxWords) {
//                return
//            }
//
//            val knownTestCharacters = prefixString + char.toString()
//            if (canMatchWithinDistance(query, knownTestCharacters, D)) {
//                // Continue searching this path
//
//                if (childNode.isTerminal) {
//                    // Check if the knownTestCharacters and query is actually within the Levenshtein distance D
//                    // without adding more suffix characters.
//                    if (levenshteinCheck(query, knownTestCharacters, D)) {
//                        output.add(knownTestCharacters)
//                    }
//                }
//
//                dfs(childNode, knownTestCharacters, D)
//
//                continue
//            }
//            // remove this path from searching
//        }
//
//    }
//
//    dfs(tree.root, "", D)
//
//
//    return output
//}
//
