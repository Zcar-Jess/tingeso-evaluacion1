#!/usr/bin/env bash
# Pruebas manuales de las Épicas 6 y 7 (matrícula, inscripción, notas y cierre de período).
#
# Uso:  bash probar-epicas-6-7.sh            (backend en http://localhost:8080)
#       BASE=http://localhost:8080 bash probar-epicas-6-7.sh
#
# Crea sus propios datos (docentes, estudiantes y períodos nuevos en cada ejecución), por lo que
# se puede repetir sin vaciar la base. Requiere las carreras cargadas por el DataLoader, con
# el plan de Técnico Analista Programador como plan 1 y el de Análisis de Datos como plan 2.
# Cada línea muestra el código HTTP recibido contra el esperado.

BASE="${BASE:-http://localhost:8080}"
PASS=0
FAIL=0
LAST_BODY=""

req() {   # req <nombre> <MÉTODO> <ruta> <código esperado> [cuerpo JSON]
  local name="$1" method="$2" path="$3" expected="$4" body="${5:-}"
  local out code
  if [ -n "$body" ]; then
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$BASE$path" -H 'Content-Type: application/json' -d "$body")
  else
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$BASE$path")
  fi
  code="${out##*$'\n'}"
  LAST_BODY="${out%$'\n'*}"

  local msg="" det=""
  if [ "${code:0:1}" = "4" ]; then
    msg=$(echo "$LAST_BODY" | sed -n 's/.*"message":"\([^"]*\)".*/\1/p')
    det=$(echo "$LAST_BODY" | sed -n 's/.*"details":\[\(.*\)\].*/\1/p')
    [ -n "$msg" ] && msg="  <- $msg"
    [ -n "$det" ] && msg="$msg | $det"
  fi
  if [ "$code" = "$expected" ]; then
    PASS=$((PASS + 1)); printf '[ OK ] %-60s %s%s\n' "$name" "$code" "$msg"
  else
    FAIL=$((FAIL + 1)); printf '[FAIL] %-60s recibido %s, esperado %s\n' "$name" "$code" "$expected"
    printf '       respuesta: %s\n' "$LAST_BODY"
  fi
}

has()   { if echo "$LAST_BODY" | grep -q "$2"; then PASS=$((PASS+1)); printf '[ OK ] %s\n' "$1"; else FAIL=$((FAIL+1)); printf '[FAIL] %s (no aparece %s)\n       respuesta: %s\n' "$1" "$2" "$LAST_BODY"; fi; }
lacks() { if echo "$LAST_BODY" | grep -q "$2"; then FAIL=$((FAIL+1)); printf '[FAIL] %s (aparece %s)\n       respuesta: %s\n' "$1" "$2" "$LAST_BODY"; else PASS=$((PASS+1)); printf '[ OK ] %s\n' "$1"; fi; }

last_id() { echo "$LAST_BODY" | sed -n 's/^{"id":\([0-9]*\).*/\1/p'; }
section() { printf '\n=== %s ===\n' "$1"; }
blk() { local out="" b; for b in "$@"; do out+="{\"day\":\"${b:0:1}\",\"module\":${b:1:1}},"; done; echo "[${out%,}]"; }
cid() { echo "$2" | grep -o "\"id\":[0-9]*,\"code\":\"$1\"" | head -1 | sed 's/[^0-9]*\([0-9]*\),.*/\1/'; }
# id de la inscripción de un curso, a partir de la última respuesta (lista de inscripciones)
enr_id() { echo "$LAST_BODY" | grep -o "{\"id\":[0-9]*,[^}]*\"courseCode\":\"$1\"" | head -1 | sed 's/^{"id":\([0-9]*\),.*/\1/'; }
sec() { # sec <período> <curso> <docente> <capacidad> <bloques...>  -> crea la sección y deja su id en SEC_ID
  local p="$1" c="$2" t="$3" cap="$4"; shift 4
  req "Sección del curso $c en el período $p" POST /api/periods/$p/sections 201 \
      "{\"courseId\":$c,\"teacherId\":$t,\"capacity\":$cap,\"scheduleBlocks\":$(blk "$@")}"
  SEC_ID=$(last_id)
}
grade() { echo "{\"teacherId\":$1,\"finalGrade\":$2}"; }

