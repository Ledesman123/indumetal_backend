# Backend — Intranet de Gestión de Almacén — INDUMETAL PERÚ S.A.C.

API REST desarrollada con **Java 17 + Spring Boot 3**, persistencia en
**Supabase (PostgreSQL)**, migraciones versionadas con **Flyway**, seguridad
**JWT**, y arquitectura en capas organizada por módulo de negocio
("package by feature"). Corresponde al **Sprint 1 — Backend y Base de Datos**
del proyecto (ver informe del curso).

Este documento explica **cómo está construido el proyecto, paso a paso**,
para que puedas entenderlo, defenderlo y seguir trabajando sobre él (Sprint 2:
Angular; Sprint 3: integración; Sprint 4: despliegue).

---

## 0. Qué se hizo en esta sesión (resumen ejecutivo)

Partimos de una base ya bastante avanzada (la que subiste en el `.zip`), que
cubría 18 de los 20 requerimientos del Product Backlog. Se completó lo que
faltaba y se corrigieron un par de detalles:

| # | Cambio | Módulo | Por qué |
|---|--------|--------|---------|
| 1 | Cuenta bloqueada (`LockedException`) ahora responde `423 Locked` en vez de `500` | `common/exception` | El `GlobalExceptionHandler` no tenía un handler específico para esa excepción y cualquier intento de login con la cuenta bloqueada devolvía un error genérico de servidor. |
| 2 | RF-07 implementado de verdad: asignación **automática** de ubicación en ingresos | `movimiento` | `ubicacionId` era obligatorio en `IngresoRequest`; no existía ninguna lógica de "auto-asignación" pese a que el RF y el README lo mencionaban. |
| 3 | **RF-14 — Módulo `dashboard` completo** (nuevo) | `dashboard/` | No existía. Se agregó resumen general, ranking de materiales con mayor movimiento, rotación de inventario, valorización del inventario y un indicador de nivel de cumplimiento de despacho. |
| 4 | **RF-15 — Módulo `reporte` completo** (nuevo) | `reporte/` | No existía. Se agregó exportación de movimientos, kardex y stock actual en **PDF** (OpenPDF) y **Excel** (Apache POI). |
| 5 | Campo `costoUnitario` en `Material` (+ migración `V2`) | `material` | Necesario para calcular la valorización del inventario (RF-14); no existía ningún campo de costo en el modelo original. |
| 6 | `.gitignore` agregado | raíz | El `.env` con credenciales reales de Supabase no estaba protegido de subirse a un repositorio Git. |

> ⚠️ **Importante sobre seguridad:** el archivo `.env` que traía el proyecto
> contiene una contraseña real de tu base de datos Supabase. Como ese archivo
> viajó dentro del `.zip` que compartiste, **te recomiendo rotar esa
> contraseña** desde Supabase (Project Settings → Database → Reset password)
> y actualizar tu `.env` local con la nueva. Nunca subas `.env` a GitHub — para
> eso quedó agregado el `.gitignore`.

---

## 1. Arquitectura general

```
Cliente (Angular, Sprint 2)
        │  HTTPS + JWT (Bearer token)
        ▼
┌──────────────────────────────────────────────────────┐
│                  Spring Boot API                       │
│                                                          │
│  Controller  →  Service  →  Repository                  │
│  (REST)         (reglas      (Spring Data JPA)           │
│                  de negocio)                             │
│                                                          │
│  Security: JwtAuthenticationFilter + SecurityConfig      │
│  Common:   ApiResponse, excepciones, auditoría            │
└──────────────────────────────────────────────────────┘
        │  JDBC (SSL)
        ▼
   Supabase (PostgreSQL)
   - Flyway gestiona el esquema (db/migration)
```

**Patrón de capas por módulo** (ejemplo con `material`):

