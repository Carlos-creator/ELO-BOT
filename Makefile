# ELOBOT — Makefile
# Requiere: Python 3.11+, JDK 17, Docker corriendo

.PHONY: all backend frontend compile clean

all: compile

# Levantar MongoDB (Docker)
mongo:
	docker compose up -d

# Levantar el backend FastAPI
backend:
	uvicorn api:app --reload --port 8001

# Compilar el frontend Java
compile:
	javac -encoding UTF-8 frontend/*.java

# Compilar y ejecutar el frontend Java
frontend: compile
	java -cp frontend ElobotChat

# Limpiar archivos .class
clean:
	del /Q frontend\*.class 2>nul || rm -f frontend/*.class
