import os, re
BASE = "/root/Spen-Vespa/app/src/main/java/dev/vskelk/cdf"

# 1. DatabaseModule.kt - Añadir provideOntologyDao
path = f"{BASE}/app/di/DatabaseModule.kt"
c = open(path).read()
if 'provideOntologyDao' not in c:
    c = c.replace("// ===== DAOs =====", """// ===== DAOs =====

    @Provides
    @Singleton
    fun provideOntologyDao(database: AppDatabase): OntologyDao = database.ontologyDao()""")
if 'import dev.vskelk.cdf.core.database.dao.OntologyDao' not in c:
    c = c.replace('import dev.vskelk.cdf.core.database.dao.*', 'import dev.vskelk.cdf.core.database.dao.*\nimport dev.vskelk.cdf.core.database.dao.OntologyDao')
open(path,'w').write(c)
print("1/5 OK: DatabaseModule")

# 2. core/di/DatabaseModule.kt si existe
path2 = f"{BASE}/core/di/DatabaseModule.kt"
if os.path.exists(path2):
    c2 = open(path2).read()
    if 'provideOntologyDao' not in c2:
        c2 = c2.replace("// ===== DAOs =====", """// ===== DAOs =====

    @Provides
    @Singleton
    fun provideOntologyDao(database: AppDatabase): OntologyDao = database.ontologyDao()""")
    if 'import dev.vskelk.cdf.core.database.dao.OntologyDao' not in c2:
        c2 = c2.replace('import dev.vskelk.cdf.core.database.dao.*', 'import dev.vskelk.cdf.core.database.dao.*\nimport dev.vskelk.cdf.core.database.dao.OntologyDao')
    open(path2,'w').write(c2)
    print("2/5 OK: core/di/DatabaseModule")
else:
    print("2/5 SKIP: core/di/DatabaseModule no existe")

# 3. Repositories.kt - DomainState
path3 = f"{BASE}/core/data/repository/Repositories.kt"
c3 = open(path3).read()
if 'object DomainState' not in c3:
    c3 = c3.replace('@Singleton\nclass OntologyRepositoryImpl', '''@Singleton

object DomainState {
    const val NO_VISTO = "NO_VISTO"
    const val DOMINADO = "DOMINADO"
    const val INESTABLE = "INESTABLE"
    val estadosDebiles = setOf(NO_VISTO, INESTABLE)
    object Thresholds { const val PRECISION_DOMINADO = 0.7f }
}

class OntologyRepositoryImpl''')
open(path3,'w').write(c3)
print("3/5 OK: Repositories")

# 4. InvestigatorViewModel.kt - CargoEntity con id String
path4 = f"{BASE}/ui/investigator/InvestigatorViewModel.kt"
c4 = open(path4).read()
if 'areaExamen' in c4 or 'nivel' in c4:
    c4 = c4.replace('CargoEntity(\n                        id = 1,', 'CargoEntity(\n                        id = "1",')
    c4 = c4.replace('                        areaExamen = "TECNICO",\n', '')
    c4 = c4.replace('                        nivel = "Distrital",\n', '')
    open(path4,'w').write(c4)
    print("4/5 OK: InvestigatorViewModel")
else:
    print("4/5 SKIP: InvestigatorViewModel ya corregido")

# 5. BootstrapRepositoryImpl.kt - areaExamen en NormativeFragmentEntity
# FIX: usar patrón específico con contexto NormativeFragmentEntity para
# evitar reemplazar la primera ocurrencia incorrecta de certainty
path5 = f"{BASE}/core/data/repository/BootstrapRepositoryImpl.kt"
c5 = open(path5).read()
if 'areaExamen = "TECNICO"' not in c5:
    # Buscar el bloque NormativeFragmentEntity(...) que contenga certainty = ALTA
    patron = r'(NormativeFragmentEntity\([^)]*?certainty = ExtractionCertainty\.ALTA,)'
    match = re.search(patron, c5, re.DOTALL)
    if match:
        original = match.group(0)
        reemplazo = original.replace(
            'certainty = ExtractionCertainty.ALTA,',
            'certainty = ExtractionCertainty.ALTA,\n                    areaExamen = "TECNICO",'
        )
        c5 = c5.replace(original, reemplazo, 1)  # count=1 por seguridad
        open(path5,'w').write(c5)
        print("5/5 OK: BootstrapRepositoryImpl")
    else:
        print("5/5 SKIP: No se encontró NormativeFragmentEntity con certainty=ALTA")
else:
    print("5/5 SKIP: BootstrapRepositoryImpl ya corregido")

print("\nTODO LISTO. Compilando...")