```
Material.java            → Entidad JPA (tabla materiales)
MaterialRepository.java  → Acceso a datos (Spring Data JPA + Specification)
MaterialService.java     → Contrato del servicio
MaterialServiceImpl.java → Reglas de negocio (validaciones, orquestación)
MaterialController.java  → Endpoints REST (valida entrada, delega al service)
dto/MaterialRequest.java / MaterialResponse.java → Contratos de entrada/salida
```

Módulos simples (bajo nivel de complejidad, ej. `almacen`) embeben la lógica
directamente en el Controller para no sobre-diseñar; los módulos con reglas
de negocio (stock, movimientos, inventario físico, auth) sí separan
Service/ServiceImpl.

---

## 2. Estructura de paquetes (actualizada)

```
com.indumetal.almacen
├── AlmacenApplication.java
├── config/
│   ├── SecurityConfig.java        Reglas HTTP, BCrypt, filtro JWT
│   ├── CorsConfig.java            CORS para el frontend Angular
│   └── JpaAuditingConfig.java     Habilita creadoEn/actualizadoEn automáticos
├── security/
│   ├── JwtService.java            Generación/validación de tokens
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityUser.java          Adaptador Usuario -> UserDetails
│   └── UserDetailsServiceImpl.java
├── common/
│   ├── dto/ApiResponse.java       Contrato estándar { success, message, data }
│   ├── dto/PageResponse.java      Contrato estándar de paginación
│   ├── audit/Auditable.java       Superclase con fechas de auditoría
│   └── exception/                 Excepciones de negocio + GlobalExceptionHandler
└── modules/
    ├── rol/                       RF-02
    ├── usuario/                   RF-01, RF-03
    ├── auth/                      RF-01 (login)
    ├── material/                  RF-04, RF-16, RF-19 (+ costoUnitario para RF-14)
    ├── almacen/                   RF-05
    ├── ubicacion/                 RF-05, RF-07
    ├── stock/                     RF-10 (saldo en tiempo real)
    ├── movimiento/                RF-06, RF-07, RF-08, RF-09, RF-13, RF-17, RF-18
    ├── kardex/                    RF-09, RF-10 (consultas)
    ├── alerta/                    RF-11 (job programado + correo)
    ├── inventariofisico/          RF-12
    ├── auditoria/                 RF-20
    ├── dashboard/  ⭐ NUEVO       RF-14 (indicadores)
    └── reporte/    ⭐ NUEVO       RF-15 (exportación PDF/Excel)
```

**Todos los módulos nuevos que agregues (ej. "proveedores") deben seguir el
mismo patrón**: entidad → repository → service (si hay lógica) → controller →
dto/.

---

## 3. Cómo se resuelve cada requerimiento (RF) — mapa completo

| RF | Descripción | Dónde vive |
|----|-------------|------------|
| RF-01 | Login JWT, bloqueo tras 5 intentos | `auth/AuthService`, `security/JwtService` |
| RF-02 | 4 roles con permisos por endpoint | `rol/RolNombre`, `@PreAuthorize` en cada Controller |
| RF-03 | CRUD de usuarios (solo Admin) | `usuario/UsuarioServiceImpl` |
| RF-04 | CRUD de materiales | `material/MaterialServiceImpl` |
| RF-05 | Almacenes y ubicaciones | `almacen/`, `ubicacion/` |
| RF-06 | Registro de ingresos | `movimiento/MovimientoService#registrarIngreso` |
| RF-07 | Asignación de ubicación (auto/manual) | `MovimientoService#asignarUbicacionAutomatica` ⭐ completado hoy |
| RF-08 | Registro de salidas a producción | `MovimientoService#registrarSalida` |
| RF-09 | Kardex automático | `kardex/KardexController` (consulta `movimientos_almacen`) |
| RF-10 | Stock en tiempo real | `stock/StockUbicacion` (tabla materializada, actualizada transaccionalmente) |
| RF-11 | Alertas por correo (stock mínimo) | `alerta/AlertaStockService` (`@Scheduled`) |
| RF-12 | Inventario físico / cíclico | `inventariofisico/InventarioFisicoService` |
| RF-13 | Trazabilidad por lote/vencimiento | Campos `lote`, `fechaVencimiento` en `MovimientoAlmacen` |
| RF-14 | Dashboard de indicadores | `dashboard/` ⭐ **nuevo módulo** |
| RF-15 | Reportes PDF/Excel | `reporte/` ⭐ **nuevo módulo** |
| RF-16 | Buscador y filtros de materiales | `material/MaterialSpecification` |
| RF-17 | Devolución de materiales | `MovimientoService#registrarDevolucion` |
| RF-18 | Transferencia entre almacenes | `MovimientoService#registrarTransferencia` |
| RF-19 | Imagen de referencia del material | Campo `imagenUrl` en `Material` |
| RF-20 | Bitácora de auditoría | `auditoria/AuditoriaService` (invocada desde los services de negocio) |