# ---------------------------------------------------------------------------
section "Preparación: asignaturas, docentes, estudiantes y períodos"
C1=$(curl -s "$BASE/api/study-plans/1/courses")
C2=$(curl -s "$BASE/api/study-plans/2/courses")
T101=$(cid TAP101 "$C1"); T102=$(cid TAP102 "$C1"); T103=$(cid TAP103 "$C1")
T104=$(cid TAP104 "$C1"); T105=$(cid TAP105 "$C1"); T106=$(cid TAP106 "$C1")
T201=$(cid TAP201 "$C1"); T202=$(cid TAP202 "$C1"); T204=$(cid TAP204 "$C1")
D101=$(cid TAD101 "$C2")
echo "asignaturas: 101=$T101 102=$T102 103=$T103 104=$T104 105=$T105 106=$T106 201=$T201 202=$T202 204=$T204 TAD101=$D101"

U=$(date +%s); R=$(echo "$U" | tail -c 7)
req "Docente 1"  POST /api/teachers 201 "{\"run\":\"A1-$R\",\"fullName\":\"Docente Uno $R\",\"email\":\"d1.$U@ipt.cl\",\"professionalTitle\":\"Ingeniero\",\"academicDegree\":\"Magíster\"}"
TE1=$(last_id)
req "Docente 2"  POST /api/teachers 201 "{\"run\":\"A2-$R\",\"fullName\":\"Docente Dos $R\",\"email\":\"d2.$U@ipt.cl\",\"professionalTitle\":\"Ingeniero\",\"academicDegree\":\"Licenciado\"}"
TE2=$(last_id)
mkstu() { req "Estudiante $1" POST /api/students 201 "{\"run\":\"$1-$R\",\"fullName\":\"Estudiante $1 $R\",\"email\":\"$1.$U@ipt.cl\",\"careerId\":1}"; }
mkstu SA; SA=$(last_id)   # estudiante principal
mkstu SB; SB=$(last_id)   # no matriculado
mkstu SC; SC=$(last_id)   # en postergación
mkstu SD; SD=$(last_id)   # para la prueba de cupos

PERIODS=$(curl -s "$BASE/api/periods")
Y=$((2040 + RANDOM % 50))
while echo "$PERIODS" | grep -q -E "\"academicYear\":($Y|$((Y+1))),"; do Y=$((Y+2)); done
req "Período $Y-1" POST /api/periods 201 "{\"academicYear\":$Y,\"semester\":1}"; P1=$(last_id)
req "Período $Y-2" POST /api/periods 201 "{\"academicYear\":$Y,\"semester\":2}"; P2=$(last_id)
req "Período $((Y+1))-1" POST /api/periods 201 "{\"academicYear\":$((Y+1)),\"semester\":1}"; P3=$(last_id)

section "Oferta del período $Y-1 (docente 1, salvo TAP106 con docente 2)"
sec $P1 $T101 $TE1 30 L1 W1 V1; S101=$SEC_ID
sec $P1 $T102 $TE1 30 L2 W2;    S102=$SEC_ID
sec $P1 $T103 $TE1 30 L3 W3;    S103=$SEC_ID
sec $P1 $T104 $TE1 1  M1 J1;    S104=$SEC_ID    # capacidad 1
sec $P1 $T105 $TE1 30 M2 J2;    S105=$SEC_ID
sec $P1 $T106 $TE2 30 L1 W1;    S106=$SEC_ID    # choca con TAP101
sec $P1 $T201 $TE1 30 V2 V3 V4; S201=$SEC_ID

# ---------------------------------------------------------------------------
section "Épica 6: matrícula en el período"
req "Matricular a SA"                         POST /api/periods/$P1/registrations 201 "{\"studentId\":$SA}"
req "Matricular a SA otra vez"                POST /api/periods/$P1/registrations 409 "{\"studentId\":$SA}"
req "Matricular a SC"                         POST /api/periods/$P1/registrations 201 "{\"studentId\":$SC}"
req "Matricular a SD"                         POST /api/periods/$P1/registrations 201 "{\"studentId\":$SD}"
req "Matricular estudiante inexistente"       POST /api/periods/$P1/registrations 404 '{"studentId":999999}'
req "Listar matriculados"                     GET  /api/periods/$P1/registrations 200

section "Épica 6: inscripción, casos de rechazo"
req "Pasar SC a POSTERGACIÓN"                 PATCH /api/students/$SC/status 200 '{"status":"POSTPONED"}'
req "Estudiante no matriculado (SB)"          POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SB,\"courseIds\":[$T101,$T102,$T103]}"
req "Estudiante no REGULAR (SC)"              POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SC,\"courseIds\":[$T101,$T102,$T103]}"
req "Menos de 3 asignaturas"                  POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T101,$T102]}"
req "Asignatura repetida en la selección"     POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T101,$T101,$T102]}"
req "Falta prerrequisito (TAP201 exige 101)"  POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T101,$T102,$T201]}"
req "Asignatura no ofrecida (TAP202)"         POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T202]}"
req "Asignatura de otro plan (TAD101)"        POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$D101]}"
req "Asignatura inexistente"                  POST /api/periods/$P1/enrollments 404 "{\"studentId\":$SA,\"courseIds\":[999999]}"
req "Estudiante inexistente"                  POST /api/periods/$P1/enrollments 404 "{\"studentId\":999999,\"courseIds\":[$T101,$T102,$T103]}"
req "Choque de horario TAP101 / TAP106"       POST /api/periods/$P1/enrollments 409 "{\"studentId\":$SA,\"courseIds\":[$T101,$T106,$T102]}"
req "Sin lista de asignaturas"                POST /api/periods/$P1/enrollments 400 "{\"studentId\":$SA,\"courseIds\":[]}"

