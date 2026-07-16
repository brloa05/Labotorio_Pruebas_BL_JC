# Laboratorio ARSW - Estrategia Integral de Pruebas

Proyecto base Spring Boot (API de pedidos) usado para aplicar la guía de laboratorio
**"Estrategia Integral de Pruebas para Aplicaciones Web y Microservicios"** (ARSW).

## Estado del laboratorio

| Sección de la guía | Estado | Ubicación |
|---|---|---|
| 1-3. Propósito, resultados de aprendizaje, estrategia por capas | Conceptual, sin código | — |
| 4. Proyecto base Spring Boot | Hecho | `src/main/java/edu/eci/arsw/testing` |
| 5. Pruebas unitarias con JUnit y Mockito | Hecho | `src/test/java/edu/eci/arsw/testing/service/OrderServiceTest.java` |
| 6. Pruebas de API con MockMvc | Hecho | `src/test/java/edu/eci/arsw/testing/controller/OrderControllerTest.java` |
| 7. Pruebas de integración (Spring Boot + Testcontainers) | Hecho | `src/test/java/edu/eci/arsw/testing/integration/OrderIntegrationTest.java` |
| 8. Pruebas de frontend con Playwright | Hecho (proyecto y specs; requiere un frontend real en `localhost:5173` para ejecutarse) | `frontend-tests/` |
| 9. Pruebas de carga con k6 | Hecho (scripts creados y ejecutados contra el backend real) | `load-tests/` |
| 10. Estrategia de pruebas en CI/CD | Hecho | `.github/workflows/arsw-testing-pipeline.yml` |
| 11. Actividades, reto final y rúbrica | Hecho | Ver detalle abajo |

### Proyecto base (sección 4)

API de pedidos (`/orders`) con arquitectura en capas:

```
OrderController -> OrderService -> OrderRepository -> H2 (en memoria)
```

- `model/Order.java` — entidad JPA.
- `dto/CreateOrderRequest.java`, `dto/OrderResponse.java` — DTOs de entrada/salida.
- `repository/OrderRepository.java` — `JpaRepository<Order, String>`.
- `service/OrderService.java` — reglas de negocio (crear pedido, buscar por id, límite de total en 5.000.000).
- `controller/OrderController.java` — endpoints `POST /orders` y `GET /orders/{id}`.

### Pruebas unitarias (sección 5)

`OrderServiceTest` prueba `OrderService` simulando `OrderRepository` con Mockito:

- `shouldCreateOrderWhenRequestIsValid`
- `shouldRejectOrderWhenTotalExceedsLimit`
- `shouldFindOrderByIdWhenOrderExists` — **Actividad 1**
- `shouldThrowExceptionWhenOrderDoesNotExist` — **Actividad 1**

### Pruebas de API (sección 6)

`OrderControllerTest` prueba `OrderController` con `@WebMvcTest` y `MockMvc`:

- `shouldCreateOrder` — valida HTTP 201 y cuerpo JSON.
- `shouldRejectInvalidRequest` — valida HTTP 400 ante datos inválidos.
- `shouldFindOrderById` — **Actividad 2**: valida HTTP 200, id, customerId y status para `GET /orders/{id}`.

### Pruebas de integración (sección 7)

`OrderIntegrationTest` levanta el contexto completo con `@SpringBootTest` y ejecuta
`OrderService` real (sin mocks) contra la base H2 en memoria configurada en
`application.properties`, validando que un pedido creado pueda encontrarse después por id.

Se agregaron además las dependencias de Testcontainers (`org.testcontainers:junit-jupiter`
y `org.testcontainers:postgresql`, scope `test`) en `pom.xml`, tal como lo pide la sección 7.1,
como base para reemplazar H2 por una base PostgreSQL real en Docker en una extensión futura.

### Pruebas de frontend con Playwright (sección 8)

`frontend-tests/` es un proyecto Playwright independiente (`npm init playwright@latest`),
tal como indica la sección 8.1 de la guía. Contiene `tests/orders.spec.js` con las dos pruebas
E2E de la sección 8.2:

- `usuario puede consultar la página principal`
- `usuario puede crear un pedido` (usa los `data-testid` recomendados en la sección 8.3:
  `customer-id`, `order-total`, `create-order`, `order-status`)

Estas pruebas apuntan a `http://localhost:5173`, como en la guía. Este repositorio es solo
backend (no incluye un frontend real), así que las pruebas quedan listas para ejecutarse tan
pronto exista una aplicación frontend corriendo en ese puerto con esos `data-testid`.