---

## 4. Reglas de negocio importantes (para defender el proyecto)

- **Stock en tiempo real (RF-10)**: vive en `stock_ubicacion`, actualizada de
  forma transaccional por `MovimientoService` en cada ingreso/salida/
  devolución/transferencia. Usa bloqueo optimista (`@Version`) para que varios
  almaceneros puedan trabajar simultáneamente sin condiciones de carrera.
- **Kardex (RF-09)**: no es una tabla aparte; se construye consultando
  `movimientos_almacen` ordenado cronológicamente (`KardexItem.fromMovimiento`).
- **Asignación automática de ubicación (RF-07)** ⭐: cuando un ingreso no
  especifica `ubicacionId`, `MovimientoService.asignarUbicacionAutomatica`
  aplica esta estrategia:
  1. Si el material **ya tiene stock** en alguna ubicación, se reutiliza esa
     misma ubicación (evita fragmentar el mismo material en muchos sitios).
  2. Si es la **primera vez** que ingresa ese material, se asigna la primera
     ubicación activa registrada en el sistema.
  Si prefieres otra estrategia (ej. por categoría de material, o por
  capacidad disponible), este es el único método que hay que tocar.
- **Alertas de stock mínimo (RF-11)**: job `@Scheduled` (cron configurable en
  `application.yml -> app.stock.revisar-cron`, por defecto todos los días
  7:00 a.m.) que revisa `stock_ubicacion` y envía correo a Supervisores y
  Administradores activos.
- **Inventario físico (RF-12)**: al *iniciar* toma una "foto" del stock del
  sistema; al *cerrar*, ajusta el stock real al conteo físico registrado.
- **Auditoría (RF-20)**: `AuditoriaService.registrar(...)` se invoca
  explícitamente desde los services de negocio (ya conectado en `Material` y
  `MovimientoAlmacen`); replica el patrón en los servicios que agregues.
- **Dashboard (RF-14)** ⭐ — nota metodológica importante: el indicador
  "nivel de cumplimiento de despacho" pedido en el RF-14 normalmente se mide
  como *(pedidos atendidos completos / total de pedidos solicitados)*, pero el
  Product Backlog **no incluye una entidad de "solicitud/orden de despacho"**
  separada de los movimientos ya ejecutados (está fuera del alcance definido).
  Por eso se implementó como una métrica *proxy*, documentada en el código
  (`CumplimientoDespachoResponse`): el porcentaje de materiales activos cuyo
  stock actual está en o por encima de su stock mínimo (es decir, materiales
  que el almacén puede despachar sin quiebre en este momento). Si en Sprint 2
  o más adelante agregan un módulo de "solicitudes de despacho", ese sería el
  momento de recalcular este indicador de forma más precisa.
- **Reportes (RF-15)** ⭐: no se guarda nada nuevo en la base de datos; el
  documento (PDF/Excel) se arma en memoria a partir de los datos que ya
  existen (`movimientos_almacen`, `stock_ubicacion`) y se entrega como
  descarga (`Content-Disposition: attachment`).

