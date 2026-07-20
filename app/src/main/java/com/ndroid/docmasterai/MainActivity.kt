package com.ndroid.docmasterai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docedit.sdk.api.DocumentEditor
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ndroid.docmasterai.premium.PremiumManager
import com.ndroid.docmasterai.scanner.ScannerManager
import com.ndroid.docmasterai.ui.navigation.BottomNavBar
import com.ndroid.docmasterai.ui.navigation.DocMasterNavHost
import com.ndroid.docmasterai.ui.navigation.Screen
import com.ndroid.docmasterai.ui.theme.DocMasterTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.io.path.exists

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var scannerManager: ScannerManager

    @Inject
    lateinit var premiumManager: PremiumManager

    private val mainViewModel: MainViewModel by viewModels()

    /** Which document is currently open in the editor, so its result can be saved back. */
    private var editingDocumentId: String? = null

//    initializing pdfbox here


    private val editorLauncher = DocumentEditor.registerLauncher(this) { result ->
        val docId = editingDocumentId
        editingDocumentId = null
        if (!result.wasCancelled) {
            if (docId != null) {
                mainViewModel.saveEditedDocument(docId, result)
            } else {
                // Opened from external intent, create a new document
                mainViewModel.createNewDocument(result)
            }
        }
    }

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            gmsResult?.let {
                lifecycleScope.launch {
                    scannerManager.onScanResult(it)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type
        
        when (action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { launchEditor(it, type) }
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                uri?.let { launchEditor(it, type) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    // For multiple files, we currently only support images
                    if (type?.startsWith("image/") == true) {
                        editorLauncher.launch(DocumentEditor.Input(images = uris))
                    } else {
                        // If it's PDFs, just open the first one for now
                        launchEditor(uris[0], type)
                    }
                }
            }
        }
    }

    private fun launchEditor(uri: Uri, type: String?) {
        // Ensure we have permission to read the URI
        try {
            applicationContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        if (type?.startsWith("image/") == true) {
            editorLauncher.launch(DocumentEditor.Input(images = listOf(uri)))
        } else if (type == "application/pdf" || uri.toString().endsWith(".pdf", true)) {
            editorLauncher.launch(DocumentEditor.Input(pdf = uri))
        } else {
            if (uri.toString().endsWith(".pdf", true)) {
                editorLauncher.launch(DocumentEditor.Input(pdf = uri))
            } else {
                editorLauncher.launch(DocumentEditor.Input(images = listOf(uri)))
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PDFBoxResourceLoader.init(this)
        
        handleIntent(intent)

        setContent {
            DocMasterTheme {
                DocMasterMainScreen(
                    onStartScan = { scannerManager.startScan(this, scannerLauncher) },
                    onItemClick = { documentId, images, pdf ->
                        editingDocumentId = documentId
                        val isPremium = premiumManager.isPremium.value || 
                            (premiumManager.trialStatus.value is com.ndroid.docmasterai.premium.TrialStatus.Active)
                        
                        val config = com.docedit.sdk.api.EditorConfig(isPremium = isPremium)
                        editorLauncher.launch(DocumentEditor.Input(images = images, pdf = pdf, config = config))
                    }
                )
            }
        }
    }
}

@Composable
fun DocMasterMainScreen(onStartScan: () -> Unit,
                        onItemClick: (documentId: String, images: List<Uri>, pdf: Uri?) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar only on main screens
    val showBottomBar = currentDestination?.let { dest ->
        listOf(
            Screen.Home::class,
            Screen.Scanner::class,
            Screen.PdfTools::class,
            Screen.FileManager::class,
            Screen.Settings::class,
        ).any { dest.hasRoute(it) }
    } ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                BottomNavBar(navController = navController, onScanClick = onStartScan)
            }
        }
    ) { innerPadding ->
        DocMasterNavHost(
            navController = navController,
            onStartScan = onStartScan,
            modifier = Modifier.padding(innerPadding),
            onItemClick = onItemClick
        )
    }
}
