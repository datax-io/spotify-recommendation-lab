package io.datax.spotifyrecommendationlab

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.view.View
import android.view.inputmethod.EditorInfo
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
    private lateinit var textViewCurrentSpotifyClient: TextView
    private lateinit var textViewCurrentSpotifyUser: TextView

    private lateinit var buttonFetchHistory: AppCompatButton
    private lateinit var progressFetchHistory: LinearProgressIndicator
    private lateinit var textViewHistoryResult: TextView

    private lateinit var textViewCurrentParcelApp: TextView
    private lateinit var buttonChangeParcelAppId: AppCompatButton
    private lateinit var textViewCurrentParcelClient: TextView
    private lateinit var buttonChangeParcelClientId: AppCompatButton
    private lateinit var buttonAuthorizeParcel: AppCompatButton
    private lateinit var textViewCurrentParcelUser: TextView

    private lateinit var buttonChangeParticipantId: AppCompatButton
    private lateinit var textViewCurrentParticipantId: TextView
    private lateinit var textViewCurrentPygridHost: TextView
    private lateinit var buttonChangePygridHost: AppCompatButton
    private lateinit var textViewCurrentPygridToken: TextView
    private lateinit var buttonChangePygridToken: AppCompatButton

    private lateinit var buttonStartTraining: AppCompatButton

    private lateinit var workflowManager: WorkflowManager<TokenRequest, ByteArray>

    private val databaseDriverFactory = DatabaseDriverFactory(this)

    private lateinit var workflowJob: Job

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
        textViewCurrentSpotifyClient = findViewById(R.id.text_view_current_spotify_client)
        textViewCurrentSpotifyUser = findViewById(R.id.text_view_current_spotify_user)

        buttonFetchHistory = findViewById(R.id.fetch_history)
        buttonFetchHistory.setOnClickListener { fetchHistory() }
        progressFetchHistory = findViewById(R.id.progress_fetch_history)
        textViewHistoryResult = findViewById(R.id.text_view_history_result)

        textViewCurrentParcelApp = findViewById(R.id.text_view_current_parcel_app)
        textViewCurrentParcelClient = findViewById(R.id.text_view_current_parcel_client)

        buttonChangeParcelAppId = findViewById(R.id.button_change_parcel_app_id)
        buttonChangeParcelAppId.setOnClickListener { attemptChangeParcelAppId() }

        buttonChangeParcelClientId = findViewById(R.id.button_change_parcel_client_id)
        buttonChangeParcelClientId.setOnClickListener { attemptChangeParcelClientId() }

        buttonAuthorizeParcel = findViewById(R.id.button_authorize_parcel)
        buttonAuthorizeParcel.setOnClickListener { getParcelAuthorization() }
        textViewCurrentParcelUser = findViewById(R.id.text_view_current_parcel_user)

        textViewCurrentPygridHost = findViewById(R.id.text_view_current_pygrid_host)
        textViewCurrentPygridToken = findViewById(R.id.text_view_current_pygrid_token)

        buttonChangePygridHost = findViewById(R.id.button_change_pygrid_host)
        buttonChangePygridHost.setOnClickListener { attemptChangePygridHost() }

        buttonChangePygridToken = findViewById(R.id.button_change_pygrid_token)
        buttonChangePygridToken.setOnClickListener { attemptChangePygridToken() }

        buttonChangeParticipantId = findViewById(R.id.button_change_participant_id)
        buttonChangeParticipantId.setOnClickListener { attemptChangeSpotifyParticipantId() }
        textViewCurrentParticipantId = findViewById(R.id.text_view_current_participant_id)

        buttonStartTraining = findViewById(R.id.button_start_training)

        workflowManager = WorkflowManager(
            preferences = this,
            databaseDriverFactory = databaseDriverFactory,
            openIDHelperDelegate = OpenIDHelper,
            formDataUploadDelegate = byteArrayUploader,
        )

        refreshUIValues()

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

    @SuppressLint("SetTextI18n")
    private fun refreshUIValues() {

        workflowManager.spotifyHelper.also {
            textViewCurrentSpotifyClient.text = when (val value = it.clientId) {
                null -> "Client ID not set"
                else -> "Client ID: $value"
            }
            textViewCurrentSpotifyUser.text = when (val value = it.user?.displayName ?: it.user?.id) {
                null -> "Not authorized"
                else -> "User: $value"
            }
            buttonAuthorizeSpotify.isEnabled = it.clientId != null
            buttonFetchHistory.isEnabled = it.user != null
        }

        workflowManager.spotifyHistoryFetcher.also {
            textViewHistoryResult.text = "${it.getStatus().trackCount} tracks"
            progressFetchHistory.alpha = when (it.getStatus().trackCount) {
                null -> 1f
                else -> 0f
            }
        }

        workflowManager.parcelHelper.also {
            textViewCurrentParcelApp.text = when (val value = it.appId) {
                null -> "App ID not set"
                else -> "App ID: $value"
            }
            textViewCurrentParcelClient.text = when (val value = it.clientId) {
                null -> "Client ID not set"
                else -> "Client ID: $value"
            }
            textViewCurrentParcelUser.text = when (val value = it.user?.id) {
                null -> "Not authorized"
                else -> "User ID: $value"
            }
            buttonAuthorizeParcel.isEnabled = it.appId != null && it.clientId != null
        }

        workflowManager.pygridHelper.also {
            textViewCurrentParticipantId.text = "Participant ID: ${it.participantId}"
            textViewCurrentPygridHost.text = when (val value = it.host) {
                null -> "Host not set"
                else -> "Host: $value"
            }
            textViewCurrentPygridToken.text = when (it.authToken) {
                null -> "Auth token not set"
                else -> "Auth token set"
            }
        }

        buttonStartTraining.isEnabled = workflowManager.ready
    }

    private fun attemptChangeSpotifyClientId() {
        promptForInput("Spotify Client ID", workflowManager.spotifyHelper.clientId ?: "") {
            when (it) {
                workflowManager.spotifyHelper.clientId, "" -> return@promptForInput
                else -> {
                    println("In attempt Client ID: $it")
                    workflowManager.spotifyHelper.clientId = it
                }
            }
        }
    }

    private fun attemptChangeSpotifyParticipantId() {
        promptForInput("Spotify Participant ID", workflowManager.pygridHelper.participantId.toString()) {
            when (it) {
                workflowManager.pygridHelper.participantId.toString(), "" -> return@promptForInput
                else -> workflowManager.pygridHelper.participantId = it.toInt()
            }
        }
    }

    private fun attemptChangeParcelAppId() {
        promptForInput("Parcel App ID", workflowManager.parcelHelper.appId ?: "") { newValue ->
            newValue
                .takeUnless { it == workflowManager.parcelHelper.appId || it == "" }
                ?.let { workflowManager.parcelHelper.appId = it }
        }
    }

    private fun attemptChangeParcelClientId() {
        promptForInput("Parcel Client ID", workflowManager.parcelHelper.clientId ?: "") { newValue ->
            newValue
                .takeUnless { it == workflowManager.parcelHelper.clientId || it == "" }
                ?.let { workflowManager.parcelHelper.clientId = it }
        }
    }

    private fun attemptChangePygridHost() {
        promptForInput("Pygrid Host", workflowManager.pygridHelper.host ?: "") { newValue ->
            newValue
                .takeUnless { it == workflowManager.pygridHelper.host || it == "" }
                ?.let { workflowManager.pygridHelper.host = it }
        }
    }

    private fun attemptChangePygridToken() {
        promptForInput("Pygrid Auth Token", workflowManager.pygridHelper.authToken ?: "") { newValue ->
            newValue
                .takeUnless { it == workflowManager.pygridHelper.authToken || it == "" }
                ?.let { workflowManager.pygridHelper.authToken = it }
        }
    }

    override fun onDestroy() {
        workflowJob.cancel()
        super.onDestroy()
    }

    private fun listenForChanges() {
        workflowJob = launch {
            workflowManager.changesFlow.collect {
                println("Spotify User: ${workflowManager.spotifyHelper.user}")
                refreshUIValues()
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
        val clientId = workflowManager.parcelHelper.clientId
            ?: throw Exception("No client ID")
        val tokenRequest: TokenRequest = OpenIDHelper.getTokenRequest(clientId, fragment)
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
        textViewHistoryResult.text = "Fetching data..."

        CoroutineScope(Dispatchers.Main).launch {
            kotlin.runCatching { workflowManager.fetchHistory() }
        }
    }

    private fun promptForInput(title: String, value: String, valueHandler: (String) -> Unit) {
        val context = this
        val input = EditText(context)
        if (title == "Spotify Participant ID") {
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.inputFilterNumberRange(1..24)
        } else {
            input.inputType = InputType.TYPE_CLASS_TEXT
        }
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

    fun EditText.inputFilterNumberRange(range: IntRange) {
        filterMin(range.first)
        filters = arrayOf<InputFilter>(InputFilterMax(range.last))
    }

    class InputFilterMax(private var max: Int) : InputFilter {

        override fun filter(
            p0: CharSequence, p1: Int, p2: Int, p3: Spanned?, p4: Int, p5: Int,
        ): CharSequence? {
            try {
                val replacement = p0.subSequence(p1, p2).toString()
                val newVal = p3.toString().substring(0, p4) + replacement + p3.toString()
                    .substring(p5, p3.toString().length)
                val input = newVal.toInt()
                if (input <= max) return null
            } catch (e: NumberFormatException) {
            }
            return ""
        }
    }

    fun EditText.filterMin(min: Int) {
        onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (!b) {
                setTextMin(min)
            }
        }
        setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setTextMin(min)
            }
            false
        }
    }

    fun EditText.setTextMin(min: Int) {
        try {
            val value = text.toString().toInt()
            if (value < min) {
                setText("$min")
            }
        } catch (e: Exception) {
            setText("$min")
        }
    }

}
