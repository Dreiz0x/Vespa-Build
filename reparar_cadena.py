import os, re

BASE = "/root/Spen-Vespa/app/src/main/java/dev/vskelk/cdf"

# ------------------------------------------------------------
# FASE 1: Reparar AdaptiveRepositoryImpl.kt
# ------------------------------------------------------------
path = f"{BASE}/core/data/repository/AdaptiveRepositoryImpl.kt"
with open(path, 'r') as f:
    c = f.read()

# 1a: completeSession ahora acepta weakSubtemas:String, dominantErrors:String
# Buscar la llamada exacta
old = """studySessionDao.completeSession(
                    sessionId = sessionId,
                    correctos = correctos,
                    tiempoPromedioSeg = tiempoPromedio,
                    weakSubtemas = distinctWeakSubtemas,
                    dominantErrors = dominantErrors
                )"""
new = """studySessionDao.completeSession(
                    sessionId = sessionId,
                    correctos = correctos,
                    tiempoPromedioSeg = tiempoPromedio,
                    weakSubtemas = distinctWeakSubtemas.joinToString(","),
                    dominantErrors = dominantErrors.joinToString(",")
                )"""
c = c.replace(old, new)
with open(path, 'w') as f:
    f.write(c)

# ------------------------------------------------------------
# FASE 2: Reparar BootstrapRepositoryImpl.kt
# ------------------------------------------------------------
path = f"{BASE}/core/data/repository/BootstrapRepositoryImpl.kt"
with open(path, 'r') as f:
    c = f.read()

# 2a: CargoEntity id debe ser String, sin areaExamen ni nivel
old_cargo = """CargoEntity(
                        id = 1,
                        nombre = "VOE",
                        descripcion = "Vocalía de Organización Electoral",
                        areaExamen = "TECNICO",
                        nivel = "Distrital",
                        isDefault = true
                    )"""
new_cargo = """CargoEntity(
                        id = "1",
                        nombre = "VOE",
                        descripcion = "Vocalia de Organizacion Electoral",
                        isDefault = true
                    )"""
c = c.replace(old_cargo, new_cargo)

with open(path, 'w') as f:
    f.write(c)

# ------------------------------------------------------------
# FASE 3: Analizar TODOS los archivos Kotlin en busca de errores predecibles
# ------------------------------------------------------------
errores_futuros = []

# Mapeo de firmas que cambiamos
firmas = {
    'StudySessionEntity': ['sessionId', 'modulo', 'examArea', 'startedAt', 'completedAt', 'correctos', 'totalReactivos', 'tiempoPromedioSeg', 'weakSubtemas', 'dominantErrors'],
    'UserTopicMasteryEntity': ['subtemaId', 'estadoDominio', 'precision', 'totalIntentos', 'velocidadPromedio', 'lastReviewed'],
    'UserGapLogEntity': ['gapId', 'subtemaId', 'errorType', 'reactivoId', 'sessionId', 'timestamp'],
    'OntologyNodeEntity': ['id', 'nodeType', 'name', 'description', 'parentId', 'weight', 'displayOrder', 'isActive', 'updatedAt'],
    'CargoEntity': ['id', 'nombre', 'descripcion', 'isDefault'],
}

# Buscar todas las referencias a estos constructores en TODO el proyecto
for entity, campos in firmas.items():
    # Buscar constructor con parámetros nombrados
    for root_dir, dirs, files in os.walk(f"{BASE}"):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root_dir, file)
                with open(filepath, 'r') as f:
                    lineas = f.readlines()
                for i, linea in enumerate(lineas):
                    if f'{entity}(' in linea and '=' in linea:
                        # Verificar si usa parámetros viejos
                        if entity == 'CargoEntity' and ('areaExamen' in linea or 'nivel' in linea):
                            errores_futuros.append(f"{filepath}:{i+1}: CargoEntity usa areaExamen/nivel (eliminados)")
                        if entity == 'StudySessionEntity' and 'startTime' in linea:
                            errores_futuros.append(f"{filepath}:{i+1}: StudySessionEntity usa startTime (ahora startedAt)")

# ------------------------------------------------------------
# FASE 4: Compilar y capturar salida
# ------------------------------------------------------------
import subprocess
result = subprocess.run(
    ['/root/Spen-Vespa/gradlew', 'clean', 'assembleDebug', '--no-daemon'],
    cwd='/root/Spen-Vespa',
    capture_output=True,
    text=True
)

# Extraer errores reales
errores = []
for line in result.stdout.split('\n') + result.stderr.split('\n'):
    if 'error:' in line or 'Unresolved' in line:
        errores.append(line.strip())

# Mostrar resultados
print("\n=== ERRORES ACTUALES ===")
for e in errores:
    print(e)

print("\n=== ERRORES PREDECIBLES (parámetros viejos) ===")
for e in errores_futuros:
    print(e)

if not errores and not errores_futuros:
    print("\nBUILD EXITOSO - APK generada en app/build/outputs/apk/debug/")
