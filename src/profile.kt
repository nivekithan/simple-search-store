import dictionary.createWordDictionary
import levensteinAutomata.v7.LevenshteinAutomata
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.measureTime

fun main() {

    val maxWords = 20

    println("Loading dictionary and queries...")
    val trie = createWordDictionary()
    val queries = Files.readAllLines(Paths.get("data/queries.txt"))
        .map(String::trim)
        .filter { it.isNotEmpty() }
        .subList(0, 10_000)



    println("Running benchmarks...\n")

    val d = 5
    val duration = measureTime {

        val automata = LevenshteinAutomata(d)
    }

    println("Constructing D=${d} took ${duration.inWholeMilliseconds}ms")


    return

//    var queriesCompleted = 0
//    queries.forEach { query ->
//        levensteinAutomata.v7.fuzzySearchTrieTree(trie, query, automata, maxWords)
//        queriesCompleted++
//        if (queriesCompleted % 100 == 0 || queriesCompleted == queries.size) {
//            println("  v4: $queriesCompleted/${queries.size} queries completed")
//        }
//    }
//
//
//    println("Distance $d benchmarking complete.\n")
}