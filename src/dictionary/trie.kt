package dictionary

class TrieNode(val key: Char?, var isTerminal: Boolean) {
    val children: HashMap<Char, TrieNode> = HashMap()

    override fun toString(): String {
        return children.toString()

    }
}

class TrieTree() {
    val root = TrieNode(null, false)

    fun addWord(word: String) {
        var currentNode = root

        for ((i, character) in word.withIndex()) {
            val isTerminal = i == word.length - 1
            val possibleChildrenNode = currentNode.children[character]

            if (possibleChildrenNode == null) {
                val newNode = TrieNode(character, isTerminal)
                currentNode.children[character] = newNode
                currentNode = newNode


                continue
            }

            if (isTerminal) {
                possibleChildrenNode.isTerminal = true
            }
            currentNode = possibleChildrenNode
        }
    }


    /**
     * Finds maximum 100 words which matches the provided prefix
     */
    fun prefixSearch(prefix: String): List<String> {
        var currentNode: TrieNode? = root
        val output = mutableListOf<String>()

        for (character in prefix) {
            require(currentNode != null, { "unreachable" })

            val node = currentNode.children[character]

            if (node == null) {
                currentNode = null
                break
            }

            currentNode = node
        }

        if (currentNode == null) {
            // We could not find any words which matches the existing prefix
            return output
        }

        if (currentNode.isTerminal) {
            // Add current word as matching word
            output.add(prefix)
        }

        findAllEndingWords(currentNode, output, prefix, 100)

        return output
    }

    private fun findAllEndingWords(
        node: TrieNode,
        output: MutableList<String>,
        prefix: String,
        maxOutput: Int,
    ) {

        for ((character, node) in node.children.entries) {
            val newPrefix = "${prefix}${character}"

            if (node.isTerminal) {
                output.add(newPrefix)
            }

            if (output.size >= maxOutput) {
                break
            }

            findAllEndingWords(node, output, newPrefix, maxOutput)
        }
    }


    fun prettyPrint() {
        prettyPrintHelper(root, "", true)
    }

    private fun prettyPrintHelper(node: TrieNode, prefix: String, isLast: Boolean) {
        val connector = if (isLast) "└── " else "├── "
        val nodeDisplay = if (node.key != null) {
            "${node.key}${if (node.isTerminal) " (word)" else ""}"
        } else {
            "root"
        }

        println("$prefix$connector$nodeDisplay")

        val children = node.children.toList().sortedBy { it.first }
        for ((index, child) in children.withIndex()) {
            val isLastChild = index == children.size - 1
            val newPrefix = prefix + if (isLast) "    " else "│   "
            prettyPrintHelper(child.second, newPrefix, isLastChild)
        }
    }

    override fun toString(): String {
        return root.toString()
    }
}
