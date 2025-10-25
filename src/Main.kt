import dictionary.createWordDictionary
import levensteinAutomata.v2.fuzzySearchTrieTree


fun main() {
    val dictionary = createWordDictionary()

    val output = fuzzySearchTrieTree(dictionary, "hell", 2, 100);

    println(output)
}