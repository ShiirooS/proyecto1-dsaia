# AI Development Log

Proyecto #1 «Desarrollo asistido por IA» (Tópicos Especiales II, Prof. Regis Rivera).
Grupo: Gustavo Sánchez, Bruno Ferreira y Ana Aparicio.
Repositorio: `ShiirooS/proyecto1-dsaia`, fork de `natanael-lima/hotel-reservation-system`.

Como pide el enunciado, aquí no está cada prompt: solo las interacciones con la IA que cambiaron el desarrollo. La bitácora cruda, en orden cronológico, está en [ai-log/raw.md](ai-log/raw.md).

## Herramientas de IA

| Herramienta | Para qué se usó |
|-------------|-----------------|
| Claude Code de escritorio (Claude Opus 5.5 y Claude Sonnet 5.5) | Análisis del repositorio, implementación, pruebas, depuración y verificación manual con su navegador integrado. Los commits hechos con IA lo indican con `Co-Authored-By` (35 con Opus 5.5 y 7 con Sonnet 5.5 en `develop`) |
| Agente arquitecto (`nassa-architect:architect`) | Arquitectura, contrato de API, ADR e historias de usuario detalladas |
| Agente desarrollador web (`nassa-dev-web:web-expert`) | Frontend de HU-04 y HU-02 (login, rutas por rol, calendario) |
| Agente revisor de backend (`senior-architect-backend`) | Revisión de seguridad de los PR, con pruebas de penetración |
| Advisor de Claude Code | Revisión del plan antes de empezar a codificar |

Las herramientas de calidad que validaron el trabajo de la IA no son IA: GitHub Actions (backend y frontend), SonarCloud, el compilador y las suites de pruebas (JUnit + MockMvc y Vitest + Testing Library).

## Registro

