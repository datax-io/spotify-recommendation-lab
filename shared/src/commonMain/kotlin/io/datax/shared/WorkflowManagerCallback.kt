package io.datax.shared

interface WorkflowManagerCallback {

    fun onSpotifyUserChanged(user: SpotifyUser?)

    fun onSpotifyHistoryStatusChanged(status: SpotifyHistoryStatus?)

    fun onParcelUserChanged(user: ParcelUser?)

    fun onTrainingReadinessChanged(ready: Boolean)

}
