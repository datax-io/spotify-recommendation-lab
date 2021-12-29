package io.datax.shared

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow

abstract class Changeable {

    private val changeChannel = BroadcastChannel<Unit>(Channel.CONFLATED)
    internal val changeFlow get() = changeChannel.asFlow()

    internal fun notifyChanged() = changeChannel.offer(Unit)

}