section "Épica 6: inscripción, casos de éxito"
req "SA inscribe TAP101, 102 y 103"           POST /api/periods/$P1/enrollments 201 "{\"studentId\":$SA,\"courseIds\":[$T101,$T102,$T103]}"
req "Ya inscrito en TAP101"                   POST /api/periods/$P1/enrollments 409 "{\"studentId\":$SA,\"courseIds\":[$T101,$T104,$T105]}"
req "SA agrega TAP104 y 105 (total 5)"        POST /api/periods/$P1/enrollments 201 "{\"studentId\":$SA,\"courseIds\":[$T104,$T105]}"
req "Sin cupo en TAP104 (capacidad 1)"        POST /api/periods/$P1/enrollments 409 "{\"studentId\":$SD,\"courseIds\":[$T104,$T102,$T103]}"
req "Oferta disponible para SA"               GET  "/api/students/$SA/available-sections?periodId=$P1" 200
has   "  incluye TAP106"                      '"TAP106"'
lacks "  no incluye TAP201 (prerrequisito pendiente)" '"TAP201"'
lacks "  no incluye TAP101 (ya inscrita)"     '"courseCode":"TAP101"'
req "Inscripciones de SA"                     GET  "/api/students/$SA/enrollments?periodId=$P1" 200
E105=$(enr_id TAP105)
req "Anular la inscripción de TAP105"         DELETE /api/enrollments/$E105 204
req "Anular una inscripción inexistente"      DELETE /api/enrollments/999999 404
req "Estudiantes inscritos en TAP101"         GET  /api/sections/$S101/enrollments 200

# ---------------------------------------------------------------------------
section "Épica 7: calificaciones finales"
req "Docente que no es el responsable"        PUT /api/sections/$S101/students/$SA/grade 403 "$(grade $TE2 6.0)"
req "Estudiante no inscrito en la sección"    PUT /api/sections/$S101/students/$SD/grade 422 "$(grade $TE1 6.0)"
req "Nota 7.5 (fuera de rango)"               PUT /api/sections/$S101/students/$SA/grade 400 "$(grade $TE1 7.5)"
req "Nota 0.9 (fuera de rango)"               PUT /api/sections/$S101/students/$SA/grade 400 "$(grade $TE1 0.9)"
req "Nota 5.55 (dos decimales)"               PUT /api/sections/$S101/students/$SA/grade 400 "$(grade $TE1 5.55)"
req "Sección inexistente"                     PUT /api/sections/999999/students/$SA/grade 404 "$(grade $TE1 6.0)"
req "TAP101 nota 6.0 (aprobada)"              PUT /api/sections/$S101/students/$SA/grade 200 "$(grade $TE1 6.0)"
req "TAP102 nota 5.0 (aprobada)"              PUT /api/sections/$S102/students/$SA/grade 200 "$(grade $TE1 5.0)"
req "TAP103 nota 3.0 (reprobada)"             PUT /api/sections/$S103/students/$SA/grade 200 "$(grade $TE1 3.0)"
req "Modificar TAP102 a 5.5"                  PUT /api/sections/$S102/students/$SA/grade 200 "$(grade $TE1 5.5)"
req "Inscripciones de SA con notas"           GET "/api/students/$SA/enrollments?periodId=$P1" 200
E101=$(enr_id TAP101)
req "Anular inscripción con nota registrada"  DELETE /api/enrollments/$E101 409
req "Secciones del docente 1 en el período"   GET "/api/teachers/$TE1/sections?periodId=$P1" 200

