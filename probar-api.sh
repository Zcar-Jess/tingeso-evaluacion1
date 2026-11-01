#!/usr/bin/env bash
# Pruebas manuales de las Épicas 1-5 de SIGA IPT contra el backend en ejecución.
#
# Uso (con la base VACÍA y la app recién iniciada, para que los ids partan en 1):
#     bash probar-api.sh                     # usa http://localhost:8080
#     BASE=http://localhost:8080 bash probar-api.sh
#
# Cada línea muestra el código HTTP recibido contra el esperado.
# En los rechazos (4xx) se imprime además el mensaje que devolvió el servidor.

BASE="${BASE:-http://localhost:8080}"
PASS=0
FAIL=0
LAST_BODY=""

# req <nombre> <MÉTODO> <ruta> <código esperado> [cuerpo JSON]
req() {
  local name="$1" method="$2" path="$3" expected="$4" body="${5:-}"
  local out code
  if [ -n "$body" ]; then
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$BASE$path" \
          -H 'Content-Type: application/json' -d "$body")
  else
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$BASE$path")
  fi
  code="${out##*$'\n'}"
  LAST_BODY="${out%$'\n'*}"

  local msg=""
  if [ "${code:0:1}" = "4" ]; then
    msg=$(echo "$LAST_BODY" | sed -n 's/.*"message":"\([^"]*\)".*/\1/p')
    [ -n "$msg" ] && msg="  <- $msg"
  fi

  if [ "$code" = "$expected" ]; then
    PASS=$((PASS + 1))
    printf '[ OK ] %-58s %s%s\n' "$name" "$code" "$msg"
  else
    FAIL=$((FAIL + 1))
    printf '[FAIL] %-58s recibido %s, esperado %s\n' "$name" "$code" "$expected"
    printf '       respuesta: %s\n' "$LAST_BODY"
  fi
}

# Id del último recurso devuelto: {"id":7,...}
last_id() { echo "$LAST_BODY" | sed -n 's/^{"id":\([0-9]*\).*/\1/p'; }

section() { printf '\n=== %s ===\n' "$1"; }

# ---------------------------------------------------------------------------
section "Épicas 2 y 3: carreras, planes y asignaturas"
req "Listar carreras"                         GET    /api/careers 200
req "Carrera con código repetido"             POST   /api/careers 409 \
    '{"code":"10450","name":"Duplicada","description":"x"}'
req "Eliminar carrera con planes y estudiantes" DELETE /api/careers/1 409
req "Asignatura con SCT 9"                    POST   /api/study-plans/1/courses 400 \
    '{"code":"X1","name":"X","semester":1,"theoryHours":2,"exerciseHours":0,"labHours":0,"sct":9}'
req "Asignatura con 8 horas semanales"        POST   /api/study-plans/1/courses 422 \
    '{"code":"X2","name":"X","semester":1,"theoryHours":4,"exerciseHours":4,"labHours":0,"sct":5}'

COURSES=$(curl -s "$BASE/api/study-plans/1/courses")
cid() { echo "$COURSES" | grep -o "\"id\":[0-9]*,\"code\":\"$1\"" | head -1 | sed 's/[^0-9]*\([0-9]*\),.*/\1/'; }
TAP101=$(cid TAP101)   # TEL 2-2-2 = 6 h -> 3 bloques
TAP102=$(cid TAP102)   # TEL 2-2-0 = 4 h -> 2 bloques
TAP103=$(cid TAP103)   # TEL 2-0-2 = 4 h -> 2 bloques
echo "ids de asignaturas: TAP101=$TAP101 TAP102=$TAP102 TAP103=$TAP103"

# ---------------------------------------------------------------------------
section "Épica 1: estudiantes"
req "Crear estudiante (queda REGULAR)"        POST   /api/students 201 \
    '{"run":"20111111-1","fullName":"Estudiante Uno","email":"est1@ipt.cl","careerId":1}'
