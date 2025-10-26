import dictionary.createWordDictionary
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureNanoTime
import levensteinAutomata.v3.fuzzySearchTrieTree as fuzzySearchV3
import levensteinAutomata.v4.fuzzySearchTrieTree as fuzzySearchV4

data class BenchmarkResult(
    val algorithm: String,
    val distance: Int,
    val totalNs: Long,
    val queriesCount: Int
) {
    val totalMs: Double = totalNs / 1_000_000.0
    val nsPerQuery: Long = totalNs / queriesCount
    val queriesPerSecond: Long = (queriesCount * 1_000_000_000L) / totalNs
}

fun main() {
    val maxWords = 20
    val distances = listOf(1, 2, 3)

    println("Loading dictionary and queries...")
    val trie = createWordDictionary()
    val queries = Files.readAllLines(Paths.get("data/queries.txt"))
        .map(String::trim)
        .filter { it.isNotEmpty() }
        .subList(0, 10_000)

    println("Dictionary loaded. Total queries: ${queries.size}\n")

    println("Performing warm-up runs (excluded from timings)...")
    distances.forEach { d ->
        queries.take(50).forEach { query ->
            fuzzySearchV3(trie, query, d, maxWords)
            fuzzySearchV4(trie, query, d, maxWords)
        }
    }
    println("Warm-up complete.\n")

    val results = mutableListOf<BenchmarkResult>()

    println("Running benchmarks...\n")
    distances.forEach { d ->
        println("Benchmarking distance $d...")

        var queriesCompleted = 0
        val v3Time = measureNanoTime {
            queries.forEach { query ->
                fuzzySearchV3(trie, query, d, maxWords)
                queriesCompleted++
                if (queriesCompleted % 100 == 0 || queriesCompleted == queries.size) {
                    println("  v3: $queriesCompleted/${queries.size} queries completed")
                }
            }
        }
        results.add(BenchmarkResult("v3", d, v3Time, queries.size))

        queriesCompleted = 0
        val v4Time = measureNanoTime {
            queries.forEach { query ->
                fuzzySearchV4(trie, query, d, maxWords)

                queriesCompleted++
                if (queriesCompleted % 100 == 0 || queriesCompleted == queries.size) {
                    println("  v4: $queriesCompleted/${queries.size} queries completed")
                }
            }
        }
        results.add(BenchmarkResult("v4", d, v4Time, queries.size))

        println("Distance $d benchmarking complete.\n")
    }

    printResults(results, maxWords)
}

fun printResults(results: List<BenchmarkResult>, maxWords: Int) {
    println("=".repeat(80))
    println("Benchmark Results (maxWords=$maxWords, queries=${results.first().queriesCount})")
    println("=".repeat(80))
    println()
    println("%-12s %-6s %-15s %-18s %-18s".format("Algorithm", "D", "Total (ms)", "ns/query", "Throughput (q/s)"))
    println("-".repeat(80))

    val groupedByDistance = results.groupBy { it.distance }

    groupedByDistance.forEach { (d, resultsForD) ->
        resultsForD.forEach { result ->
            println(
                "%-12s %-6s %-15.2f %-18s %-18s".format(
                    result.algorithm,
                    "D=$d",
                    result.totalMs,
                    "%,d".format(result.nsPerQuery),
                    "%,d".format(result.queriesPerSecond)
                )
            )
        }

        if (resultsForD.size == 2) {
            val v3Result = resultsForD.find { it.algorithm == "v3" }
            val v4Result = resultsForD.find { it.algorithm == "v4" }
            if (v3Result != null && v4Result != null) {
                val percentDiff = ((v4Result.totalNs - v3Result.totalNs).toDouble() / v3Result.totalNs) * 100
                val comparison = if (percentDiff > 0) {
                    "v4 is %.1f%% slower than v3".format(percentDiff)
                } else {
                    "v4 is %.1f%% faster than v3".format(-percentDiff)
                }
                println("    → $comparison")
            }
        }
        println()
    }

    println("=".repeat(80))
}
