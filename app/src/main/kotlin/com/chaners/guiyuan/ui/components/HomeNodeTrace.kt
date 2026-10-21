package com.chaners.guiyuan.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.OnPlacedModifierNode
import com.chaners.guiyuan.system.HomePressTrace

// Diagnostic-only markers: no pointer, semantics or drawing behavior.
internal fun Modifier.homeNodeTrace(tag: String, sourceId: Int): Modifier =
    this.then(HomeNodeTraceElement(tag, sourceId))

private data class HomeNodeTraceElement(
    val tag: String,
    val sourceId: Int,
) : ModifierNodeElement<HomeNodeTraceNode>() {
    override fun create(): HomeNodeTraceNode = HomeNodeTraceNode(tag, sourceId)

    override fun update(node: HomeNodeTraceNode) {
        HomePressTrace.record("modifier.update tag=$tag source=$sourceId node=${node.id}")
        node.tag = tag
        node.sourceId = sourceId
    }
}

private class HomeNodeTraceNode(
    var tag: String,
    var sourceId: Int,
) : Modifier.Node(), OnPlacedModifierNode {
    val id = System.identityHashCode(this)
    private var coordinatesId = 0

    override fun onAttach() {
        HomePressTrace.record("modifier.attach tag=$tag source=$sourceId node=$id")
    }

    override fun onDetach() {
        HomePressTrace.record("modifier.detach tag=$tag source=$sourceId node=$id")
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val current = System.identityHashCode(coordinates)
        if (coordinatesId != current) {
            coordinatesId = current
            HomePressTrace.record(
                "modifier.placed tag=$tag node=$id coordinates=$current " +
                    "size=${coordinates.size.width}x${coordinates.size.height}",
            )
        }
    }
}
