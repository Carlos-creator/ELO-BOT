import numpy as np
from pymongo import MongoClient
from sentence_transformers import SentenceTransformer

# ==========================================
# CONFIGURACIÓN DE CONEXIÓN
# ==========================================
USER = "rag_user"
PASSWORD = "rag_password_2026"
HOST = "localhost"
PORT = "27017"
MONGO_URI = f"mongodb://{USER}:{PASSWORD}@{HOST}:{PORT}/?authSource=admin"

DB_NAME = "universidad_rag"
COLLECTION_NAME = "programas_asignaturas"

print("⏳ Cargando modelo de embeddings...")
embedding_model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')

client = MongoClient(MONGO_URI)
collection = client[DB_NAME][COLLECTION_NAME]

# ==========================================
# FUNCIÓN DE BÚSQUEDA HÍBRIDA (METADATA + VECTOR)
# ==========================================
def recuperar_contexto_rag(pregunta, codigo_ramo=None, top_k=3):
    """
    1. Filtra en MongoDB por el código de la asignatura (Metadata).
    2. Calcula la similitud del coseno solo en esos chunks.
    3. Devuelve los mejores K fragmentos listos para el LLM.
    """
    # --- PASO 1: PRE-FILTRADO EN MONGODB ---
    query_mongo = {}
    
    if codigo_ramo:
        # Buscamos exactamente el código (ej: "ELO-329")
        query_mongo["codigo_asignatura"] = codigo_ramo.strip().upper()
        print(f"\n🔍 [Filtro Activo] Extrayendo solo chunks de la asignatura: {query_mongo['codigo_asignatura']}")
    else:
        print("\n🌍 [Búsqueda Global] Buscando en todos los programas...")

    # Traemos los documentos que cumplen el filtro
    documentos = list(collection.find(query_mongo))
    
    if not documentos:
        return f"❌ No se encontró la asignatura {codigo_ramo} en la base de datos."

    print(f"📦 MongoDB redujo el espacio de búsqueda a {len(documentos)} chunks.")

    # --- PASO 2: SIMILITUD VECTORIAL ---
    vector_pregunta = embedding_model.encode(pregunta)
    resultados = []

    for doc in documentos:
        v_chunk = np.array(doc["embedding"])
        # Cálculo de similitud del coseno
        similitud = np.dot(vector_pregunta, v_chunk) / (np.linalg.norm(vector_pregunta) * np.linalg.norm(v_chunk))
        
        resultados.append({
            "score": similitud,
            "codigo": doc["codigo_asignatura"],
            "asignatura": doc["asignatura"],
            "texto": doc["texto"]
        })

    # Ordenamos de mayor a menor similitud
    resultados_ordenados = sorted(resultados, key=lambda x: x["score"], reverse=True)

    # En búsqueda global: un chunk por curso para maximizar diversidad
    if not codigo_ramo:
        vistos = set()
        mejores_chunks = []
        for r in resultados_ordenados:
            if r["codigo"] not in vistos:
                vistos.add(r["codigo"])
                mejores_chunks.append(r)
            if len(mejores_chunks) == top_k:
                break
    else:
        mejores_chunks = resultados_ordenados[:top_k]

    # --- PASO 3: CONSTRUCCIÓN DEL CONTEXTO PARA EL LLM ---
    # Unimos los textos recuperados en un solo gran string estructurado
    contexto_formateado = ""
    for idx, chunk in enumerate(mejores_chunks):
        contexto_formateado += f"\n--- Fragmento {idx + 1} (Origen: [{chunk['codigo']}] {chunk['asignatura']}) ---\n"
        contexto_formateado += f"{chunk['texto']}\n"

    return contexto_formateado

# ==========================================
# CÓMO ARMAR EL PROMPT PARA EL LLM
# ==========================================
def armar_prompt_llm(pregunta_usuario, contexto_recuperado):
    """
    Estructura el prompt final que le enviarás a la API de tu LLM.
    Incluye instrucciones claras para evitar alucinaciones.
    """
    prompt = f"""Eres un asistente académico experto de la universidad. 
Tu tarea es responder a la pregunta del estudiante utilizando ÚNICAMENTE la información proporcionada en el siguiente contexto extraído de los programas de asignatura.
Si la respuesta no está en el contexto, di claramente "No tengo suficiente información en el programa para responder a esto".

CONTEXTO DE LOS PROGRAMAS:
{contexto_recuperado}

PREGUNTA DEL ESTUDIANTE:
{pregunta_usuario}

RESPUESTA:"""
    return prompt

# ==========================================
# PRUEBA DEL SISTEMA
# ==========================================
if __name__ == "__main__":
    # Imagina que un usuario en tu interfaz elige el ramo de Diseño y Programación y hace esta pregunta:
    codigo_seleccionado = "ELO-329"  
    pregunta = "¿Qué lenguaje de programación se utiliza en el curso y cuáles son los requisitos previos?"

    # 1. Recuperar la información
    contexto_llm = recuperar_contexto_rag(pregunta, codigo_ramo=codigo_seleccionado, top_k=2)
    
    # 2. Armar el prompt final
    prompt_final = armar_prompt_llm(pregunta, contexto_llm)

    print("\n" + "="*50)
    print("🤖 ESTE ES EL TEXTO EXACTO QUE LE ENVIARÁS A LA API DEL LLM:")
    print("="*50)
    print(prompt_final)