---

## 5. Configurar la conexión a Supabase

1. En tu proyecto de Supabase: **Project Settings → Database → Connection string**.
2. Copia la cadena en modo **Session pooler** (puerto `6543`, recomendado) o
   **Direct connection** (puerto `5432`).
3. Copia `.env.example` a `.env` (o configura las variables en tu IDE/servidor)
   y completa:

```
SUPABASE_DB_URL=jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=tu_password
JWT_SECRET=clave-larga-y-aleatoria
```

4. Al iniciar la aplicación, **Flyway crea automáticamente todas las tablas**:
   - `V1__init_schema.sql`: esquema completo (roles, usuarios, almacenes,
     ubicaciones, materiales, stock, movimientos, inventario físico,
     auditoría), incluyendo los 4 roles y un usuario administrador semilla.
   - `V2__add_costo_unitario_material.sql` ⭐ (nueva): agrega la columna
     `costo_unitario` a `materiales`, usada por el dashboard (RF-14).

   Usuario administrador semilla:
   - Correo: `admin@indumetal.pe`
   - Password: `Admin#2026`

   **Cambia esta contraseña apenas inicies sesión por primera vez.**

---

## 6. Ejecutar en local

```bash
export $(cat .env | xargs)   # carga las variables de entorno (Linux/Mac)
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.
Documentación interactiva (Swagger): `http://localhost:8080/swagger-ui.html`

> Nota: este entorno de revisión no tenía acceso a internet ni a un caché de
> Maven, así que el código se validó con una revisión manual exhaustiva
> (imports, tipos, firmas de métodos, JPQL) en vez de una compilación real.
> Antes de tu primera ejecución, corre `mvn clean compile` y avísame si sale
> algún error — es la validación que faltó hacer en este entorno.

---

## 7. Probar el login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@indumetal.pe","password":"Admin#2026"}'
```

Respuesta:
```json
{
  "success": true,
  "message": "Inicio de sesion exitoso",
  "data": {
    "token": "eyJhbGciOi...",
    "tipo": "Bearer",
    "usuarioId": 1,
    "nombreCompleto": "Administrador General",
    "rol": "ADMINISTRADOR"
  }
}
```

Usa ese token en cada petición posterior:
```
Authorization: Bearer eyJhbGciOi...
```

---

## 8. Endpoints principales

| Módulo | Método y ruta | RF | Rol requerido |
|---|---|---|---|
| Auth | `POST /api/auth/login` | RF-01 | Público |
| Usuarios | `POST/PUT/GET /api/usuarios` | RF-03 | ADMINISTRADOR |
| Materiales | `POST/PUT/GET /api/materiales` | RF-04, RF-16, RF-19 | Todos (lectura) / Almacenero+ (escritura) |
| Almacenes | `POST/GET /api/almacenes` | RF-05 | Supervisor/Admin |
| Ubicaciones | `POST /api/ubicaciones`, `GET /api/ubicaciones/por-almacen/{id}` | RF-05, RF-07 | Supervisor/Admin |
| Movimientos | `POST /api/movimientos/ingresos` (`ubicacionId` opcional, ver RF-07) | RF-06, RF-07 | Almacenero+ |
| Movimientos | `POST /api/movimientos/salidas` | RF-08 | Almacenero+ |
| Movimientos | `POST /api/movimientos/devoluciones` | RF-17 | Almacenero+ |
| Movimientos | `POST /api/movimientos/transferencias` | RF-18 | Almacenero+ |
| Kardex | `GET /api/materiales/{id}/kardex` | RF-09 | Autenticado |
| Stock | `GET /api/materiales/{id}/stock` | RF-10 | Autenticado |
| Inventario físico | `POST /api/inventarios-fisicos` ... `/{id}/cerrar` | RF-12 | Almacenero+ |
| Auditoría | `GET /api/auditoria` | RF-20 | ADMINISTRADOR |
| **Dashboard** ⭐ | `GET /api/dashboard/resumen` | RF-14 | Supervisor/Jefe Producción/Admin |
| **Dashboard** ⭐ | `GET /api/dashboard/materiales-mas-movimiento?desde=&hasta=&limite=` | RF-14 | Supervisor/Jefe Producción/Admin |
| **Dashboard** ⭐ | `GET /api/dashboard/rotacion-inventario?desde=&hasta=` | RF-14 | Supervisor/Jefe Producción/Admin |
| **Dashboard** ⭐ | `GET /api/dashboard/valorizacion` | RF-14 | Supervisor/Jefe Producción/Admin |
| **Dashboard** ⭐ | `GET /api/dashboard/cumplimiento-despacho` | RF-14 | Supervisor/Jefe Producción/Admin |
| **Reportes** ⭐ | `GET /api/reportes/movimientos/pdf?desde=&hasta=[&materialId=]` | RF-15 | Todos los roles operativos |
| **Reportes** ⭐ | `GET /api/reportes/movimientos/excel?desde=&hasta=[&materialId=]` | RF-15 | Todos los roles operativos |
| **Reportes** ⭐ | `GET /api/reportes/kardex/{materialId}/pdf` | RF-15 | Todos los roles operativos |
| **Reportes** ⭐ | `GET /api/reportes/kardex/{materialId}/excel` | RF-15 | Todos los roles operativos |
| **Reportes** ⭐ | `GET /api/reportes/stock/excel` | RF-15 | Todos los roles operativos |

Todos los endpoints (excepto los de `reporte`, que devuelven el archivo
binario directamente) devuelven el mismo contrato:
```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

