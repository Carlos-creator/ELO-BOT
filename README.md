# ELOBOT — Asistente Académico Universitario

Chatbot basado en RAG *(Retrieval-Augmented Generation)* que responde preguntas sobre programas de asignatura universitarios usando una base de datos vectorial y el LLM Gemini de Google.

## ¿Cómo funciona?

```
Usuario escribe pregunta
        │
        ▼
Se genera un embedding de la pregunta (sentence-transformers)
        │
        ▼
Se buscan los chunks más similares en MongoDB (similitud coseno)
        │
        ▼
Los chunks recuperados se le pasan como contexto a Gemini
        │
        ▼
Gemini responde basándose SOLO en ese contexto
```

## Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Python 3.10+
- JDK 17+ (`winget install Microsoft.OpenJDK.17`)

## Estructura del proyecto

```
ELOBOT/
├── api.py                  ← Servidor FastAPI (puente Python ↔ Java)
├── queries.py              ← Lógica RAG: embedding + búsqueda en MongoDB
├── respuestas_bro.py       ← Llama a Gemini con el contexto recuperado
├── docker-compose.yml      ← Levanta MongoDB
├── requirements.txt        ← Dependencias Python
├── .env                    ← API keys (NO subir al repo)
├── .env.example            ← Plantilla de variables de entorno
├── RAMOS_Y_PREGUNTAS.md    ← Lista de los 84 ramos disponibles y preguntas de prueba
├── data/
│   └── universidad_rag.programas_asignaturas.json  ← 840 chunks con embeddings
└── frontend/
    └── ElobotChat.java     ← Chatbot con interfaz gráfica (Java Swing)
```

## Instalación

### 1. Clonar el repositorio y configurar variables de entorno

```bash
git clone https://github.com/Carlos-creator/ELO-BOT.git
cd ELO-BOT
cp .env.example .env
# Editar .env y agregar tu API key de Gemini
```

### 2. Instalar dependencias Python

```bash
pip install -r requirements.txt
```

### 3. Levantar MongoDB con Docker

```bash
docker compose up -d
```

### 4. Poblar la base de datos

Importar los 840 chunks (84 asignaturas) desde el JSON incluido en el repositorio:

```powershell
docker cp "data\universidad_rag.programas_asignaturas.json" mongo_rag_db:/tmp/datos.json
docker exec mongo_rag_db mongoimport --username rag_user --password rag_password_2026 --authenticationDatabase admin --db universidad_rag --collection programas_asignaturas --file /tmp/datos.json --jsonArray
```

> Para reimportar desde cero (si ya hay datos), borra la colección primero:
> ```powershell
> docker exec mongo_rag_db mongosh -u rag_user -p rag_password_2026 --authenticationDatabase admin --eval "db.getSiblingDB('universidad_rag').programas_asignaturas.drop()"
> ```

## Ejecutar el sistema

### Backend (API Python)
```bash
uvicorn api:app --reload --port 8001
```

### Frontend (Chatbot Java)
```bash
cd frontend
javac -encoding UTF-8 ElobotChat.java
java ElobotChat
```

> El backend debe estar corriendo **antes** de abrir el frontend.

## Variables de entorno

| Variable | Descripción |
|---|---|
| `GEMINI_API_KEY` | API key de Google Gemini ([obtener aquí](https://aistudio.google.com/app/apikey)) |

## Conexión MongoDB

| Parámetro | Valor |
|---|---|
| Host | `localhost:27017` |
| Usuario | `rag_user` |
| Contraseña | `rag_password_2026` |
| Base de datos | `universidad_rag` |
| Colección | `programas_asignaturas` |

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Base de datos | MongoDB (Docker) |
| Embeddings | `paraphrase-multilingual-mpnet-base-v2` |
| LLM | Google Gemini 2.5 Flash |
| Backend API | FastAPI + Uvicorn |
| Frontend | Java Swing (JDK 17) |
