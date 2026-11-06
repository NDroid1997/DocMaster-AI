package com.docedit.sdk.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.docedit.sdk.api.DocumentEditor
import com.docedit.sdk.presentation.common.ZoomablePage

class EditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val input = try {
            DocumentEditor.readInput(intent)
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Invalid editor input.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        viewModel.load(input)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                BackHandler(enabled = state.busy) { /* Finish writing before allowing navigation. */ }
                LaunchedEffect(state.result) {
                    state.result?.let { result ->
                        setResult(Activity.RESULT_OK, DocumentEditor.resultIntent(result))
                        finish()
                    }
                }
                EditorScreen(state, viewModel, onCancel = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(state: EditorState, viewModel: EditorViewModel, onCancel: () -> Unit) {
    val canEdit = !state.busy && state.pages.isNotEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document editor") },
                navigationIcon = {
                    TextButton(onClick = onCancel, enabled = !state.busy) { Text("Cancel") }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = canEdit) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.preview != null) {
                    key(state.selectedIndex, state.pages.getOrNull(state.selectedIndex)) {
                        ZoomablePage(state.preview.asImageBitmap(), onScaleChange = {})
                    }
                } else if (state.error == null && state.pages.isNotEmpty()) {
                    CircularProgressIndicator()
                }
                if (state.busy) CircularProgressIndicator()
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { viewModel.select(state.selectedIndex - 1) },
                    enabled = canEdit && state.selectedIndex > 0,
                ) { Text("Previous") }
                Text(if (state.pages.isEmpty()) "No pages" else "${state.selectedIndex + 1} / ${state.pages.size}")
                TextButton(
                    onClick = { viewModel.select(state.selectedIndex + 1) },
                    enabled = canEdit && state.selectedIndex < state.pages.lastIndex,
                ) { Text("Next") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = viewModel::rotate, enabled = canEdit) { Text("Rotate") }
                TextButton(onClick = viewModel::remove, enabled = canEdit && state.pages.size > 1) { Text("Delete") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(
                    onClick = { viewModel.move(-1) }, enabled = canEdit && state.selectedIndex > 0,
                ) { Text("Move earlier") }
                TextButton(
                    onClick = { viewModel.move(1) }, enabled = canEdit && state.selectedIndex < state.pages.lastIndex,
                ) { Text("Move later") }
            }
        }
    }
}
