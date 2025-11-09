import dictionary.createWordDictionary
import levensteinAutomata.v7.LevenshteinAutomata
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureNanoTime
import levensteinAutomata.v3.fuzzySearchTrieTree as fuzzySearchV3
import levensteinAutomata.v7.fuzzySearchTrie as fuzzySearchV7

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

data class AutomataBuild(
    val automata: LevenshteinAutomata,
    val buildNs: Long,
)

fun main() {
    val maxWords = 20
    val distances = listOf(1, 2, 3)

    println("Precomputing v7 automata (construction times excluded from benchmarks)...")
    val automataByDistance = distances.associateWith { distance ->
        var automata: LevenshteinAutomata? = null
        val buildNs = measureNanoTime {
            automata = LevenshteinAutomata(distance)
        }
        println("  D=$distance: ${"%.2f".format(buildNs / 1_000_000.0)} ms")
        AutomataBuild(automata = automata!!, buildNs = buildNs)
    }
    println()

    println("Loading dictionary and queries...")
    val trie = createWordDictionary()
    val queries = Files.readAllLines(Paths.get("data/queries.txt"))
        .map(String::trim)
        .filter { it.isNotEmpty() }
        .subList(0, 10_000)

    println("Dictionary loaded. Total queries: ${queries.size}\n")

    println("Performing warm-up runs (excluded from timings)...")
    distances.forEach { d ->
        val v7Automata = automataByDistance.getValue(d).automata
        queries.take(50).forEach { query ->
            fuzzySearchV3(trie, query, d, maxWords)
            fuzzySearchV7(trie, query, v7Automata, maxWords)
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

        val v7Automata = automataByDistance.getValue(d).automata
        queriesCompleted = 0
        val v7Time = measureNanoTime {
            queries.forEach { query ->
                fuzzySearchV7(trie, query, v7Automata, maxWords)

                queriesCompleted++
                if (queriesCompleted % 100 == 0 || queriesCompleted == queries.size) {
                    println("  v7: $queriesCompleted/${queries.size} queries completed")
                }
            }
        }
        results.add(BenchmarkResult("v7", d, v7Time, queries.size))

        println("Distance $d benchmarking complete.\n")
    }

    printResults(results, maxWords, automataByDistance.mapValues { it.value.buildNs })
}

fun printResults(
    results: List<BenchmarkResult>,
    maxWords: Int,
    buildTimesNs: Map<Int, Long>,
) {
    println("=".repeat(80))
    println("Benchmark Results (maxWords=$maxWords, queries=${results.first().queriesCount})")
    println("=".repeat(80))
    println()
    println("DFA construction times (excluded from benchmarks):")
    buildTimesNs.toSortedMap().forEach { (distance, ns) ->
        println("  D=$distance: ${"%.2f".format(ns / 1_000_000.0)} ms")
    }
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
            val v7Result = resultsForD.find { it.algorithm == "v7" }
            if (v3Result != null && v7Result != null) {
                val percentDiff = ((v7Result.totalNs - v3Result.totalNs).toDouble() / v3Result.totalNs) * 100
                val comparison = if (percentDiff > 0) {
                    "v7 is %.1f%% slower than v3".format(percentDiff)
                } else {
                    "v7 is %.1f%% faster than v3".format(-percentDiff)
                }
                println("    → $comparison")
            }
        }
        println()
    }

    println("=".repeat(80))
}
