import os

BASE = "/root/Spen-Vespa/app/src/main/java/dev/vskelk/cdf"

# 1. AdaptiveRepositoryImpl - corregir ErrorType, recordAttemptAndUpdateMastery, completeSession
with open(f"{BASE}/core/data/repository/AdaptiveRepositoryImpl.kt", 'r') as f:
    content = f.read()

# Arreglar ErrorType.fromDistractor (eliminar, no existe)
content = content.replace('val resolvedErrorType = errorType ?: selectedOption?.distractorTipo?.let { ErrorType.fromDistractor(it) }', 'val resolvedErrorType = errorType ?: selectedOption?.distractorTipo')

# Arreglar recordAttemptAndUpdateMastery (firma nueva sin isCorrect/tiempoRespuestaMs)
old_call = '''userMasteryDao.recordAttemptAndUpdateMastery(subtemaId = reactivo.subtemaId, isCorrect = isCorrect, tiempoRespuestaMs = tiempoRespuestaMs)'''
new_call = '''userMasteryDao.recordAttemptAndUpdateMastery(
    subtemaId = reactivo.subtemaId,
    precision = if (isCorrect) 1.0f else 0.0f,
    velocidadPromedio = tiempoRespuestaMs.toFloat(),
    estadoDominio = if (isCorrect) "DOMINADO" else "INESTABLE"
)'''
content = content.replace(old_call, new_call)

# Arreglar completeSession (nuevos parámetros: weakSubtemas como String, dominantErrors como String)
old_complete = '''studySessionDao.completeSession(
    sessionId = sessionId,
    correctos = correctos,
    tiempoPromedioSeg = tiempoPromedio,
    weakSubtemas = distinctWeakSubtemas,
    dominantErrors = dominantErrors
)'''
new_complete = '''studySessionDao.completeSession(
    sessionId = sessionId,
    correctos = correctos,
    tiempoPromedioSeg = tiempoPromedio,
    weakSubtemas = distinctWeakSubtemas.joinToString(","),
    dominantErrors = dominantErrors.joinToString(",")
)'''
content = content.replace(old_complete, new_complete)

with open(f"{BASE}/core/data/repository/AdaptiveRepositoryImpl.kt", 'w') as f:
    f.write(content)
print("OK: AdaptiveRepositoryImpl.kt")

# 2. BootstrapRepositoryImpl - CargoEntity ya no tiene areaExamen ni nivel
with open(f"{BASE}/core/data/repository/BootstrapRepositoryImpl.kt", 'r') as f:
    content = f.read()

# Arreglar CargoEntity (quitar areaExamen y nivel)
old_cargo = '''CargoEntity(
    id = 1,
    nombre = "VOE",
    descripcion = "Vocalía de Organización Electoral",
    areaExamen = "TECNICO",
    nivel = "Distrital",
    isDefault = true
)'''
new_cargo = '''CargoEntity(
    id = "1",
    nombre = "VOE",
    descripcion = "Vocalia de Organizacion Electoral",
    isDefault = true
)'''
content = content.replace(old_cargo, new_cargo)

with open(f"{BASE}/core/data/repository/BootstrapRepositoryImpl.kt", 'w') as f:
    f.write(content)
print("OK: BootstrapRepositoryImpl.kt")

print("\nLISTO. Ejecuta: cd /root/Spen-Vespa && ./gradlew clean assembleDebug --no-daemon 2>&1 | tail -15")
