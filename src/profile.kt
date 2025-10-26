import dictionary.createWordDictionary
import java.nio.file.Files
import java.nio.file.Paths

fun main() {

    val maxWords = 20
    val distances = listOf(3)

    println("Loading dictionary and queries...")
    val trie = createWordDictionary()
    val queries = Files.readAllLines(Paths.get("data/queries.txt"))
        .map(String::trim)
        .filter { it.isNotEmpty() }
        .subList(0, 10_000)

    println("Running benchmarks...\n")
    distances.forEach { d ->
        println("Benchmarking distance $d...")

        var queriesCompleted = 0
        queries.forEach { query ->
            levensteinAutomata.v4.fuzzySearchTrieTree(trie, query, d, maxWords)
            queriesCompleted++
            if (queriesCompleted % 100 == 0 || queriesCompleted == queries.size) {
                println("  v4: $queriesCompleted/${queries.size} queries completed")
            }
        }


        println("Distance $d benchmarking complete.\n")
    }
}