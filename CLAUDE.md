# CLAUDE.md — Contexto del proyecto ELOBOT

## ¿Qué es este proyecto?

ELOBOT es un chatbot académico universitario basado en RAG para la carrera de Ingeniería Civil Electrónica (ELO). Responde preguntas sobre programas de asignatura usando MongoDB como base de datos vectorial y Gemini como LLM. Es un proyecto universitario — el profe pidió backend en Python y frontend en Java.

## Stack

| Capa | Tecnología |
|---|---|
| Base de datos | MongoDB en Docker |
| Embeddings | `sentence-transformers` — modelo `paraphrase-multilingual-mpnet-base-v2` (corre local, sin API key) |
| LLM | Google Gemini 2.5 Flash vía `google-genai` |
| Backend API | FastAPI + Uvicorn en **puerto 8001** (el 8000 está ocupado por otro contenedor del usuario) |
| Frontend | Java Swing — JDK 17 requerido (`winget install Microsoft.OpenJDK.17`) |

## Estructura de archivos

```
ELOBOT/
├── api.py                  ← FastAPI, endpoint POST /consultar
├── queries.py              ← Búsqueda híbrida: filtro MongoDB + similitud coseno en Python
├── respuestas_bro.py       ← Cliente Gemini, retorna texto de respuesta
├── cargar_datos.py         ← AÚN NO EXISTE — pendiente de crear
├── docker-compose.yml      ← MongoDB, container: mongo_rag_db, puerto 27017
├── requirements.txt
├── .env                    ← GEMINI_API_KEY (no subir al repo)
├── .env.example
├── .gitignore
├── README.md
├── data/
│   └── dump_rag_db.gz      ← CORRUPTO — ver sección "Problema pendiente"
└── frontend/
    └── ElobotChat.java     ← UI dark theme, Swing
```

## Cambios realizados respecto al código base original

### `respuestas_bro.py`
- **Antes:** `consultar_asistente_universitario()` solo imprimía la respuesta y retornaba `None`.
- **Ahora:** retorna `respuesta.text` (string). Si no hay contexto, retorna el mensaje de error. El bloque `__main__` imprime el resultado manualmente.
- API key movida a `.env` como `GEMINI_API_KEY`, se carga con `load_dotenv()`.

### `queries.py`
- Sin cambios de lógica. Solo se reorganizó en la estructura de carpetas.

### Archivos nuevos creados

**`api.py`**
- Servidor FastAPI con un solo endpoint: `POST /consultar`
- Body: `{ "pregunta": str, "codigo_ramo": str }`
- Response: `{ "respuesta": str }`
- CORS abierto (`allow_origins=["*"]`) para que Java pueda conectarse
- Importa y llama a `consultar_asistente_universitario` de `respuestas_bro.py`
- Correr con: `uvicorn api:app --reload --port 8001`

**`frontend/ElobotChat.java`**
- UI dark theme completa en Java Swing (JDK 17+)
- Tema oscuro: fondo `#0D0D19`, burbujas usuario azul `#4F6CF6`, burbujas bot `#20203C`
- Header con ícono gradiente, nombre ELOBOT, estado "● En línea"
- Botones de ventana estilo macOS (amarillo = minimizar, rojo = cerrar) en el header
- Campo para código del ramo (default: "ELO-329")
- Botón **TXT** para subir un archivo `.txt` y cargar su contenido como pregunta
- Indicador de escritura `•••` mientras espera respuesta de la API
- Scroll automático al último mensaje
- Llama a `http://localhost:8001/consultar` vía `HttpClient` (Java 11+)
- Parsing manual de JSON (sin dependencias externas)
- **Compilar:** `javac -encoding UTF-8 ElobotChat.java` (el flag de encoding es obligatorio en Windows)
- **Correr:** `java ElobotChat`

**`.env`**
```
GEMINI_API_KEY=<key real aquí>
```

**`.env.example`**
```
GEMINI_API_KEY=tu_api_key_aqui
```

**`.gitignore`**
- Excluye `.env`, `__pycache__`, `.venv`, `.vscode`, `.idea`, `.DS_Store`, `.claude`

**`requirements.txt`**
```
pymongo
numpy
sentence-transformers
google-genai
python-dotenv
fastapi
uvicorn[standard]
```

**`data/`**
- Carpeta creada para separar el dump de los archivos Python

## Problema pendiente: dump_rag_db.gz CORRUPTO

El archivo `data/dump_rag_db.gz` está corrupto — empieza con bytes `FF FE` (BOM UTF-16) en lugar de `1F 8B` (magic gzip). Fue enviado por correo y se corrompió en el proceso. No es recuperable automáticamente.

**Solución A:** Pedir al dueño de la BD que genere el dump en Linux/WSL:
```bash
docker exec mongo_rag_db mongodump --username rag_user --password rag_password_2026 \
  --authenticationDatabase admin --archive --gzip > dump_rag_db.gz
```
Y compartir por Google Drive (no por correo).

**Solución B (pendiente de implementar):** El usuario tiene ~80 archivos fuente de los programas de asignatura. Hay que crear `cargar_datos.py` que:
1. Lea los archivos (formato aún no confirmado — preguntar si son PDF, DOCX o TXT)
2. Divida en chunks
3. Genere embeddings con `sentence-transformers`
4. Inserte en MongoDB

## Esquema de documentos en MongoDB

```
DB: universidad_rag
Collection: programas_asignaturas

{
  "codigo_asignatura": "ELO-329",   // filtro principal
  "asignatura": "Nombre del ramo",
  "texto": "Chunk de texto del programa...",
  "embedding": [0.123, -0.456, ...]  // vector 768 dimensiones
}
```

## Credenciales MongoDB (desarrollo)

```
URI: mongodb://rag_user:rag_password_2026@localhost:27017/?authSource=admin
DB: universidad_rag
Collection: programas_asignaturas
```

## Notas importantes

- Puerto **8001** para la API (el 8000 está ocupado por el contenedor `inf23620241-backend` de otro proyecto del usuario)
- El usuario tiene Windows 11, PowerShell — usar `cmd /c` o `docker cp` para operaciones de redirección de archivos binarios, nunca `<` en PowerShell
- Java 8 JRE estaba instalado antes pero sin `javac`. Se instaló JDK 17 con `winget install Microsoft.OpenJDK.17`
- El modelo de embeddings tarda en cargar la primera vez (~30 seg)
- El frontend requiere que el backend esté corriendo antes de enviar mensajes

## Flujo de arranque completo

```powershell
# 1. Levantar MongoDB
docker compose up -d

# 2. (Una sola vez) Restaurar la BD — cuando el dump esté bien
docker cp data\dump_rag_db.gz mongo_rag_db:/tmp/dump_rag_db.gz
docker exec mongo_rag_db mongorestore --username rag_user --password rag_password_2026 --authenticationDatabase admin --archive=/tmp/dump_rag_db.gz --gzip

# 3. Levantar la API
uvicorn api:app --reload --port 8001

# 4. En otra terminal — compilar y correr el frontend
cd frontend
javac -encoding UTF-8 ElobotChat.java
java ElobotChat
```
