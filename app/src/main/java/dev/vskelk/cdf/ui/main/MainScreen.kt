package dev.vskelk.cdf.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.vskelk.cdf.R
import dev.vskelk.cdf.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSimulator: () -> Unit,
    onNavigateToDiagnosis: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToInvestigator: () -> Unit,
    onNavigateToQuarantine: () -> Unit,
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
                        Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = VespaOnSurfaceMid)
                    }
                    IconButton(onClick = { /* Modo Offline toggle */ }) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = VespaOnSurfaceMid)
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
                CorpusStatusCard(version = uiState.corpusVersion, pendingItems = uiState.pendientesInvestigador)
            }

            item {
                Text("Módulos de Estudio", style = MaterialTheme.typography.titleMedium, color = VespaOnSurfaceMid)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleCard("Simulador", Icons.Default.Quiz, onNavigateToSimulator, Modifier.weight(1f))
                    ModuleCard("Diagnóstico", Icons.Default.Analytics, onNavigateToDiagnosis, Modifier.weight(1f))
                    ModuleCard("Entrevista", Icons.Default.Mic, onNavigateToInterview, Modifier.weight(1f))
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Consulta al motor experto...", color = VespaOnSurfaceLow) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VespaOutline,
                        unfocusedBorderColor = VespaOutline,
                        focusedTextColor = VespaOnSurface,
                        unfocusedTextColor = VespaOnSurface
                    ),
                    trailingIcon = {
                        IconButton(onClick = { /* Acción de búsqueda */ }) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = VespaOnSurfaceMid)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToInvestigator,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VespaOnSurface)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Investigador")
                }
            }

            if (uiState.recientes.isNotEmpty()) {
                item { Text("Resultados Recientes", style = MaterialTheme.typography.titleMedium, color = VespaOnSurfaceMid) }
                items(uiState.recientes) { session -> SessionCard(session = session) }
            }
        }
    }
}

@Composable
private fun CorpusStatusCard(version: String, pendingItems: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = VespaSurface), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Corpus v$version", style = MaterialTheme.typography.bodyMedium, color = VespaOnSurface)
                if (pendingItems > 0) Text("$pendingItems pendientes en cuarentena", style = MaterialTheme.typography.bodySmall, color = VespaWarning)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VespaSuccess, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ModuleCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = VespaSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = VespaPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = VespaOnSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = VespaSurface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(VespaPrimary, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Sesión · ${session.modulo}", style = MaterialTheme.typography.bodyMedium, color = VespaOnSurface)
                Text("${session.correctos}/${session.total} correctas", style = MaterialTheme.typography.bodySmall, color = VespaOnSurfaceMid)
            }
        }
    }
}