```bash
cd frontend-tests
npx playwright test
npx playwright show-report
```

### Pruebas de carga con k6 (sección 9)

`load-tests/` contiene los dos scripts de la guía:

- `load-test.js` — script básico de la sección 9.2 (`vus: 10`, `duration: 30s`, `GET /orders/ORD-1`).
- `create-order-load-test.js` — script con `stages` y `thresholds` de la sección 9.3
  (`POST /orders`, hasta 30 VUs, `http_req_failed rate<0.05`, `http_req_duration p(95)<800ms`).

k6 se instaló con `winget install k6` (opción para Windows de la sección 9.1).

```bash
cd load-tests
k6 run load-test.js
k6 run create-order-load-test.js
```

### Estrategia de pruebas en CI/CD (sección 10)

`.github/workflows/arsw-testing-pipeline.yml` implementa el pipeline de la sección 10.1:
se dispara en cada `push` a `main` y en cada `pull_request`, configura Java 17 (Temurin) y
ejecuta `mvn test` (pruebas unitarias, de API e integración del backend).

Siguiendo la recomendación de la sección 10 sobre momentos de ejecución:

| Momento | Qué se ejecuta actualmente | Qué falta para el pipeline completo de la guía |
|---|---|---|
| Cada commit / push | Compilación + `mvn test` (unitarias + API rápidas) vía el job `backend-tests` | — |
| Pull request | Mismo job `backend-tests` (unitarias, integración y API, ya que `mvn test` las ejecuta todas) | Frontend E2E principales (Playwright) — requiere un frontend real desplegado o levantado en el runner |
| Antes de release | — | Integración completa con Testcontainers, E2E completo, prueba de carga controlada (k6) y reportes de evidencia |

El job actual solo corre `mvn test`, que es rápido porque `OrderIntegrationTest` usa H2 en
memoria; no ejecuta un contenedor Testcontainers real ni las pruebas de Playwright o k6, tal
como sugiere la guía al reservar las pruebas más costosas para pull requests, releases o
ambientes controlados en lugar de cada commit.

## Cómo ejecutar las pruebas

Backend (unitarias, API, integración):

```bash
mvn test
```

Actualmente: **8 tests, 0 fallos**.

Frontend E2E (requiere un frontend corriendo en `localhost:5173`):

```bash
cd frontend-tests
npx playwright test
```

Carga (requiere el backend corriendo en `localhost:8080`, por ejemplo con `mvn spring-boot:run`):

```bash
cd load-tests
k6 run create-order-load-test.js
```

## Actividad 3

> Explique la diferencia entre una prueba unitaria del servicio, una prueba del controlador
> con MockMvc y una prueba de integración con SpringBootTest. Analice rapidez, confianza y
> costo de mantenimiento.

| Tipo de prueba | Qué aísla / qué levanta | Rapidez | Confianza que aporta | Costo de mantenimiento |
|---|---|---|---|---|
| **Unitaria** (`OrderServiceTest`) | Solo `OrderService`; `OrderRepository` se reemplaza por un mock de Mockito. No se levanta el contexto de Spring. | Muy alta (milisegundos, sin I/O ni arranque de framework). | Baja-media: confirma que la lógica de negocio (reglas, cálculos, excepciones) es correcta en aislamiento, pero no dice nada sobre si el servicio está bien conectado al resto de la aplicación. | Muy bajo: al no depender de infraestructura, cambios en la base de datos o en la configuración web no rompen estas pruebas. Son fáciles de mantener y de depurar cuando fallan, porque el error está necesariamente en la clase bajo prueba. |
| **API / MockMvc** (`OrderControllerTest`) | Solo la capa web (`OrderController`) vía `@WebMvcTest`; `OrderService` se reemplaza por un mock (`@MockitoBean`). Levanta un contexto Spring parcial (solo MVC), sin base de datos real. | Alta (segundos; arranca un contexto MVC reducido, no toda la aplicación). | Media: confirma el contrato HTTP — códigos de estado, forma del JSON, validaciones de `@Valid`, serialización — pero no valida que el servicio o la base de datos funcionen correctamente, porque el servicio está mockeado. | Bajo-medio: cambios en el contrato de la API (rutas, campos, validaciones) sí impactan estas pruebas, lo cual es deseable porque detectan rupturas de contrato. No se ven afectadas por cambios internos en persistencia. |
| **Integración** (`OrderIntegrationTest`) | Toda la aplicación real, con `@SpringBootTest`: `OrderService`, `OrderRepository` y la base de datos (H2, o PostgreSQL real con Testcontainers) trabajando juntos, sin mocks. | Baja (varios segundos; levanta el contexto completo de Spring, el `EntityManagerFactory`, el pool de conexiones, etc.). Con Testcontainers es aún más lenta porque además arranca un contenedor Docker. | Alta: es la que más se parece a un escenario real, porque valida que las capas realmente se integren (mapeo JPA, transacciones, consultas) y no solo que cada pieza funcione por separado. | Alto: al depender de más piezas (contexto de Spring, esquema de base de datos, y en el caso de Testcontainers, de Docker disponible en el entorno), es más frágil ante cambios de infraestructura y más lenta de ejecutar en cada commit. Cuando falla, el diagnóstico es más costoso porque el problema puede estar en cualquiera de las capas involucradas. |