section "Épica 7: cierre del período $Y-1"
req "Cierre con TAP104 sin nota (debe fallar)" POST /api/periods/$P1/close 422
has "  informa la sección incompleta"         "Section $S104"
req "TAP104 nota 4.0 (límite: aprobada)"      PUT /api/sections/$S104/students/$SA/grade 200 "$(grade $TE1 4.0)"
req "Cerrar el período"                       POST /api/periods/$P1/close 200
has "  se crearon 4 registros de historial"   '"recordsCreated":4'
has "  el período queda CLOSED"               '"status":"CLOSED"'
req "Cerrar de nuevo"                         POST /api/periods/$P1/close 422
req "Período cerrado: inscribir"              POST /api/periods/$P1/enrollments 422 "{\"studentId\":$SD,\"courseIds\":[$T102,$T103,$T105]}"
req "Período cerrado: modificar nota"         PUT /api/sections/$S102/students/$SA/grade 422 "$(grade $TE1 6.0)"
req "Período cerrado: matricular"             POST /api/periods/$P1/registrations 422 "{\"studentId\":$SB}"
req "Período cerrado: modificar sección"      PUT /api/sections/$S105 422 "{\"teacherId\":$TE1,\"capacity\":20,\"scheduleBlocks\":$(blk M2 J2)}"
req "Historial académico de SA"               GET /api/students/$SA/academic-history 200
has "  TAP103 figura como reprobada"          '"courseCode":"TAP103","courseName":"[^"]*","period":"[0-9-]*","finalGrade":3.0,"result":"FAILED"'
has "  TAP101 figura como aprobada"           '"courseCode":"TAP101","courseName":"[^"]*","period":"[0-9-]*","finalGrade":6.0,"result":"PASSED"'
req "Eliminar estudiante con historial"       DELETE /api/students/$SA 409
req "Cambiar carrera con historial"           PUT /api/students/$SA 409 "{\"fullName\":\"X\",\"email\":\"sa.$U@ipt.cl\",\"careerId\":2}"

# ---------------------------------------------------------------------------
section "Período $Y-2: prerrequisitos aprobados, semestres y segunda reprobación"
sec $P2 $T101 $TE1 30 M4 J4 V4; S2_101=$SEC_ID
sec $P2 $T103 $TE1 30 L1 W1;    S2_103=$SEC_ID
sec $P2 $T105 $TE1 30 L2 W2;    S2_105=$SEC_ID
sec $P2 $T201 $TE1 30 M1 J1 V1; S2_201=$SEC_ID
sec $P2 $T202 $TE1 30 M2 J2 V2; S2_202=$SEC_ID
sec $P2 $T204 $TE1 30 M3 J3 V3; S2_204=$SEC_ID
req "Matricular a SA en $Y-2"                 POST /api/periods/$P2/registrations 201 "{\"studentId\":$SA}"
req "Ya aprobada (TAP101)"                    POST /api/periods/$P2/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T101,$T103,$T105]}"
req "Semestre superior con asignaturas pendientes" POST /api/periods/$P2/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T201,$T202,$T204]}"
req "Falta TAP105 de semestre inferior"       POST /api/periods/$P2/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T103,$T201,$T202]}"
req "Oferta disponible para SA en $Y-2"       GET "/api/students/$SA/available-sections?periodId=$P2" 200
has   "  incluye TAP202 (prerrequisito aprobado)" '"TAP202"'
lacks "  no incluye TAP101 (aprobada)"        '"courseCode":"TAP101"'
req "Reprobada una vez: se puede reinscribir" POST /api/periods/$P2/enrollments 201 "{\"studentId\":$SA,\"courseIds\":[$T103,$T105,$T201]}"
req "TAP103 nota 2.0 (segunda reprobación)"   PUT /api/sections/$S2_103/students/$SA/grade 200 "$(grade $TE1 2.0)"
req "TAP105 nota 5.0"                         PUT /api/sections/$S2_105/students/$SA/grade 200 "$(grade $TE1 5.0)"
req "TAP201 nota 5.0"                         PUT /api/sections/$S2_201/students/$SA/grade 200 "$(grade $TE1 5.0)"
req "Cerrar el período $Y-2"                  POST /api/periods/$P2/close 200
has "  SA queda en la lista de eliminados"    "\"eliminatedStudents\":\[\"SA-$R\"\]"
req "Estado de SA"                            GET /api/students/$SA 200
has "  SA está ELIMINATED"                    '"status":"ELIMINATED"'
req "Cambiar a REGULAR a un ELIMINADO"        PATCH /api/students/$SA/status 422 '{"status":"REGULAR"}'
sec $P3 $T105 $TE1 30 L1 W1; S3_105=$SEC_ID
req "Matricular a SA en $((Y+1))-1"           POST /api/periods/$P3/registrations 201 "{\"studentId\":$SA}"
req "Un ELIMINADO no puede inscribir"         POST /api/periods/$P3/enrollments 422 "{\"studentId\":$SA,\"courseIds\":[$T105]}"

# ---------------------------------------------------------------------------
printf '\nResultado: %d correctas, %d fallidas\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
