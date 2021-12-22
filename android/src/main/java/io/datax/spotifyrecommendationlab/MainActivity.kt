package io.datax.spotifyrecommendationlab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.datax.shared.OpenIDHelper
import io.datax.shared.WorkflowManager
import io.datax.shared.byteArrayUploader
import io.datax.shared.repo.DatabaseDriverFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenRequest
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()

    private lateinit var buttonChangeSpotifyClientId: AppCompatButton

    private lateinit var buttonAuthorizeSpotify: AppCompatButton
    private lateinit var textViewCurrentSpotifyUser: TextView

    private lateinit var buttonFetchHistory: AppCompatButton
    private lateinit var progressFetchHistory: LinearProgressIndicator
    private lateinit var textViewHistoryResult: TextView

    private lateinit var buttonAuthorizeParcel: AppCompatButton
    private lateinit var textViewCurrentParcelUser: TextView

    private lateinit var buttonStartTraining: AppCompatButton
    private lateinit var buttonUploadDummyDocument: AppCompatButton

    private lateinit var workflowManager: WorkflowManager<ByteArray>

    private val databaseDriverFactory = DatabaseDriverFactory(this)

    private lateinit var spotifyUserJob: Job
    private lateinit var parcelUserJob: Job
    private lateinit var spotifyHistoryStatusJob: Job
    private lateinit var trainingReadinessJob: Job

    private lateinit var authorizationService: AuthorizationService

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authorizationService = AuthorizationService(this)

        this.title = "Spotify Recommendation Lab"

        buttonChangeSpotifyClientId = findViewById(R.id.button_change_spotify_client_id)
        buttonChangeSpotifyClientId.setOnClickListener { attemptChangeSpotifyClientId() }

        buttonAuthorizeSpotify = findViewById(R.id.button_authorize_spotify)
        buttonAuthorizeSpotify.setOnClickListener { getSpotifyAuthorization() }
        textViewCurrentSpotifyUser = findViewById(R.id.text_view_current_spotify_user)

        buttonFetchHistory = findViewById(R.id.fetch_history)
        buttonFetchHistory.setOnClickListener { fetchHistory() }
        progressFetchHistory = findViewById(R.id.progress_fetch_history)
        textViewHistoryResult = findViewById(R.id.text_view_history_result)

        buttonAuthorizeParcel = findViewById(R.id.button_authorize_parcel)
        buttonAuthorizeParcel.setOnClickListener { getParcelAuthorization() }
        textViewCurrentParcelUser = findViewById(R.id.text_view_current_parcel_user)

        buttonStartTraining = findViewById(R.id.button_start_training)

        buttonUploadDummyDocument = findViewById(R.id.button_upload_dummy_document)
        buttonUploadDummyDocument.setOnClickListener { uploadDummyDocument() }

        workflowManager = WorkflowManager(
            preferences = this,
            databaseDriverFactory = databaseDriverFactory,
            formDataUploadDelegate = byteArrayUploader,
            spotifyClientId = "<TODO>",
            parcelAppId = "<TODO>",
            parcelClientId = "<TODO>",
            pygridHost = "<TODO>",
            pygridAuthToken = "<TODO>",
        )

        listenForChanges()

        launch {
            intent.takeIf { it.action == Intent.ACTION_VIEW }
                ?.data
                ?.also { uri ->
                    println(uri.fragment)
                    when (uri.host) {
                        "spotifyauth" -> workflowManager.spotifyHelper.handleAuthResult(uri.fragment!!)
                        "parcelauth" -> handleParcelAuthResult(uri.fragment!!)
                    }
                }
        }
    }

    private fun attemptChangeSpotifyClientId() {
        promptForInput("Spotify Client ID", workflowManager.spotifyHelper.clientId) {
            when (it) {
                workflowManager.spotifyHelper.clientId, "" -> return@promptForInput
                else -> workflowManager.spotifyHelper.clientId = it
            }
        }
    }

    override fun onDestroy() {
        spotifyUserJob.cancel()
        parcelUserJob.cancel()
        spotifyHistoryStatusJob.cancel()
        trainingReadinessJob.cancel()
        super.onDestroy()
    }

    private fun listenForChanges() {
        spotifyUserJob = launch {
            workflowManager.spotifyHelper.userFlow.collect { user ->
                buttonAuthorizeSpotify.isEnabled = workflowManager.spotifyHelper.clientId.isNotBlank()
                textViewCurrentSpotifyUser.text = when (user) {
                    null -> "Unauthorized"
                    else -> "User: ${user.displayName ?: user.id}"
                }
                buttonFetchHistory.isEnabled = user != null
            }
        }

        spotifyHistoryStatusJob = launch {
            workflowManager.spotifyHistoryFetcher.statusFlow.collect { data ->
                textViewHistoryResult.text = when (val itemCount = data?.trackCount) {
                    null -> "No result"
                    else -> "Result: $itemCount tracks"
                }
                progressFetchHistory.alpha = 0f
                buttonFetchHistory.isEnabled = workflowManager.spotifyHelper.userFlow.value != null

                if (data != null && data.trackCount > 0) {
                    println("Data size: ${workflowManager.spotifyHistoryFetcher.trainingData().size}")
                }
            }
        }

        parcelUserJob = launch {
            workflowManager.parcelHelper.userFlow.collect { user ->
                textViewCurrentParcelUser.text = when (user) {
                    null -> "Unauthorized"
                    else -> "User: ${user.id}"
                }
                buttonUploadDummyDocument.isEnabled = user != null
            }
        }

        trainingReadinessJob = launch {
            workflowManager.trainingReadinessFlow.collect { ready ->
                buttonStartTraining.isEnabled = ready
            }
        }

    }

    private fun getSpotifyAuthorization() = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(workflowManager.spotifyHelper.getAuthUrl())
    ).takeIf { it.resolveActivity(packageManager) != null }?.let { startActivity(it) }

    private fun getParcelAuthorization() = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(workflowManager.parcelHelper.getAuthUrl())
    ).takeIf { it.resolveActivity(packageManager) != null }?.let { startActivity(it) }

    private fun handleParcelAuthResult(fragment: String) {
        val tokenRequest = OpenIDHelper.getTokenRequest(workflowManager.parcelHelper.clientId, fragment) as TokenRequest
        authorizationService.performTokenRequest(tokenRequest) { response, ex ->
            if (response == null) {
                println(ex)
                return@performTokenRequest
            }
            val accessToken = response.accessToken
                ?: return@performTokenRequest
            runBlocking { workflowManager.parcelHelper.handleAuthResult(accessToken) }
        }
    }

    private fun fetchHistory() {
        buttonFetchHistory.isEnabled = false
        progressFetchHistory.alpha = 1f
        textViewHistoryResult.text = "Fetching preference data..."

        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching { workflowManager.fetchHistory() }
        }
    }

    private fun uploadDummyDocument() {
        CoroutineScope(Dispatchers.IO).launch {
            workflowManager.parcelHelper.uploadDocument("Test content".toByteArray(), "test.txt")
                .let { println(it) }
        }
    }

    private fun promptForInput(title: String, value: String, valueHandler: (String) -> Unit) {
        val context = this
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(value)
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ -> valueHandler(input.text.toString()) }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
        input.requestFocus()
        input.selectAll()
    }

}
