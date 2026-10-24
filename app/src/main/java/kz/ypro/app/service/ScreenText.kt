package kz.ypro.app.service

import android.view.accessibility.AccessibilityNodeInfo

/** Собирает видимые тексты экрана в порядке обхода дерева сверху вниз. */
object ScreenText {

    // Ограничение защищает от зависания на огромных списках.
    private const val MAX_NODES = 400

    fun collect(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { addLast(root) }
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            val label = node.text?.toString()?.takeIf { it.isNotBlank() }
                ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            label?.let(texts::add)
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let(stack::addLast)
            }
        }
        return texts
    }
}
