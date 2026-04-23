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

/**
 * MainScreen - Pantalla principal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSimulator: () -> Unit,
    onNavigateToDiagnosis: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToInvestigator: () -> Unit,
    onNavigateToQuarantine: () -> Unit,
    onNavigateToSettings: () -> Unit, // ⚡ Parámetro nuevo para Settings
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // ⚡ ESTO REVIVE TU TEXTFIELD. Ahora guarda lo que escribes.
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vespa",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VespaBackground,
                    titleContentColor = VespaOnSurface
                ),
                actions = {
                    // Botón de Settings ⚡
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VespaOnSurfaceMid
                        )
                    }
                    // Toggle Offline
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = stringResource(R.string.main_offline_mode),
                            tint = VespaOnSurfaceMid
                        )
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
            // Corpus status
            item {
                CorpusStatusCard(
                    version = uiState.corpusVersion,
                    pendingItems = uiState.pendientesInvestigador
                )
            }

            // Módulos principales
            item {
                Text(
                    text = "Módulos de Estudio",
                    style = MaterialTheme.typography.titleMedium,
                    color = VespaOnSurfaceMid,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModuleCard(
                        title = stringResource(R.string.module_simulator),
                        description = stringResource(R.string.module_simulator_desc),
                        icon = Icons.Default.Quiz,
                        onClick = onNavigateToSimulator,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleCard(
                        title = stringResource(R.string.module_diagnosis),
                        description = stringResource(R.string.module_diagnosis_desc),
                        icon = Icons.Default.Analytics,
                        onClick = onNavigateToDiagnosis,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleCard(
                        title = stringResource(R.string.module_interview),
                        description = stringResource(R.string.module_interview_desc),
                        icon = Icons.Default.Mic,
                        onClick = onNavigateToInterview,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Chips de resumen
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(stringResource(R.string.progress_general, uiState.progresoGeneral))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = VespaPrimary.copy(alpha = 0.1f),
                            labelColor = VespaPrimary,
                            leadingIconContentColor = VespaPrimary
                        )
                    )

                    if (uiState.brechasDetectadas > 0) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(stringResource(R.string.progress_gaps_detected, uiState.brechasDetectadas))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = VespaWarningContainer,
                                labelColor = VespaWarning,
                                leadingIconContentColor = VespaWarning
                            )
                        )
                    }
                }
            }

            // Campo de consulta ⚡
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Consulta al motor experto...",
                            color = VespaOnSurfaceLow
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VespaOutline,
                        unfocusedBorderColor = VespaOutline,
                        cursorColor = VespaPrimary,
                        focusedTextColor = VespaOnSurface,
                        unfocusedTextColor = VespaOnSurface
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { 
                                // TODO: Aquí conectaremos el caso de uso del Investigador
                                if(searchQuery.isNotBlank()) {
                                    // searchQuery = "" // Limpiar si quieres
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = VespaOnSurfaceMid
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Botón Investigador
            item {
                OutlinedButton(
                    onClick = onNavigateToInvestigator,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VespaOnSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VespaOutline)
                    )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.nav_investigator))
                }
            }

            // Resultados recientes
            if (uiState.recientes.isNotEmpty()) {
                item {
                    Text(
                        text = "Resultados Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = VespaOnSurfaceMid,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.recientes) { session ->
                    SessionCard(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CorpusStatusCard(
    version: String,
    pendingItems: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VespaSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.main_corpus_status, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VespaOnSurface
                )
                if (pendingItems > 0) {
                    Text(
                        text = stringResource(R.string.main_pending_items, pendingItems),
                        style = MaterialTheme.typography.bodySmall,
                        color = VespaWarning
                    )
                }
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = VespaSuccess,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VespaSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VespaPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = VespaOnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    val precision = if (session.total > 0) session.correctos.toFloat() / session.total else 0f
    val borderColor = when {
        precision >= 0.8f -> VespaSuccess
        precision >= 0.6f -> VespaWarning
        else -> VespaError
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = VespaSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(borderColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sesión · ${session.modulo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VespaOnSurface
                )
                Text(
                    text = "${session.correctos}/${session.total} correctas",
                    style = MaterialTheme.typography.bodySmall,
                    color = VespaOnSurfaceMid
                )
            }

            Text(
                text = "${(precision * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = borderColor
            )
        }
    }
}
