import os
import re

DIRECTORIO = '.'

def analizar_room():
    print("🔍 RASTREANDO ENTITIES Y DAOS EN EL PROYECTO...\n")
    
    for raiz, _, archivos in os.walk(DIRECTORIO):
        for archivo in archivos:
            if archivo.endswith('.kt'):
                ruta_completa = os.path.join(raiz, archivo)
                
                with open(ruta_completa, 'r', encoding='utf-8') as f:
                    contenido = f.read()
                    lineas = contenido.split('\n')
                    
                    if '@Entity' in contenido:
                        print(f"📦 ENTITY ENCONTRADO: {archivo}")
                        for linea in lineas:
                            if re.search(r'\b(val|var)\b', linea) and not linea.strip().startswith('//'):
                                print(f"   -> Variable: {linea.strip()}")
                        print("-" * 40)
                    
                    elif '@Dao' in contenido:
                        print(f"🛠️  DAO ENCONTRADO: {archivo}")
                        for linea in lineas:
                            if re.search(r'\bfun\b', linea) and not linea.strip().startswith('//'):
                                print(f"   -> Función: {linea.strip()}")
                        print("-" * 40)

if __name__ == "__main__":
    analizar_room()