| # | Actividad | Herramienta IA | Qué se solicitó | Resultado | Decisión humana |
|---|-----------|----------------|-----------------|-----------|-----------------|
| 1 | Análisis | Claude Code | Leer el enunciado y analizar el repositorio elegido | Diagnóstico: backend solo con el CRUD de hoteles, Spring Security comentado, frontend 100 % maqueta, carpeta `bin/` versionada, columna `availbility` mal escrita | Aceptado como diagnóstico base |
| 2 | Planificación | Advisor de Claude Code | Revisar el plan antes de codificar | Recomendó H2 para las pruebas, CI en GitHub Actions, nada sin PR, solapes validados en el servidor (409) y no guardar número ni CVV de tarjeta | Aceptado |
| 3 | Planificación | Claude Code | Proponer historias de usuario extra | Cuatro propuestas: login JWT, mis reservas y cancelación, reseñas verificadas, tarifas por temporada | El equipo eligió login JWT y mis reservas; luego cambió login por roles (fila 11) |
| 4 | Arquitectura | Agente arquitecto | Arquitectura, contrato REST, ADR e historias detalladas | `API-CONTRACT.md` con unos 30 códigos de error, uso de `BigDecimal` y `Clock`, y 7 ADR | Aceptado como contrato de referencia. Desviaciones decididas al implementar: JWT con `sub` = username, clases JWT en los paquetes existentes, usuarios semilla propios |
| 5 | Análisis | Agente arquitecto | (Dentro de la tarea de arquitectura) | Detectó que `upstream/developer` iba 32 commits adelante de `main` (JWT, reservas, pagos). También afirmó que `develop` solo existía en local | El hallazgo de `developer` se aceptó. La afirmación sobre `develop` era **falsa** y se descartó |
| 6 | Arquitectura | Claude Code | Qué hacer con el hallazgo de `developer` | Recomendó seguir sobre `main` y traer de `developer` solo lo necesario | **Rechazado**: se rehízo todo sobre `developer` (ADR-007). Lo de `main` quedó en ramas `archive/main-*` |
| 7 | Previsión (HU-03) | Agente arquitecto | Algoritmo de previsión; el brief pedía descomposición clásica | Cambió a mínimos cuadrados con tendencia y estacionalidad mensual; demostró con un script que el método ingenuo subestima la pendiente un 25 % | Aceptado tras verificarlo; quedó como ADR-004 con vectores de prueba exactos |
| 8 | Código (HU-04) | Claude Code | `RegisterRequest` con validación de contraseña | Generó la regex `(?=.*\d)` en un String de Java: «illegal escape character» | Corregido a `(?=.*[0-9])` |
| 9 | Código (HU-04) | Claude Code | Activar Spring Security | Al proteger la escritura de hoteles, el test de regresión de HU-00 iba a fallar con 401 | Se decidió que escribir hoteles es solo de ADMIN y el test usa `@WithMockUser(roles="ADMIN")` |
| 10 | Revisión (HU-04 sobre `main`) | Agente revisor de backend | Revisar la seguridad del backend | Aprobar con cambios: secreto JWT por defecto también en producción (firmó su propio token y obtuvo 200), enumeración de usuarios por tiempos, errores 415/406 convertidos en 500 | Hallazgos aceptados y aplicados al código de la nueva base |
| 11 | Análisis de `developer` | Claude Code | Compilar, probar y revisar la nueva base | El único test exigía PostgreSQL; secreto JWT fijo en el código; sin control de roles (cualquier usuario creaba o borraba hoteles); frontend con 20 errores de TypeScript | Base para HU-00. La HU extra de login ya existía, así que se reemplazó por **control de acceso por roles** (la IA recomendaba «búsqueda por fechas») |
| 12 | Código (HU-00) | Claude Code | Portar la base técnica a `developer` | Los tests de caracterización revelaron que `User.role` guardado como ORDINAL impedía crear la tabla `users` en H2 y PostgreSQL | Corregido con `@Enumerated(STRING)` |
| 13 | Código (HU-04) | Claude Code | Implementar RBAC | Lo implementó antes de tener la matriz del contrato, con reglas propias más permisivas (USER usaba el CRUD heredado de reservas y pagos) | **Rechazado y rehecho** al llegar la matriz §13 del contrato |
| 14 | Código (HU-04, frontend) | Agente web | Login, `AuthContext`, `ProtectedRoute`, interceptor 401/403 y tests | 12 tests y luego 17; cuidó el open redirect en `?redirect=`; detectó que el `Header` tenía su propio estado de sesión | Aceptado con una modificación: `ApiError` no exponía `code` ni `errors` del contrato; se añadieron con un test |
| 15 | Revisión (HU-04) | Agente revisor de backend | Revisar el PR de roles con pruebas de penetración | **Rechazar**: 3 fallos críticos explotables (mass assignment por el `id` del cuerpo, pagos accesibles a USER, toma de cuenta al renombrar el usuario) | Los 3 corregidos con tests de regresión. Su sugerencia de quitar el perfil `dev` por defecto se **rechazó** (riesgo acotado: H2 en memoria). Informe en [reviews/HU-04-revision-backend.md](reviews/HU-04-revision-backend.md) |
| 16 | Depuración (HU-04) | Agente revisor y Claude Code | Enumeración de usuarios por tiempos de login | En `developer` no aplica: Spring ya compara contra un hash ficticio | Hallazgo descartado con evidencia; se dejó `LoginEnumerationTest` |
| 17 | Depuración (HU-04) | Claude Code | Probar RBAC con curl contra el backend real | Crear un hotel devolvió 400 | No era un bug: curl en Git Bash mandaba «Panamá» en cp1252. Sin acento, 200 |
| 18 | Proceso (HU-02) | Claude Code | Delegar el frontend del calendario en otra copia del repo | Los documentos de diseño no estaban commiteados y el agente trabajó sin el contrato de API | Lección: commitear los documentos antes de delegar en agentes |
| 19 | Depuración (HU-02) | Claude Code | Smoke test de `/api/room/get-all` | Devolvió 204 vacío y parecía un bug | No era un bug: Spring anuncia el arranque antes de cargar los datos semilla |
| 20 | Revisión (HU-01) | Claude Code (Sonnet 5.5) | Revisar el PR de pago guardado | La validación con `@Pattern` copiaba el número de tarjeta y el CVV al log DEBUG | Corregido con `checkFormat`; `SensitiveDataTest` lo vigila |
| 21 | Entorno | Claude Code | Compilar el backend en la máquina de un integrante | Cientos de «cannot find symbol»: JDK 27 no es compatible con Lombok 1.18.34 | Se instaló aparte el mismo JDK 17 que usa CI, sin tocar el proyecto |
| 22 | Diseño (HU-05) | Claude Code | Retomar HU-05 desde la especificación | Propuso poner los flujos del huésped en `BookingService` y no en el CRUD de admin como decía el brief | Aceptado y anotado como desviación |
| 23 | Código (HU-05) | Claude Code | Cancelación con reembolso | Añadió un bloqueo pesimista para que dos cancelaciones simultáneas no reembolsen dos veces | Aceptado |
| 24 | Testing (HU-05) | Claude Code | Test de integración del historial | La aserción `jsonPath(...).value(List.of(...))` fallaba con «but was null»; el error estaba en la prueba, no en el código | Corregida la prueba con `contains(...)` |
| 25 | Testing (HU-05) | Claude Code | Probar el límite de 48 h | El reloj fijo de los tests no permitía mover la hora entre reservar y cancelar | Se creó un reloj mutable solo para tests: 15:00:00 reembolsa, 15:00:01 no |
| 26 | UI (HU-05) | Claude Code | Reemplazar la maqueta de «Mis reservas» | Quitó los botones «Download PDF» y «Rate & Review», que no hacían nada | Aceptado |
| 27 | Análisis (HU-03) | Claude Code | Retomar el avance «parcial» de HU-03 | El registro de estado decía que el código existía en una rama, pero no estaba en ninguna rama remota | Se reimplementó desde ADR-004; los vectores de prueba pasaron a la primera |
| 28 | Código (HU-03) | Claude Code | Consulta de noches ocupadas por mes | Propuso contar solo habitaciones activas, para que coincida con el denominador | Aceptado y cubierto por test |
| 29 | Testing (HU-03) | Claude Code | Tests de los gráficos | Puso `initialDimension` en el código de producción creyendo que bastaba en jsdom; los gráficos seguían vacíos | **Rechazado**: se quitó y los tests simulan el contenedor |
| 30 | Testing (HU-03) | Claude Code | Verificar que la previsión se dibuja discontinua | La aserción sobre `stroke-dasharray` fallaba porque recharts reescribe el atributo al animar | Aserción eliminada; se verificó a ojo en el navegador |
| 31 | Rendimiento (HU-03) | Claude Code | Revisar el build tras añadir recharts | El bundle pasó de 364 kB a 782 kB para todos los visitantes | El panel de admin se carga aparte (`React.lazy`): el huésped baja 314 kB |
| 32 | QA manual (HU-03, HU-05) | Claude Code (navegador integrado) | Recorrer las pantallas con los usuarios de demo | Cancelaciones con y sin reembolso; dashboard con picos en julio, agosto y diciembre | Umbrales comprobados a mano (85.8 % → +15 %, $145 → $167) |

