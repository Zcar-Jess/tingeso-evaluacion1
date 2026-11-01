#!/usr/bin/env bash
# Prueba la seguridad (Keycloak + roles) del backend en ejecución.
#
# Requisitos: backend SIN el perfil dev, Keycloak con el realm siga-ipt importado
# (usuarios admin, docente y estudiante) y la carga DEMO_DATA activa (siga.demo-data=true).
#
# Uso:  bash probar-seguridad.sh
#       BASE=http://localhost:8080 KC=http://localhost:9090 bash probar-seguridad.sh

BASE="${BASE:-http://localhost:8080}"
KC="${KC:-http://localhost:9090}"
REALM="${REALM:-siga-ipt}"
PASS=0
FAIL=0
LAST_BODY=""

token() {   # token <usuario> <clave>
  curl -s -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
       -d grant_type=password -d client_id=siga-frontend -d "username=$1" -d "password=$2" \
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

# req <nombre> <token|-> <MÉTODO> <ruta> <esperado> [cuerpo]
req() {
  local name="$1" tok="$2" method="$3" path="$4" expected="$5" body="${6:-}"
  local args=(-s -w $'\n%{http_code}' -X "$method" "$BASE$path")
  [ "$tok" != "-" ] && args+=(-H "Authorization: Bearer $tok")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  local out code
  out=$(curl "${args[@]}")
  code="${out##*$'\n'}"
  LAST_BODY="${out%$'\n'*}"
  if [ "$code" = "$expected" ]; then
    PASS=$((PASS + 1)); printf '[ OK ] %-62s %s\n' "$name" "$code"
  else
    FAIL=$((FAIL + 1)); printf '[FAIL] %-62s recibido %s, esperado %s\n' "$name" "$code" "$expected"
    printf '       respuesta: %s\n' "$LAST_BODY"
  fi
}
has() { if echo "$LAST_BODY" | grep -q "$2"; then PASS=$((PASS+1)); printf '[ OK ] %s\n' "$1"; else FAIL=$((FAIL+1)); printf '[FAIL] %s (no aparece %s)\n       respuesta: %s\n' "$1" "$2" "$LAST_BODY"; fi; }
section() { printf '\n=== %s ===\n' "$1"; }
field() { echo "$LAST_BODY" | sed -n "s/.*\"$1\":\([0-9]*\).*/\1/p"; }

section "Tokens de Keycloak"
ADMIN=$(token admin 'Admin123!')
TEACHER=$(token docente 'Docente123!')
STUDENT=$(token estudiante 'Estudiante123!')
for t in ADMIN TEACHER STUDENT; do
  if [ -n "${!t}" ]; then echo "[ OK ] token de $t obtenido"; PASS=$((PASS+1));
  else echo "[FAIL] no se pudo obtener el token de $t (¿Keycloak en $KC con el realm $REALM?)"; FAIL=$((FAIL+1)); fi
done

section "Sin token y documentación pública"
req "Sin token: listar carreras"              - GET /api/careers 401
req "Token inválido"                          "esto-no-es-un-jwt" GET /api/careers 401
req "Swagger UI es público"                   - GET /swagger-ui/index.html 200
req "OpenAPI es público"                      - GET /v3/api-docs 200

section "Identidad: GET /api/me"
req "Administrador"                           "$ADMIN" GET /api/me 200
has "  rol ADMIN"                             '"role":"ADMIN"'
req "Docente"                                 "$TEACHER" GET /api/me 200
has "  rol TEACHER"                           '"role":"TEACHER"'
TID=$(field userId)
req "Estudiante"                              "$STUDENT" GET /api/me 200
has "  rol STUDENT"                           '"role":"STUDENT"'
SID=$(field userId)
echo "docente=$TID estudiante=$SID"

section "Permisos por rol"
req "ADMIN lee carreras"                      "$ADMIN" GET /api/careers 200
req "STUDENT lee carreras"                    "$STUDENT" GET /api/careers 200
req "TEACHER lee carreras"                    "$TEACHER" GET /api/careers 200
req "STUDENT no crea carreras"                "$STUDENT" POST /api/careers 403 '{"code":"X","name":"X","description":"X"}'
req "TEACHER no crea carreras"                "$TEACHER" POST /api/careers 403 '{"code":"X","name":"X","description":"X"}'
req "ADMIN lista estudiantes"                 "$ADMIN" GET /api/students 200
req "STUDENT no lista estudiantes"            "$STUDENT" GET /api/students 403
req "TEACHER no lista estudiantes"            "$TEACHER" GET /api/students 403
req "ADMIN lista docentes"                    "$ADMIN" GET /api/teachers 200
req "STUDENT no lista docentes"               "$STUDENT" GET /api/teachers 403
req "TEACHER no lista docentes"               "$TEACHER" GET /api/teachers 403
req "STUDENT no crea períodos"                "$STUDENT" POST /api/periods 403 '{"academicYear":2099,"semester":1}'
req "TEACHER no cierra períodos"              "$TEACHER" POST /api/periods/1/close 403
req "STUDENT no cierra períodos"              "$STUDENT" POST /api/periods/1/close 403

section "Cada usuario solo accede a sus propios datos"
req "STUDENT ve su ficha"                     "$STUDENT" GET /api/students/$SID 200
req "ADMIN ve la ficha del estudiante"        "$ADMIN" GET /api/students/$SID 200
OTHER=$((SID + 1000))
req "STUDENT no ve la ficha de otro"          "$STUDENT" GET /api/students/$OTHER 403
req "STUDENT ve su historial"                 "$STUDENT" GET /api/students/$SID/academic-history 200
req "STUDENT no ve el historial de otro"      "$STUDENT" GET /api/students/$OTHER/academic-history 403
req "TEACHER ve sus secciones"                "$TEACHER" GET /api/teachers/$TID/sections 200
req "TEACHER no ve las secciones de otro"     "$TEACHER" GET /api/teachers/$((TID + 1000))/sections 403
req "STUDENT no ve secciones de docentes"     "$STUDENT" GET /api/teachers/$TID/sections 403
req "ADMIN ve las secciones de un docente"    "$ADMIN" GET /api/teachers/$TID/sections 200

section "Inscripción y notas"
req "STUDENT inscribe a otro estudiante"      "$STUDENT" POST /api/periods/1/enrollments 403 "{\"studentId\":$OTHER,\"courseIds\":[1,2,3]}"
req "ADMIN no inscribe (solo el estudiante)"  "$ADMIN" POST /api/periods/1/enrollments 403 "{\"studentId\":$SID,\"courseIds\":[1,2,3]}"
req "TEACHER no inscribe"                     "$TEACHER" POST /api/periods/1/enrollments 403 "{\"studentId\":$SID,\"courseIds\":[1,2,3]}"
req "STUDENT no registra notas"               "$STUDENT" PUT /api/sections/999999/students/$SID/grade 403 '{"finalGrade":6.0}'
req "ADMIN no registra notas"                 "$ADMIN" PUT /api/sections/999999/students/$SID/grade 403 '{"finalGrade":6.0}'
req "TEACHER pasa la autorización (sección inexistente: 404)" "$TEACHER" PUT /api/sections/999999/students/$SID/grade 404 '{"finalGrade":6.0}'
req "TEACHER: nota fuera de rango"            "$TEACHER" PUT /api/sections/999999/students/$SID/grade 400 '{"finalGrade":9.0}'
req "STUDENT anula inscripción inexistente"   "$STUDENT" DELETE /api/enrollments/999999 404
req "ADMIN no anula inscripciones"            "$ADMIN" DELETE /api/enrollments/999999 403

printf '\nResultado: %d correctas, %d fallidas\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
