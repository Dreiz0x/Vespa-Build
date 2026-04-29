package dev.vskelk.cdf.ui.investigator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.vskelk.cdf.R
import dev.vskelk.cdf.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvestigatorViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ingestionState by viewModel.ingestionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.ingestPdf(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.investigator_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VespaBackground, titleContentColor = VespaOnSurface)
            )
        },
        containerColor = VespaBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text(stringResource(R.string.investigator_query_hint)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.investigar() }) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { filePickerLauncher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                Text("Seleccionar PDF")
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = ingestionState) {
                is IngestionState.Processing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is IngestionState.Success -> Text("PDF procesado exitosamente", color = VespaSuccess)
                is IngestionState.Error -> Text(state.message, color = VespaError)
                is IngestionState.Idle -> {}
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = uiState) {
                is InvestigatorUiState.Loading -> CircularProgressIndicator()
                is InvestigatorUiState.Success -> Text(state.message, color = VespaSuccess)
                is InvestigatorUiState.Error -> Text(state.message, color = VespaError)
                is InvestigatorUiState.Idle -> {}
            }
        }
    }
}
