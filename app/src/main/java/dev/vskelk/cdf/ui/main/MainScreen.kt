package dev.vskelk.cdf.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // ⚡ FIX: Necesario para el delegado 'by'
import androidx.compose.runtime.setValue // ⚡ FIX: Necesario para el delegado 'by'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.vskelk.cdf.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSimulator: () -> Unit,
    onNavigateToDiagnosis: () -> Unit,
    onNavigateToInvestigator: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vespa", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VespaBackground,
                    titleContentColor = VespaOnSurface
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = VespaOnSurfaceMid)
                    }
                }
            )
        },
        containerColor = VespaBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CorpusStatusCard(uiState.corpusVersion, uiState.pendientesInvestigador)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleCard("Simulador", Icons.Default.Quiz, onNavigateToSimulator, Modifier.weight(1f))
                    ModuleCard("Diagnóstico", Icons.Default.Analytics, onNavigateToDiagnosis, Modifier.weight(1f))
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Consulta al experto...", color = VespaOnSurfaceLow) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(onClick = onNavigateToInvestigator, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir Investigador")
                }
            }

            if (uiState.recientes.isNotEmpty()) {
                item { Text("Sesiones Recientes", color = VespaOnSurfaceMid) }
                items(uiState.recientes) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun CorpusStatusCard(version: String, pending: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = VespaSurface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Corpus v$version", color = VespaOnSurface)
                if (pending > 0) Text("$pending pendientes", color = VespaWarning, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.CheckCircle, null, tint = VespaSuccess)
        }
    }
}

@Composable
private fun ModuleCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = VespaSurface)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = VespaPrimary)
            Text(title, color = VespaOnSurface, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = VespaSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text("Sesión · ${session.modulo}", color = VespaOnSurface, modifier = Modifier.weight(1f))
            Text("${session.correctos}/${session.total}", color = VespaOnSurfaceMid)
        }
    }
}