**Conclusión práctica:** las tres capas no compiten entre sí, se complementan siguiendo la
pirámide de pruebas de la sección 3 de la guía. Se debe tener muchas pruebas unitarias
(rápidas, baratas, feedback inmediato), un número medio de pruebas de API/MockMvc (para
proteger el contrato HTTP) y pocas pruebas de integración (las más lentas y costosas,
reservadas para donde realmente se necesita confianza de que las piezas encajan), dejando las
pruebas de integración con Testcontainers para el pipeline de pull request o release en lugar
de cada commit, como recomienda la sección 10 de la guía.

## Actividad 4

> Diseñe tres pruebas E2E: crear pedido exitosamente, mostrar error si el total es inválido y
> consultar un pedido por ID. Para cada una indique flujo, datos de entrada y resultado
> esperado.

| Prueba | Flujo | Datos de entrada | Resultado esperado |
|---|---|---|---|
| **Crear pedido exitosamente** | 1. El usuario abre la página principal (`/`). 2. Completa el campo `customer-id`. 3. Completa el campo `order-total`. 4. Hace clic en `create-order`. | `customerId = "CUS-01"`, `total = 120000` | El elemento `order-status` muestra el texto `CREATED`, confirmando que el pedido se creó vía `POST /orders` y el backend respondió `201`. |
| **Mostrar error si el total es inválido** | 1. El usuario abre la página principal (`/`). 2. Completa `customer-id`. 3. Completa `order-total` con un valor inválido (negativo o vacío). 4. Hace clic en `create-order`. | `customerId = "CUS-01"`, `total = -10` | La interfaz muestra un mensaje de error visible al usuario (por ejemplo un elemento `order-error`) en lugar de `CREATED`, reflejando el `400 Bad Request` que devuelve `POST /orders` cuando la validación (`@NotBlank`, `@Min(1)`) falla. |
| **Consultar un pedido por ID** | 1. El usuario crea un pedido (o se usa uno ya existente). 2. Ingresa el id del pedido en un campo de búsqueda (por ejemplo `order-id-input`). 3. Hace clic en un botón de consulta (`search-order`). | `id = "ORD-1"` (id devuelto al crear el pedido) | La página muestra los datos del pedido (`customerId`, `total`, `status`) obtenidos de `GET /orders/{id}`, correspondientes al `200 OK` devuelto por el backend. |

Estas tres pruebas son un diseño (flujo, datos, resultado esperado) siguiendo el formato de la
guía; no se implementan como código Playwright porque, como se explica en la sección 8 más
arriba, este repositorio no incluye un frontend real contra el cual ejecutarlas.

## Actividad 5

> Ejecute una prueba de carga con k6 y documente usuarios virtuales, duración, total de
> solicitudes, porcentaje de fallos, p95 de latencia, resultado de thresholds y conclusión
> técnica.

Se ejecutó `load-tests/create-order-load-test.js` (sección 9.3, con `stages` y `thresholds`)
contra el backend real corriendo localmente (`mvn spring-boot:run`, puerto 8080), atacando
`POST /orders`.