STUDENT=$(last_id)
echo "$LAST_BODY" | grep -q '"status":"REGULAR"' && echo "       estado inicial REGULAR confirmado" \
  || echo "       ATENCION: el estado inicial no es REGULAR"
req "RUN repetido"                            POST   /api/students 409 \
    '{"run":"20111111-1","fullName":"Otro","email":"otro@ipt.cl","careerId":1}'
req "Correo repetido"                         POST   /api/students 409 \
    '{"run":"20222222-2","fullName":"Otro","email":"est1@ipt.cl","careerId":1}'
req "Correo con formato inválido"             POST   /api/students 400 \
    '{"run":"20333333-3","fullName":"Otro","email":"no-es-correo","careerId":1}'
req "Carrera inexistente"                     POST   /api/students 404 \
    '{"run":"20444444-4","fullName":"Otro","email":"o4@ipt.cl","careerId":999}'
req "Estado ELIMINADO manual"                 PATCH  /api/students/$STUDENT/status 422 '{"status":"ELIMINATED"}'
req "Estado EGRESADO manual"                  PATCH  /api/students/$STUDENT/status 422 '{"status":"GRADUATED"}'
req "Estado POSTERGACIÓN"                     PATCH  /api/students/$STUDENT/status 200 '{"status":"POSTPONED"}'
req "Estado RETIRO TEMPORAL"                  PATCH  /api/students/$STUDENT/status 200 '{"status":"TEMPORARY_WITHDRAWAL"}'
req "Volver a REGULAR"                        PATCH  /api/students/$STUDENT/status 200 '{"status":"REGULAR"}'
req "Carrera INACTIVA: pasar carrera 3 a INACTIVE" PATCH /api/careers/3/status 200 '{"status":"INACTIVE"}'
req "Carrera INACTIVA no recibe estudiantes"  POST   /api/students 422 \
    '{"run":"20555555-5","fullName":"Otro","email":"o5@ipt.cl","careerId":3}'
req "Reactivar carrera 3"                     PATCH  /api/careers/3/status 200 '{"status":"ACTIVE"}'
req "Cambiar de carrera sin historial"        PUT    /api/students/$STUDENT 200 \
    '{"fullName":"Estudiante Uno","email":"est1@ipt.cl","careerId":2}'
req "Historial académico (vacío)"             GET    /api/students/$STUDENT/academic-history 200
req "Listar con filtro de estado"             GET    "/api/students?status=REGULAR" 200
req "Crear estudiante para eliminar"          POST   /api/students 201 \
    '{"run":"20666666-6","fullName":"Borrable","email":"borrable@ipt.cl","careerId":1}'
req "Eliminar estudiante sin historial"       DELETE /api/students/$(last_id) 204

# ---------------------------------------------------------------------------
section "Épica 4: docentes"
req "Crear docente 1 (queda ACTIVE)"          POST   /api/teachers 201 \
    '{"run":"11111111-1","fullName":"Docente Uno","email":"doc1@ipt.cl","professionalTitle":"Ingeniero Civil Informático","academicDegree":"Magíster"}'
T1=$(last_id)
req "RUN de docente repetido"                 POST   /api/teachers 409 \
    '{"run":"11111111-1","fullName":"X","email":"x@ipt.cl","professionalTitle":"T","academicDegree":"G"}'
req "Correo de docente repetido"              POST   /api/teachers 409 \
    '{"run":"22222222-2","fullName":"X","email":"doc1@ipt.cl","professionalTitle":"T","academicDegree":"G"}'
req "Docente sin grado académico"             POST   /api/teachers 400 \
    '{"run":"33333333-3","fullName":"X","email":"x3@ipt.cl","professionalTitle":"T"}'
req "Crear docente 2"                         POST   /api/teachers 201 \
    '{"run":"44444444-4","fullName":"Docente Dos","email":"doc2@ipt.cl","professionalTitle":"Ingeniero","academicDegree":"Licenciado"}'
