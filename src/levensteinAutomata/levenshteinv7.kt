@file:Suppress("PackageDirectoryMismatch")

package levensteinAutomata.v7


data class State(val offset: Int, val dUsed: Int) {}

data class StateKey(val allStates: List<State>)
data class NormalisedStateKey(val shift: Int, val nextStateKey: StateKey)

class LevenshteinAutomata(val D: Int) {
    val maxChiWidth = 3 * D + 1
    val precomputedAutomata: HashMap<StateKey, HashMap<Int, NormalisedStateKey>> = hashMapOf()

    init {
        require(D <= 9, { "Levenshtein Automata does no support D greater than 9" })

        val initialStateKey = StateKey(listOf(State(offset = 0, dUsed = 0)))
        val queue = ArrayDeque<StateKey>()

        queue.add(initialStateKey)
        precomputedAutomata[initialStateKey] = hashMapOf()

        while (queue.isNotEmpty()) {
            val keyUnderProcess = queue.removeFirst()
            val transitionMap = precomputedAutomata.get(keyUnderProcess)

            require(transitionMap != null, { "[UNREACHABLE] The transitionMap should always be defined" })

            for (i in 0 until (1 shl maxChiWidth)) {
                val nextStates = getTransition(stateKey = keyUnderProcess, chiVector = i)


                val normalizedState = normalise(nextStates)

                transitionMap[i] = normalizedState

                if (!precomputedAutomata.containsKey(normalizedState.nextStateKey)) {
                    precomputedAutomata[normalizedState.nextStateKey] = hashMapOf()
                    queue.add(normalizedState.nextStateKey)
                }
            }
        }
    }

    fun initialStateKey(): NormalisedStateKey {
        return NormalisedStateKey(0, StateKey(listOf(State(offset = 0, dUsed = 0))))
    }

    fun step(query: String, state: NormalisedStateKey, character: Char): NormalisedStateKey? {
        val encoding = characteristicEncoding(query, state.shift, character)
        val transition = precomputedAutomata[state.nextStateKey] ?: return null

        val nextState = transition[encoding] ?: return null

        if (nextState.nextStateKey.allStates.isEmpty()) {
            return null
        }

        return NormalisedStateKey(state.shift + nextState.shift, nextState.nextStateKey)
    }

    private fun characteristicEncoding(query: String, offset: Int, character: Char): Int {
        var marker = 1 shl (maxChiWidth - 1)
        var encoding = 0;


        for (i in 0 until maxChiWidth) {
            val index = i + offset

            if (index < query.length && query[index] == character) {
                encoding = encoding or marker
            }

            marker = marker shr 1
        }

        return encoding
    }

    private fun getTransition(stateKey: StateKey, chiVector: Int): List<State> {

        if (stateKey.allStates.isEmpty()) {
            return emptyList()
        }

        val nextStates = HashSet<State>()

        for (state in stateKey.allStates) {
            // The maximum value of offset will always be 2D

            if (D - state.dUsed > 0) {
                // We have D to perform deletion and substitution

                // Deletion
                nextStates.add(State(offset = state.offset, dUsed = state.dUsed + 1))

                // substitution
                nextStates.add(State(offset = state.offset + 1, dUsed = state.dUsed + 1))
            }

            for (i in 0..(D - state.dUsed)) {
                val dGettingUsed = i

                val letterMask = 1 shl (maxChiWidth - 1 - i - state.offset)
                val isLetterMatching = chiVector and letterMask != 0

                if (isLetterMatching) {
                    // Addition
                    nextStates.add(State(offset = state.offset + i + 1, dUsed = state.dUsed + dGettingUsed))
                }
            }
        }

        return nextStates.toList()
    }

    private fun normalise(allStates: List<State>): NormalisedStateKey {
        if (allStates.isEmpty()) {
            return NormalisedStateKey(0, StateKey(emptyList()))
        }

        val minOffset = allStates.minOf { it.offset }
        val normalisedStates =
            allStates.map { state -> State(offset = state.offset - minOffset, dUsed = state.dUsed) }
                .distinct()
                .sortedWith(compareBy<State> { it.offset }.thenBy { it.dUsed })

        return NormalisedStateKey(shift = minOffset, nextStateKey = StateKey(normalisedStates))
    }
}