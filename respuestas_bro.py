import os
from dotenv import load_dotenv
from google import genai
from queries import recuperar_contexto_rag, armar_prompt_llm

load_dotenv()

# ==========================================
# ROTACIÓN DE API KEYS (FALLBACK)
# Se intentan en orden; si una falla (cuota agotada), pasa a la siguiente.
# ==========================================
_raw_keys = [
    os.getenv("GEMINI_API_KEY_1"),
    os.getenv("GEMINI_API_KEY_2"),
    os.getenv("GEMINI_API_KEY_3"),
    os.getenv("GEMINI_API_KEY_4"),
]
CLIENTES_GEMINI = [genai.Client(api_key=k) for k in _raw_keys if k]

if not CLIENTES_GEMINI:
    raise RuntimeError("No hay API keys configuradas. Revisa tu .env")

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

    for i, cliente in enumerate(CLIENTES_GEMINI):
        try:
            print(f"Enviando al LLM (key {i + 1}/{len(CLIENTES_GEMINI)})...")
            respuesta = cliente.models.generate_content(
                model='gemini-2.5-flash',
                contents=prompt
            )
            return respuesta.text
        except Exception as e:
            print(f"⚠️  Key {i + 1} falló: {e}")

    return "❌ Todas las API keys están agotadas o fallaron. Intenta más tarde."

if __name__ == "__main__":
    codigo_prueba = "ELO-329"
    pregunta_prueba = "¿Qué lenguaje de programación se utiliza en el curso y cuáles son los requisitos previos?"

    texto = consultar_asistente_universitario(pregunta_prueba, codigo_prueba)
    print("\n" + "="*50)
    print("🎓 RESPUESTA DEL ASISTENTE:")
    print("="*50)
    print(texto)
    print("="*50)