import dictionary.createWordDictionary

fun main() {
    val wordDictionary = createWordDictionary()

    val output = wordDictionary.prefixSearch("hell")

    println(output)
}