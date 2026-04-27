package dev.vskelk.cdf.ui.investigator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    var query by remember { mutableStateOf("") }
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ingestionState by viewModel.ingestionState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.ingestPdf(it) }
    }

    val filters = listOf(
        "Todas", "Estructura", "Proceso", "Organización",
        "Justicia", "Resultados", "Razonamiento Lógico-Matemático", "Comprensión Lectora"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.investigator_title))
                        Text(
                            text = "Bozal de Hierro activo",
                            style = MaterialTheme.typography.labelSmall,
                            color = VespaPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VespaBackground,
                    titleContentColor = VespaOnSurface
                )
            )
        },
        containerColor = VespaBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Filtros por área
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VespaPrimary,
                            selectedLabelColor = VespaOnPrimary,
                            labelColor = VespaOnSurfaceMid
                        )
                    )
                }
            }

            // Campo de consulta
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.investigator_query_hint),
                        color = VespaOnSurfaceLow
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VespaPrimary,
                    unfocusedBorderColor = VespaOutline,
                    cursorColor = VespaPrimary,
                    focusedTextColor = VespaOnSurface,
                    unfocusedTextColor = VespaOnSurface
                ),
                trailingIcon = {
                    IconButton(onClick = { /* Lógica de investigación IA */ }) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar", tint = VespaPrimary)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Ingesta
            Text(
                text = "INGESTA DE DOCUMENTOS PDF",
                style = MaterialTheme.typography.labelLarge,
                color = VespaOnSurfaceLow,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = VespaSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { filePickerLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VespaPrimary)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar PDF del temario")
                    }

                    when (val state = ingestionState) {
                        is IngestionState.Processing -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VespaPrimary)
                            Text(
                                text = "Procesando: ${state.fileName} (${state.blocks} bloques...)",
                                style = MaterialTheme.typography.bodySmall,
                                color = VespaOnSurfaceMid,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        is IngestionState.Success -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VespaSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Completado: ${state.fileName} (${state.totalBlocks} bloques)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VespaSuccess
                                )
                            }
                        }
                        is IngestionState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección Corpus
            Text(
                text = "CORPUS OFICIAL HABILITADO",
                style = MaterialTheme.typography.labelLarge,
                color = VespaOnSurfaceLow,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = VespaSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf("LEGIPE", "Reglamento INE", "Acuerdos CG", "TEPJF", "Constitución").forEach { fuente ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = VespaPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = fuente, style = MaterialTheme.typography.bodyMedium, color = VespaOnSurface)
                        }
                    }
                }
            }
        }
    }
}