| Métrica | Resultado |
|---|---|
| Usuarios virtuales (VUs) | Rampa de 0 → 10 → 30 → 0 (`stages`: 20s a 10 VUs, 30s a 30 VUs, 20s bajando a 0) |
| Duración total | 1m10s (más *graceful stop*) |
| Total de solicitudes (`http_reqs`) | 993 |
| Iteraciones completas | 993 (0 interrumpidas) |
| Porcentaje de fallos (`http_req_failed`) | 0.00% (0 de 993) |
| Latencia promedio (`http_req_duration avg`) | 9.22 ms |
| Latencia p95 (`http_req_duration p(95)`) | 20.14 ms |
| Latencia máxima | 268.71 ms |
| Threshold `http_req_failed rate<0.05` | ✅ cumplido (0.00% < 5%) |
| Threshold `http_req_duration p(95)<800` | ✅ cumplido (20.14ms < 800ms) |
| Checks | 1986/1986 exitosos (100%) — `created` (HTTP 201) y `duration < 800ms` |

**Conclusión técnica:** con hasta 30 usuarios virtuales concurrentes creando pedidos, el
endpoint `POST /orders` respondió sin errores (0% de fallos) y con una latencia muy por debajo
del umbral definido (p95 de ~20 ms frente a un límite de 800 ms), es decir, con margen amplio
respecto al límite exigido. La latencia máxima puntual (268.71 ms) sugiere una posible
variación aislada (por ejemplo, un warm-up de JIT/Hibernate al inicio de la prueba) que no
afectó el percentil 95 ni el resultado de los thresholds. Para esta carga y esta base de datos
en memoria (H2), el servicio se mantiene estable y no muestra señales de degradación; una
prueba concluyente sobre el comportamiento en producción requeriría repetir el ensayo contra
una base de datos real (por ejemplo PostgreSQL vía Testcontainers, como en la sección 7.1) y
con volúmenes de carga más altos para encontrar el punto de quiebre del sistema.

## Actividad integradora (11.1)

> Diseñe una estrategia de pruebas para una aplicación de comercio electrónico con frontend
> React, backend Spring Boot, base de datos PostgreSQL, API REST, autenticación y despliegue
> en AWS: tipos de pruebas, herramientas, capa que valida cada prueba, momento de ejecución en
> el pipeline, errores que podría detectar y evidencia que genera.

| Tipo de prueba | Herramientas | Capa que valida | Momento en el pipeline | Errores que detecta | Evidencia que genera |
|---|---|---|---|---|---|
| Unitaria (backend) | JUnit, Mockito | Servicios y reglas de negocio de Spring Boot, aislados de la base de datos | Cada commit | Cálculos o reglas de negocio incorrectas, excepciones no lanzadas donde corresponde | Reporte Surefire, cobertura de código |
| Unitaria (frontend) | Vitest/Jest, Testing Library | Componentes React aislados | Cada commit | Renderizado incorrecto, props/estado mal manejados, lógica de UI rota | Reporte del test runner, cobertura |
| API | MockMvc, REST Assured | Controladores REST (contrato HTTP: rutas, códigos, JSON, validaciones) | Cada commit / pull request | Rupturas de contrato de API, validaciones (`@Valid`) que dejan de aplicarse, serialización incorrecta | Reporte de tests, logs de la capa web |
| Integración | `@SpringBootTest`, Testcontainers con PostgreSQL real | Servicio + repositorio + base de datos real (no H2) | Pull request | Mapeo JPA incorrecto, queries mal escritas, problemas de transacciones que no aparecen con una base en memoria | Reporte de tests, logs de arranque de Testcontainers |
| Seguridad / autenticación | Spring Security Test, pruebas de tokens JWT/OAuth | Filtros de seguridad y control de acceso | Pull request | Endpoints sin protección, tokens inválidos o expirados aceptados, escalamiento de privilegios | Reporte de tests de seguridad, logs de intentos de acceso denegado |
| E2E de frontend | Playwright, Cypress | Flujo completo de usuario (frontend + backend + base de datos desplegados en un ambiente real o de staging) | Pull request (flujos críticos: login, checkout) / antes de release (suite completa) | Rupturas en flujos de negocio críticos (agregar al carrito, pagar, iniciar sesión) que las pruebas por capas no detectan | Reporte HTML de Playwright, capturas de pantalla y videos de fallos |
| Carga | k6 | Sistema completo bajo concurrencia (API + base de datos + infraestructura AWS) | Antes de release, en un ambiente controlado (staging) | Degradación de latencia, errores bajo concurrencia, cuellos de botella en el pool de conexiones o en la base de datos | Métricas de k6 (p95, `http_req_failed`, throughput), resultado de thresholds |
| Pipeline / CI-CD | GitHub Actions | Todas las capas anteriores, de forma repetible | Continuo (cada push/PR dispara las pruebas rápidas; release dispara las costosas) | Regresiones que un desarrollador olvidó verificar manualmente antes de integrar su cambio | Historial de ejecuciones de Actions, logs, badges de estado del pipeline |

