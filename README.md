# Backend — Intranet de Gestión de Almacén — INDUMETAL PERÚ S.A.C.

API REST desarrollada con **Java 17 + Spring Boot 3**, persistencia en
**Supabase (PostgreSQL)**, migraciones versionadas con **Flyway**, seguridad
**JWT**, y arquitectura en capas organizada por módulo de negocio
("package by feature"). Corresponde al **Sprint 1 — Backend y Base de Datos**
del proyecto (ver informe del curso).

Este documento explica **cómo está construido el proyecto, paso a paso**,
para que puedas entenderlo, defenderlo y seguir trabajando sobre él (Sprint 2:
Angular; Sprint 3: integración; Sprint 4: despliegue).



##  Arquitectura general

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

##  Estructura de paquetes (actualizada)

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
    ├── rol/                       
    ├── usuario/                   
    ├── auth/                      
    ├── material/                  
    ├── almacen/                   
    ├── ubicacion/                 
    ├── stock/                     
    ├── movimiento/                
    ├── kardex/                    
    ├── alerta/                    
    ├── inventariofisico/          
    ├── auditoria/                 
    ├── dashboard/                 
    └── reporte/                   
```

**Todos los módulos nuevos que agregues (ej. "proveedores") deben seguir el
mismo patrón**: entidad → repository → service (si hay lógica) → controller →
dto/.

## Reglas de negocio importantes 

- **Stock en tiempo real **: vive en `stock_ubicacion`, actualizada de
  forma transaccional por `MovimientoService` en cada ingreso/salida/
  devolución/transferencia. Usa bloqueo optimista (`@Version`) para que varios
  almaceneros puedan trabajar simultáneamente sin condiciones de carrera.
- **Kardex (RF-09)**: no es una tabla aparte; se construye consultando
  `movimientos_almacen` ordenado cronológicamente (`KardexItem.fromMovimiento`).
- **Asignación automática de ubicación ** ⭐: cuando un ingreso no
  especifica `ubicacionId`, `MovimientoService.asignarUbicacionAutomatica`
  aplica esta estrategia:
  1. Si el material **ya tiene stock** en alguna ubicación, se reutiliza esa
     misma ubicación (evita fragmentar el mismo material en muchos sitios).
  2. Si es la **primera vez** que ingresa ese material, se asigna la primera
     ubicación activa registrada en el sistema.
  Si prefieres otra estrategia (ej. por categoría de material, o por
  capacidad disponible), este es el único método que hay que tocar.
- **Alertas de stock mínimo **: job `@Scheduled` (cron configurable en
  `application.yml -> app.stock.revisar-cron`, por defecto todos los días
  7:00 a.m.) que revisa `stock_ubicacion` y envía correo a Supervisores y
  Administradores activos.
- **Inventario físico **: al *iniciar* toma una "foto" del stock del
  sistema; al *cerrar*, ajusta el stock real al conteo físico registrado.
- **Auditoría **: `AuditoriaService.registrar(...)` se invoca
  explícitamente desde los services de negocio (ya conectado en `Material` y
  `MovimientoAlmacen`); replica el patrón en los servicios que agregues.
- **Dashboard ** ⭐ — nota metodológica importante: el indicador
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
- **Reportes ** ⭐: no se guarda nada nuevo en la base de datos; el
  documento (PDF/Excel) se arma en memoria a partir de los datos que ya
  existen (`movimientos_almacen`, `stock_ubicacion`) y se entrega como
  descarga (`Content-Disposition: attachment`).

##  Configurar la conexión a Supabase

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
   - `V2__add_costo_unitario_material.sql` : agrega la columna
     `costo_unitario` a `materiales`, usada por el dashboard .



##  Ejecutar en local

```bash
export $(cat .env | xargs)   # carga las variables de entorno (Linux/Mac)
mvn spring-boot:run
```