T2=$(last_id)
req "Docente 2 a INACTIVE (sin secciones)"    PATCH  /api/teachers/$T2/status 200 '{"status":"INACTIVE"}'

# ---------------------------------------------------------------------------
section "Épica 5: períodos y secciones"
req "Crear período 2027-1 (queda OPEN)"       POST   /api/periods 201 '{"academicYear":2027,"semester":1}'
P=$(last_id)
req "Período repetido"                        POST   /api/periods 409 '{"academicYear":2027,"semester":1}'
req "Período con semestre 3"                  POST   /api/periods 400 '{"academicYear":2027,"semester":3}'

B3='[{"day":"L","module":1},{"day":"W","module":1},{"day":"V","module":1}]'
req "Sección TAP101 válida (3 bloques)"       POST   /api/periods/$P/sections 201 \
    "{\"courseId\":$TAP101,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":$B3}"
S1=$(last_id)
req "Punto 4: TAP103 con 1 bloque (necesita 2)" POST /api/periods/$P/sections 422 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":1,\"scheduleBlocks\":[{\"day\":\"L\",\"module\":1}]}"
req "Bloques duplicados L1, L1"               POST   /api/periods/$P/sections 422 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"L\",\"module\":1},{\"day\":\"L\",\"module\":1}]}"
req "Punto 5: conflicto del docente en L1"    POST   /api/periods/$P/sections 409 \
    "{\"courseId\":$TAP102,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"L\",\"module\":1},{\"day\":\"M\",\"module\":1}]}"
req "Sección TAP102 sin conflicto (L2, M2)"   POST   /api/periods/$P/sections 201 \
    "{\"courseId\":$TAP102,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"L\",\"module\":2},{\"day\":\"M\",\"module\":2}]}"
S2=$(last_id)
req "Segunda sección de TAP101 en el período" POST   /api/periods/$P/sections 409 \
    "{\"courseId\":$TAP101,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":$B3}"
req "Capacidad 61"                            POST   /api/periods/$P/sections 400 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":61,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Capacidad 0"                             POST   /api/periods/$P/sections 400 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":0,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Día inválido X"                          POST   /api/periods/$P/sections 400 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"X\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Módulo 7"                                POST   /api/periods/$P/sections 400 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":7},{\"day\":\"J\",\"module\":2}]}"
req "Docente inexistente"                     POST   /api/periods/$P/sections 404 \
    "{\"courseId\":$TAP103,\"teacherId\":999,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Asignatura inexistente"                  POST   /api/periods/$P/sections 404 \
    "{\"courseId\":999,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Docente INACTIVO como responsable"       POST   /api/periods/$P/sections 422 \
    "{\"courseId\":$TAP103,\"teacherId\":$T2,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Período inexistente"                     POST   /api/periods/999/sections 404 \
    "{\"courseId\":$TAP103,\"teacherId\":$T1,\"capacity\":30,\"scheduleBlocks\":[{\"day\":\"J\",\"module\":1},{\"day\":\"J\",\"module\":2}]}"
req "Oferta del período"                      GET    /api/periods/$P/sections 200
req "Modificar sección: capacidad 0"          PUT    /api/sections/$S1 400 \
    "{\"teacherId\":$T1,\"capacity\":0,\"scheduleBlocks\":$B3}"
req "Modificar sección: capacidad 40"         PUT    /api/sections/$S1 200 \
    "{\"teacherId\":$T1,\"capacity\":40,\"scheduleBlocks\":$B3}"
req "Docente 1 a INACTIVE con secciones abiertas" PATCH /api/teachers/$T1/status 409 '{"status":"INACTIVE"}'
req "Eliminar docente con secciones"          DELETE /api/teachers/$T1 409
req "Eliminar sección sin inscritos"          DELETE /api/sections/$S2 204
req "Reactivar docente 2"                     PATCH  /api/teachers/$T2/status 200 '{"status":"ACTIVE"}'

# ---------------------------------------------------------------------------
printf '\nResultado: %d correctas, %d fallidas\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
