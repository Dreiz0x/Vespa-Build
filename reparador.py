import os
import re

def arreglar_app_database():
    ruta_db = None
    # Buscar AppDatabase.kt
    for raiz, _, archivos in os.walk('.'):
        if 'AppDatabase.kt' in archivos:
            ruta_db = os.path.join(raiz, 'AppDatabase.kt')
            break
            
    if not ruta_db:
        print("❌ No encontré AppDatabase.kt")
        return

    with open(ruta_db, 'r', encoding='utf-8') as f:
        contenido = f.read()

    # Buscar el bloque de entities = [...]
    match = re.search(r'(entities\s*=\s*\[)(.*?)(\])', contenido, re.DOTALL)
    if match:
        prefijo = match.group(1)
        entidades_crudas = match.group(2)
        sufijo = match.group(3)
        
        # Limpiar y quitar duplicados conservando el orden original
        lista_entidades =[e.strip() for e in entidades_crudas.split(',') if e.strip()]
        entidades_unicas =[]
        vistos = set()
        
        for e in lista_entidades:
            if e not in vistos:
                entidades_unicas.append(e)
                vistos.add(e)
                
        # Reconstruir el texto
        nuevo_bloque = ",\n        ".join(entidades_unicas)
        texto_final = f"{prefijo}\n        {nuevo_bloque}\n    {sufijo}"
        
        contenido_arreglado = contenido[:match.start()] + texto_final + contenido[match.end():]
        
        with open(ruta_db, 'w', encoding='utf-8') as f:
            f.write(contenido_arreglado)
        print("✅ AppDatabase.kt: ¡Duplicados eliminados con éxito!")

def arreglar_daos():
    ruta_dao = None
    for raiz, _, archivos in os.walk('.'):
        if 'UserMasteryDao.kt' in archivos:
            ruta_dao = os.path.join(raiz, 'UserMasteryDao.kt')
            break
            
    if not ruta_dao:
        print("❌ No encontré UserMasteryDao.kt")
        return
        
    with open(ruta_dao, 'r', encoding='utf-8') as f:
        contenido = f.read()
        
    # Reemplazar palabra exacta "user_gap_log" por "user_gap_logs"
    contenido_arreglado = re.sub(r'\buser_gap_log\b', 'user_gap_logs', contenido)
    
    with open(ruta_dao, 'w', encoding='utf-8') as f:
        f.write(contenido_arreglado)
    print("✅ UserMasteryDao.kt: ¡Tablas corregidas al plural (user_gap_logs)!")

if __name__ == '__main__':
    print("🚀 Iniciando Auto-Reparador Quirúrgico...\n")
    arreglar_app_database()
    arreglar_daos()
    print("\n🎉 ¡Reparación completada! Ahora ejecuta tu build.")
