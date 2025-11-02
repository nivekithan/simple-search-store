package levensteinAutomata.v7

import dictionary.TrieNode
import dictionary.TrieTree
import java.util.*
import kotlin.math.abs

data class State(val offset: Int, val distance: Int)
data class StateKey(val states: List<State>)
private data class Normalized(val shift: Int, val key: StateKey)
private data class Transition(val shift: Int, val next: StateKey)

@ConsistentCopyVisibility
data class AutomataState internal constructor(val globalOffset: Int, internal val key: StateKey)

class LevenshteinAutomata(private val maxD: Int) {
    private val chiWidth: Int = (3 * maxD) + 1
    private val totalChi: Int
    private val chiPatterns: Array<BooleanArray>
    private val canonicalStates = HashMap<StateKey, StateKey>()
    private val transitionsByState = HashMap<StateKey, MutableMap<Int, Transition>>()
    private val initialStateKey: StateKey
    private val initialGlobalOffset: Int

    init {
        require(chiWidth in 0 until Int.SIZE_BITS) {
            "Unsupported edit distance $maxD; chi width $chiWidth is too large"
        }

        totalChi = 1 shl chiWidth
        chiPatterns = Array(totalChi) { encode -> decodeChi(encode) }

        val normalizedInitial = normalize(listOf(State(0, maxD)))
        val canonicalInitial = canonicalize(normalizedInitial.key)
        initialStateKey = canonicalInitial
        initialGlobalOffset = normalizedInitial.shift

        transitionsByState[canonicalInitial] = HashMap()
        val queue = ArrayDeque<StateKey>()
        queue.add(canonicalInitial)

        while (queue.isNotEmpty()) {
            val currentKey = queue.removeFirst()
            val currentTransitions = transitionsByState.getOrPut(currentKey) { HashMap() }

            for (encoding in 0 until totalChi) {
                val chi = chiPatterns[encoding]
                val nextStates = step(currentKey.states, chi)
                val normalized = normalize(nextStates)
                val canonicalNext = canonicalize(normalized.key)

                if (!transitionsByState.containsKey(canonicalNext)) {
                    transitionsByState[canonicalNext] = HashMap()
                    queue.add(canonicalNext)
                }

                currentTransitions[encoding] = Transition(normalized.shift, canonicalNext)
            }
        }
    }

    fun initialState(): AutomataState {
        return AutomataState(initialGlobalOffset, initialStateKey)
    }

    fun step(query: String, state: AutomataState, character: Char): AutomataState? {
        val encoding = characteristicEncoding(query, character, state.globalOffset)
        val transitions = transitionsByState[state.key] ?: return null
        val transition = transitions[encoding] ?: return null
        if (transition.next.states.isEmpty()) {
            return null
        }
        return AutomataState(state.globalOffset + transition.shift, transition.next)
    }

    fun isAccepting(state: AutomataState, queryLength: Int): Boolean {
        val globalOffset = state.globalOffset
        for (s in state.key.states) {
            val absoluteOffset = s.offset + globalOffset
            if (queryLength - absoluteOffset <= maxD) {
                return true
            }
        }
        return false
    }

    fun eval(query: String, input: String): Boolean {
        var currentState = initialState()
        for (character in input) {
            currentState = step(query, currentState, character) ?: return false
        }
        return isAccepting(currentState, query.length)
    }

    private fun canonicalize(key: StateKey): StateKey {
        return canonicalStates.getOrPut(key) { key }
    }

    private fun normalize(states: List<State>): Normalized {
        if (states.isEmpty()) {
            return Normalized(0, StateKey(emptyList()))
        }

        val minOffset = states.minOf { it.offset }
        val normalizedStates = states.map { state ->
            State(state.offset - minOffset, state.distance)
        }.sortedWith(compareBy<State> { it.offset }.thenBy { it.distance })

        return Normalized(minOffset, StateKey(normalizedStates))
    }

    private fun step(states: List<State>, chi: BooleanArray): List<State> {
        if (states.isEmpty()) {
            return emptyList()
        }

        val nextStates = LinkedHashSet<State>(states.size * 3)

        for (state in states) {
            val offset = state.offset
            val distance = state.distance

            if (distance > 0) {
                nextStates.add(State(offset, distance - 1))
                nextStates.add(State(offset + 1, distance - 1))
            }

            if (offset >= chi.size) {
                continue
            }

            for (index in offset until chi.size) {
                val delta = index - offset
                if (delta > distance) {
                    break
                }
                if (chi[index]) {
                    nextStates.add(State(offset + delta + 1, distance - delta))
                }
            }
        }

        return simplify(nextStates)
    }

    private fun simplify(states: Collection<State>): List<State> {
        if (states.isEmpty()) {
            return emptyList()
        }

        val list = states.toList()
        val useful = ArrayList<State>(list.size)

        outer@ for (candidate in list) {
            for (other in list) {
                if (candidate == other) {
                    continue
                }
                if (other.implies(candidate)) {
                    continue@outer
                }
            }
            useful.add(candidate)
        }

        return useful
    }

    private fun State.implies(other: State): Boolean {
        if (other.distance < 0) {
            return true
        }
        val distanceDifference = this.distance - other.distance
        if (distanceDifference < 0) {
            return false
        }
        return distanceDifference >= abs(other.offset - this.offset)
    }

    private fun decodeChi(encoding: Int): BooleanArray {
        val array = BooleanArray(chiWidth)
        var mask = 1
        var idx = 0
        while (idx < chiWidth) {
            array[idx] = (encoding and mask) != 0
            mask = mask shl 1
            idx++
        }
        return array
    }

    private fun characteristicEncoding(query: String, character: Char, offset: Int): Int {
        var encoding = 0
        var bit = 1
        for (position in 0 until chiWidth) {
            val index = offset + position
            if (index < query.length && query[index] == character) {
                encoding = encoding or bit
            }
            bit = bit shl 1
        }
        return encoding
    }
}

fun fuzzySearchTrieTree(
    tree: TrieTree,
    query: String,
    automata: LevenshteinAutomata,
    maxWords: Int,
): List<String> {
    val results = mutableListOf<String>()
    val initialState = automata.initialState()
    val queryLength = query.length

    fun dfs(node: TrieNode, state: AutomataState, prefix: String) {
        if (results.size >= maxWords) {
            return
        }

        for ((character, child) in node.children.entries) {
            if (results.size >= maxWords) {
                return
            }

            val nextState = automata.step(query, state, character) ?: continue
            val newPrefix = prefix + character

            if (child.isTerminal && automata.isAccepting(nextState, queryLength)) {
                results.add(newPrefix)
                if (results.size >= maxWords) {
                    return
                }
            }

            dfs(child, nextState, newPrefix)
        }
    }

    dfs(tree.root, initialState, "")
    return results
}