## Reto final (11.2)

Checklist de la guía y su estado en este repositorio:

| # | Tarea del reto final | Estado | Evidencia |
|---|---|---|---|
| 1 | Implementar una prueba unitaria funcional | ✅ | `OrderServiceTest` (sección 5) |
| 2 | Implementar una prueba de API con MockMvc | ✅ | `OrderControllerTest` (sección 6) |
| 3 | Implementar una prueba de integración | ✅ | `OrderIntegrationTest` (sección 7) |
| 4 | Proponer o implementar una prueba E2E de frontend | ✅ (propuesta + specs Playwright, sin frontend real) | `frontend-tests/`, Actividad 4 (sección 8) |
| 5 | Crear un script k6 de carga | ✅ | `load-tests/` (sección 9) |
| 6 | Ejecutar las pruebas y anexar evidencia | ✅ | `mvn test` (8/8 verde), `load-tests/k6-result-actividad5.txt` |
| 7 | Analizar métricas de carga | ✅ | Actividad 5 (sección 9) |
| 8 | Proponer un pipeline de pruebas | ✅ | `.github/workflows/arsw-testing-pipeline.yml` (sección 10) |
| 9 | Reflexionar sobre qué pruebas aportan más valor al sistema | ✅ | Ver reflexión abajo |

### Reflexión: ¿qué pruebas aportan más valor al sistema?

No hay un único tipo de prueba que domine; el valor depende de qué pregunta responde cada una:

- Las **pruebas unitarias** (`OrderServiceTest`) son las que más valor aportan **por peso
  invertido**: cuestan casi nada (milisegundos, sin infraestructura) y detectan el error más
  cerca de su origen, en la lógica de negocio (por ejemplo, el límite de 5.000.000 en
  `OrderService`). Son la base de la pirámide y las que se deben tener en mayor cantidad.
- Las **pruebas de API** (`OrderControllerTest`) aportan el valor más específico: protegen el
  contrato con quien consuma la API (frontend, otros servicios), algo que ni la prueba unitaria
  ni la de integración validan directamente (código HTTP, forma del JSON, validación de
  entrada).
- Las **pruebas de integración** (`OrderIntegrationTest`, sección 7.1 con Testcontainers)
  aportan el valor más **realista**: son las únicas que hubieran detectado, por ejemplo, un
  mapeo JPA incorrecto o una consulta que funciona en H2 pero falla en PostgreSQL. Su costo es
  mayor, así que se reservan para menos casos.
- Las **pruebas E2E** (sección 8) aportan valor de **negocio**: validan que el usuario final
  realmente pueda completar un flujo crítico, algo que ninguna prueba de capas inferiores
  garantiza por sí sola (todas las piezas pueden pasar sus pruebas individuales y aun así el
  flujo de extremo a extremo estar roto por un problema de integración de UI).
- Las **pruebas de carga** (sección 9, Actividad 5) aportan un valor distinto a las demás: no
  buscan defectos funcionales sino límites de capacidad y degradación bajo concurrencia, algo
  que solo se observa ejecutando el sistema real bajo tráfico simulado.

En términos de retorno por esfuerzo, para este proyecto las pruebas unitarias y de API son las
que dan más señal por cada minuto invertido, y por eso corren en cada commit
(`.github/workflows/arsw-testing-pipeline.yml`, sección 10). Las pruebas de integración, E2E y
de carga aportan una confianza que las anteriores no pueden dar, pero a un costo de tiempo y
de infraestructura mucho mayor, por lo que tiene sentido reservarlas para pull requests o
releases en lugar de cada commit.

## Rúbrica

| Criterio | Descripción | Peso |
|---|---|---|
| Pruebas unitarias | Valida lógica de negocio con aislamiento | 15% |
| Pruebas de API | Verifica endpoints, estados HTTP y JSON | 15% |
| Pruebas de integración | Valida interacción entre capas | 20% |
| Pruebas frontend | Propone o implementa flujo E2E automatizado | 15% |
| Pruebas de carga | Diseña y ejecuta prueba con k6 | 20% |
| Análisis técnico | Interpreta resultados y propone mejoras | 15% |
