import os
from dotenv import load_dotenv
from google import genai
from queries import recuperar_contexto_rag, armar_prompt_llm

load_dotenv()

# ==========================================
# CONFIGURACIÓN DEL LLM (NUEVA LIBRERÍA GENAI)
# ==========================================
cliente_gemini = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

# ==========================================
# EJECUCIÓN DEL FLUJO COMPLETO
# ==========================================
def consultar_asistente_universitario(pregunta, codigo_ramo):
    print(f"\nBuscando información en la base de datos para: {codigo_ramo}...")

    top_k = 3 if codigo_ramo else 10
    contexto = recuperar_contexto_rag(pregunta, codigo_ramo=codigo_ramo, top_k=top_k)

    if contexto.startswith("❌"):
        return contexto

    prompt = armar_prompt_llm(pregunta, contexto)
    print("Enviando contexto al LLM para redactar la respuesta...")

    respuesta = cliente_gemini.models.generate_content(
        model='gemini-2.5-flash',
        contents=prompt
    )
    return respuesta.text

if __name__ == "__main__":
    codigo_prueba = "ELO-329"
    pregunta_prueba = "¿Qué lenguaje de programación se utiliza en el curso y cuáles son los requisitos previos?"

    texto = consultar_asistente_universitario(pregunta_prueba, codigo_prueba)
    print("\n" + "="*50)
    print("🎓 RESPUESTA DEL ASISTENTE:")
    print("="*50)
    print(texto)
    print("="*50)