### Ejemplos rápidos de los módulos nuevos

```bash
# Resumen del dashboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/dashboard/resumen

# Top 5 materiales con más movimiento en septiembre 2026
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/dashboard/materiales-mas-movimiento?desde=2026-09-01&hasta=2026-09-30&limite=5"

# Descargar reporte de movimientos en Excel
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reportes/movimientos/excel?desde=2026-09-01&hasta=2026-09-30" \
  -o movimientos.xlsx

# Descargar el kardex de un material en PDF
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reportes/kardex/1/pdf" -o kardex_material_1.pdf
```

---

## 9. Manejo de errores (HTTP)

| Excepción | Código HTTP | Cuándo ocurre |
|---|---|---|
| `ResourceNotFoundException` | 404 | El id solicitado no existe |
| `BusinessException` | 422 | Regla de negocio violada (ej. stock insuficiente) |
| `UnauthorizedException`, `BadCredentialsException`, `DisabledException` | 401 | Credenciales inválidas o usuario inactivo |
| `LockedException` ⭐ | 423 | Cuenta bloqueada tras 5 intentos fallidos (antes devolvía 500 por error) |
| `AccessDeniedException` | 403 | El rol autenticado no tiene permiso para el endpoint |
| `MethodArgumentNotValidException` | 400 | Falla de validación (`@Valid`) con detalle por campo |
| Cualquier otra `Exception` | 500 | Error interno no controlado |

---

## 10. Próximos pasos sugeridos

1. **Ahora mismo**: correr `mvn clean compile` (o abrir en tu IDE) para
   confirmar que compila en tu máquina — ver nota de la sección 6.
2. **Rotar la contraseña de Supabase** (ver advertencia al inicio de este
   documento) y actualizar tu `.env` local.
3. Escribir pruebas unitarias con `spring-boot-starter-test` (ya está en el
   `pom.xml`).
4. **Sprint 2**: construir el frontend en Angular consumiendo esta API
   (contrato `ApiResponse` uniforme facilita el manejo de respuestas).
5. **Sprint 4**: dockerizar (`Dockerfile` + `docker-compose`) y desplegar.
