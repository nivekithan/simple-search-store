package dictionary

import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.forEachLine

fun createWordDictionary(): TrieTree {
    val tree = TrieTree()
    val cwd = Path("").absolute()

    println("cwd=${cwd}")

    val wordPath = Path("./data/actual_words.txt")


    wordPath.forEachLine(Charsets.UTF_8, { line ->
        // Adds each word to dictionary
        tree.addWord(line.trim())
    })

    return tree
}