## Respuestas incorrectas de la IA

| Qué dijo o hizo la IA | Cómo se detectó | Fila |
|-----------------------|-----------------|------|
| Afirmó que la rama `develop` solo existía en local | Con git: `origin/develop` ya estaba publicada | 5 |
| Generó `(?=.*\d)` dentro de un String de Java | El compilador: «illegal escape character» | 8 |
| Implementó permisos más amplios de los acordados | Al comparar con la matriz §13 del contrato | 13 |
| Escribió código de roles con 3 fallos críticos que pasaba sus propios tests | Revisión independiente con pruebas de penetración | 15 |
| Dejó que la validación copiara el número y el CVV al log | Revisión del flujo de pago | 20 |
| Dio por existente un código que nunca se subió | Buscando en las ramas remotas de GitHub | 27 |
| Escribió una aserción de test incorrecta | Al leer el fallo: el código estaba bien | 24 |
| Creyó que `initialDimension` bastaba para dibujar los gráficos en jsdom | Los tests seguían sin gráficos | 29 |
| Afirmó un atributo SVG que recharts cambia al animar | El test fallaba de forma inestable | 30 |

## Cambios de la IA que se rechazaron

| Propuesta | Por qué se rechazó | Fila |
|-----------|--------------------|------|
| Seguir sobre `main` en lugar de `developer` | Duplicaba lo que `developer` ya tenía (ADR-007) | 6 |
| Historia extra «búsqueda por fechas» | Era más urgente cerrar el hueco de seguridad con roles | 11 |
| Matriz de permisos propia, anterior al contrato | Más permisiva que la acordada | 13 |
| Quitar el perfil `dev` por defecto | Riesgo acotado (H2 en memoria, sin datos reales); quedó documentado en el README | 15 |
| Hallazgo de enumeración por tiempos sobre `developer` | No aplicaba en la nueva base; se dejó un test de regresión | 16 |
| `initialDimension` en el código de producción | No arreglaba los tests y ensuciaba producción | 29 |

## Decisiones humanas

1. **Base del proyecto**: trabajar sobre `upstream/developer` y no sobre `main`, en contra de la recomendación de la IA ([ADR-007](architecture/adr/ADR-007-base-developer-en-lugar-de-main.md)).
2. **Historias extra**: control de acceso por roles (HU-04) y mis reservas con cancelación (HU-05), elegidas entre las propuestas de la IA.
3. **Flujo de trabajo**: un issue y una rama por historia, PR a `develop`, merge solo con CI en verde y revisión; los arreglos posteriores van en ramas `fix/HU-XX-...`.
4. **Contrato antes que código**: tras la fila 13, las reglas de negocio se escriben solo después de que exista el contrato.
5. **Riesgo aceptado**: el perfil `dev` sigue siendo el predeterminado, documentado.
6. **Algoritmo de previsión**: se aceptó el método de la IA solo después de verificarlo con vectores de prueba (ADR-004).
7. **Entorno**: JDK 17 aparte, igual que CI, en lugar de cambiar Lombok o la versión de Java del proyecto.
8. **Rendimiento**: cargar el panel de admin bajo demanda para no penalizar a los huéspedes.

Relacionados: [CONTEXTO](CONTEXTO.md), [estado por HU](estado/), [revisiones](reviews/), [ADR](architecture/adr/).
