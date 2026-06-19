import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv
from respuestas_bro import consultar_asistente_universitario

load_dotenv()

app = FastAPI(title="ELOBOT API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class Consulta(BaseModel):
    pregunta: str
    codigo_ramo: str

@app.post("/consultar")
def consultar(consulta: Consulta):
    codigo = consulta.codigo_ramo.strip().upper() or None
    respuesta = consultar_asistente_universitario(consulta.pregunta, codigo)
    return {"respuesta": respuesta}
