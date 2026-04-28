import os
import re

DIRECTORIO = '.'

def analizar_proyecto():
    print("🚀 INICIANDO SUPER LINTER DE KSP & ROOM...\n")
    
    entidades_registradas = set()
    entidades_en_database = set()
    warnings = []
    
    for raiz, _, archivos in os.walk(DIRECTORIO):
        for archivo in archivos:
            if archivo.endswith('.kt'):
                ruta_completa = os.path.join(raiz, archivo)
                
                with open(ruta_completa, 'r', encoding='utf-8') as f:
                    contenido = f.read()
                    
                    # 1. Buscar Entidades
                    if '@Entity' in contenido:
                        match = re.search(r'(?:data\s+)?class\s+(\w+)', contenido)
                        if match:
                            nombre_clase = match.group(1)
                            entidades_registradas.add(nombre_clase)
                            
                        # Buscar variables complejas sin @Ignore (Listas, Sets, etc)
                        lineas = contenido.split('\n')
                        for i, linea in enumerate(lineas):
                            if ('val ' in linea or 'var ' in linea) and not linea.strip().startswith('//'):
                                if 'setOf(' in linea or 'listOf(' in linea or 'List<' in linea:
                                    if '@Ignore' not in lineas[i-1] and '@Ignore' not in linea:
                                        warnings.append(f"⚠️  {archivo}: Tiene una Lista/Set sin @Ignore o TypeConverter -> {linea.strip()}")

                    # 2. Buscar POJOs de Relación (@Relation)
                    if '@Relation' in contenido:
                        print(f"🔗 POJO DE RELACIÓN ENCONTRADO: {archivo}")
                        match_parent = re.search(r'parentColumn\s*=\s*"([^"]+)"', contenido)
                        match_entity = re.search(r'entityColumn\s*=\s*"([^"]+)"', contenido)
                        if match_parent and match_entity:
                            print(f"   -> Verifica que '{match_parent.group(1)}' y '{match_entity.group(1)}' estén perfectamente escritos en sus Entidades.")
                        else:
                            warnings.append(f"❌ {archivo}: La etiqueta @Relation está mal formada o le faltan columnas.")
                        print("-" * 40)

                    # 3. Buscar el AppDatabase (Para ver qué entidades faltan)
                    if '@Database' in contenido:
                        print(f"🗄️  ARCHIVO DATABASE ENCONTRADO: {archivo}")
                        match = re.search(r'entities\s*=\s*\[(.*?)\]', contenido, re.DOTALL)
                        if match:
                            bloque_entidades = match.group(1)
                            # Extraer los nombres limpiando "::class" y espacios
                            limpios = re.findall(r'(\w+)::class', bloque_entidades)
                            entidades_en_database.update(limpios)
                            print(f"   -> Entidades registradas en el DB: {len(limpios)}")
                        print("-" * 40)

    # 4. REPORTE FINAL (Cruzar datos)
    print("\n" + "="*50)
    print("📊 REPORTE DE ERRORES KSP (POSIBLES CAUSAS)")
    print("="*50)
    
    if warnings:
        print("\n🚨 SOSPECHOSOS DE VARIABLES (Requieren @Ignore o Converter):")
        for w in warnings:
            print(w)
            
    print("\n🚨 SOSPECHOSOS DE ENTIDADES NO REGISTRADAS EN LA DATABASE:")
    faltantes = entidades_registradas - entidades_en_database
    if faltantes:
        for f in faltantes:
            print(f"❌ Falta agregar a entities=[...] -> {f}")
    else:
        print("✅ Todas las entidades encontradas están en tu @Database.")
        
    print("\n🚨 SOSPECHOSOS DE NOMBRES INCONSISTENTES:")
    if "CuarentenaFragmentoEntity" not in entidades_registradas and "QuarantineEntity" in entidades_registradas:
        print("❌ CUIDADO: Tienes un archivo 'QuarantineEntity' pero en el log anterior tu DAO buscaba 'CuarentenaFragmentoEntity'. ¡Revisa que el nombre de la clase (data class) coincida con lo que pide el DAO!")

if __name__ == "__main__":
    analizar_proyecto()
