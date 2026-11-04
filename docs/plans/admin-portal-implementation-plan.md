# Plan de Implementación: Portal de Operaciones y Administración B2B (`ticketing-admin`)

**Fecha:** Noviembre 2026  
**Contexto:** Hibana.tk — Back-Office B2B (`admin.hibana.tk` / `http://localhost:5174`)  
**Estándar:** Vue 3 (Composition API `<script setup>`), TypeScript, Pinia, Vite, Tailwind CSS (Abyssal Luminescence)  
**Destinatario:** Agente ejecutor / Ingeniero Frontend  

---

## 1. Misión y Requerimientos Innegociables

1. **Aislamiento Físico y Lógico (B2B vs B2C):**
   * El panel debe residir en un proyecto independiente hermano: `ticketing-admin/`.
   * Servidor de desarrollo en puerto `:5174` (aislado de `:5173` B2C).
   * No debe permitir el acceso a ningún usuario que no posea los roles `ROLE_ADMIN` o `ROLE_OPERATOR`.

2. **Fidelidad Estética (*Abyssal Luminescence*):**
   * Utilizar la misma paleta oficial de [DESIGN.md](../../wireframe/paleta_de_colores/DESIGN.md):
     * Fondo / Superficies: `surface: #0c141f`, `surface-container-lowest: #070f19`, `surface-container: #18202b`, `surface-container-high: #232a36`.
     * Acentos: `primary: #86cffb`, `secondary: #4fdbc8`, `tertiary: #7bd0ff`, `error: #ffb4ab`.
     * Tipografías: `Space Grotesk` (encabezados/display), `Hanken Grotesk` (cuerpo/lectura), `JetBrains Mono` (labels/métricas/IDs).
   * Layout maestro basado en el wireframe corporativo **Amaryllis Ops Portal** ([wireframe/creacion_evento/paso 1/code.html](../../wireframe/creacion_evento/paso%201/code.html)).

3. **Seguridad y Paridad con Azure Entra ID (Floci-AZ):**
   * El backend ya tiene activo el emulador nativo de Entra ID en `http://localhost:4577/common/v2.0/`.
   * El Gateway (`:8080`) valida tokens RS256 con el filtro `AdminRbacGatewayFilterFactory` en todas las mutaciones administrativas.
   * El frontend debe incluir un **Dev Identity Switcher** en desarrollo para obtener tokens reales de Floci-AZ con un solo clic, y conmutar a `@azure/msal-browser` en producción.

---

## 2. Arquitectura del Proyecto `ticketing-admin`

### 2.1 Estructura de Directorios

```text
ticketing-admin/
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
├── .env.development
├── .env.production
└── src/
    ├── assets/
    │   ├── main.css                        # Variables CSS Abyssal Luminescence
    │   └── fonts/
    ├── types/
    │   ├── auth.ts                         # User, Token, Role types
    │   ├── event.ts                        # Event, Sector, SalesPolicy DTOs
    │   └── venue.ts                        # Venue, Capacity, Location DTOs
    ├── services/
    │   ├── api.ts                          # Axios/Fetch client con interceptor Bearer JWT
    │   ├── authService.ts                  # Floci ROPC / MSAL auth provider
    │   ├── eventAdminService.ts            # Consumo de EventAdminController (:8081 via Gateway :8080)
    │   └── venueService.ts                 # Consumo de Recintos
    ├── stores/
    │   ├── auth.ts                         # useAuthStore (perfil, token, rol activo)
    │   └── eventWizard.ts                  # useEventWizardStore (estado borrador 5 pasos)
    ├── router/
    │   └── index.ts                        # Vue Router con beforeEach RBAC Guard
    ├── layouts/
    │   └── AdminLayout.vue                 # Amaryllis Sidebar + Header con Telemetría
    ├── components/
    │   ├── auth/
    │   │   └── DevAuthSwitcher.vue         # Selector rápido Alex Thorne / Operador / Regular
    │   ├── common/
    │   │   ├── StatusBadge.vue             # DRAFT, PUBLISHED, CANCELLED
    │   │   └── ConfirmModal.vue            # Modal de confirmación (cancelación con motivo)
    │   └── wizard/
    │       ├── Step1GeneralInfo.vue        # Info & Recinto
    │       ├── Step2DatesSchedule.vue      # Fechas & Horarios
    │       ├── Step3PricingSectors.vue     # Mapeo de Sectores & Precios
    │       ├── Step4PoliciesAccess.vue     # Políticas de Compra & QR
    │       ├── Step5ReviewPublish.vue      # Resumen & Proyección Financiera
    │       └── LiveCatalogPreview.vue      # Sticky Live Card Preview (B2C view)
    ├── views/
    │   ├── LoginView.vue                   # Pantalla de acceso / Selección de identidad
    │   ├── UnauthorizedView.vue            # Error 403 Forbidden amigable
    │   ├── EventsListView.vue              # Directorio central de eventos
    │   ├── EventCreateWizardView.vue       # Host del wizard de 5 pasos
    │   └── VenuesListView.vue              # Directorio y capacidades de recintos
    ├── App.vue
    └── main.ts
```

---

## 3. Especificación Técnica de Módulos

### 3.1 Módulo 1: Scaffolding & Setup Visual
1. Inicializar el proyecto con Vite:
   ```bash
   bun create vite ticketing-admin --template vue-ts
   cd ticketing-admin
   bun add pinia vue-router lucide-vue-next
   bun add -D tailwindcss postcss autoprefixer @types/node
   ```
2. Configurar `vite.config.ts`:
   * Puerto local: `5174`.
   * Alias `@` hacia `./src`.
   * Proxy hacia el Gateway `:8080` para `/api`.
3. Configurar `tailwind.config.js` y `main.css`:
   * Importar la configuración de temas y colores idéntica a `ticketing-frontend/tailwind.config.js`.
   * Agregar fuentes de Google Fonts en `index.html`: `Space Grotesk`, `Hanken Grotesk`, `JetBrains Mono` y `Material Symbols Outlined`.

### 3.2 Módulo 2: Autenticación, Dev Switcher & Router Guards
1. **Contrato de Autenticación (`types/auth.ts`):**
   ```typescript
   export type UserRole = 'ROLE_ADMIN' | 'ROLE_OPERATOR' | 'ROLE_USER';

   export interface UserProfile {
     id: string;
     email: string;
     name: string;
     roles: UserRole[];
   }
   ```
2. **`authService.ts` (Integración con Floci-AZ):**
   * Endpoint de token: `http://localhost:4577/00000000-0000-0000-0000-000000000002/oauth2/v2.0/token`.
   * Login ROPC de desarrollo:
     * **Alex Thorne (Admin + Operator):** `username: alex.thorne@hibana.tk`, `client_id: hibana-admin-portal`.
     * **Operador de Guardia:** `username: operator@hibana.tk`, `client_id: hibana-admin-portal`.
     * **Usuario Regular:** `username: user@hibana.tk`, `client_id: hibana-admin-portal`.
3. **`router/index.ts` (Guard de Seguridad):**
   * Rutas protegidas: `/events`, `/events/new`, `/venues`.
   * `beforeEach((to, from, next))`:
     * Si no hay token en `authStore`: redirigir a `/login`.
     * Si el usuario no tiene `ROLE_ADMIN` ni `ROLE_OPERATOR`: redirigir a `/unauthorized`.
     * De lo contrario: permitir navegación `next()`.
4. **Interceptor HTTP (`api.ts`):**
   * Adjunta automáticamente el encabezado `Authorization: Bearer ${token}`.
   * Si la respuesta es HTTP 401: limpia el estado y redirige a `/login`.
   * Si la respuesta es HTTP 403: muestra notificación toast de "Acceso Denegado por Política RBAC".

### 3.3 Módulo 3: Shell Amaryllis Ops Portal (`AdminLayout.vue`)
Basado en [wireframe/creacion_evento/paso 1/code.html](../../wireframe/creacion_evento/paso%201/code.html):
* **Sidebar lateral fija (ancho 64 / 256px, `bg-surface-container-low`):**
  * Logo de marca: Ícono de gema / token con texto `AMARYLLIS` y subtítulo `Ops Portal`.
  * Navegación con íconos de Material Symbols:
    * Eventos (`festival` $\to$ `/events`)
    * Recintos (`apartment` $\to$ `/venues`)
    * Mesa de Soporte (`support_agent` $\to$ badge "Próximamente")
    * Telemetría (`dns` $\to$ badge "Próximamente")
  * Widget inferior de salud: `Platform Health: Operational (99.98%)`.
* **Barra superior (Header sticky, `bg-surface/90 backdrop-blur-xl`):**
  * Indicador de clúster activo: `Cluster: local-floci-az`.
  * Componente `DevAuthSwitcher.vue`: Dropdown interactivo con el avatar de Alex Thorne, mostrando el rol actual y permitiendo conmutar entre *Alex Thorne (Admin)*, *Operador* y *Usuario Regular*.

### 3.4 Módulo 4: Asistente de Creación de Eventos en 5 Pasos
Hosteado en `/events/new`, utiliza un layout de dos columnas: **Columna izquierda (8 cols)** para el formulario de pasos y **Columna derecha (4 cols)** para la tarjeta *Live Sticky Catalog Preview*.

* **Paso 1: Información & Recinto:**
  * Nombre oficial del espectáculo (`title`), slug autogenerado a partir del título.
  * Selector de categoría (`FESTIVALES`, `CONCIERTOS`, `TEATRO`, `ELECTRONICA`).
  * Subgéneros musicales / tags.
  * Editor de descripción oficial con toolbar estético (Negrita, Cursiva, Encabezado, Listas, Citas).
  * Subida/URL de Banner Landscape 16:9 y Póster Vertical 3:4.
  * Selector de Recinto precargado desde el catálogo:
    * Al seleccionar, muestra la tarjeta de geolocalización y bloquea el campo de aforo legal máximo autorizado por bomberos (ej. *11,000 espectadores*).
* **Paso 2: Fechas & Horarios:**
  * Fecha y hora de apertura de puertas (`doorsOpenAt`).
  * Fecha y hora de inicio (`startsAt`) y fin estimada (`endsAt`).
  * Selector de zona horaria (`America/Argentina/Buenos_Aires`).
  * Activación de Sala de Espera Virtual y umbral de cola.
* **Paso 3: Mapeo de Sectores & Precios:**
  * Tabla editable para añadir sectores:
    * Nombre (ej. *Campo Delantero VIP*, *Platea Preferencial*, *General*).
    * Tipo (`SEATED` / `GENERAL_ADMISSION`).
    * Capacidad / Aforo del sector.
    * Precio base y tarifa de servicio (`serviceFee`).
    * Moneda (`ARS`, `USD`).
  * Barra de progreso de aforo: Suma automática de capacidades de sectores vs aforo total habilitado del recinto (alerta en rojo si se sobrepasa la capacidad autorizada).
* **Paso 4: Políticas & Acceso:**
  * Límite de entradas por comprador (máx. 4 o 6 entradas por DNI).
  * Restricción de edad mínima (ej. *Apto para mayores de 18 años*).
  * Activación de código QR dinámico rotativo anti-fraude.
* **Paso 5: Resumen & Proyección Financiera:**
  * Checklist de auditoría pre-lanzamiento (todos los datos validados).
  * Tarjeta de proyección de taquilla bruta estimada: `Total = Σ (Capacidad x Precio Base)`.
  * Botón primario: **"Publicar Evento a Venta General"** $\to$ Ejecuta `POST /api/v1/events` y随后 `POST /api/v1/events/{id}/publish`.
  * Botón secundario: **"Guardar como Borrador"** $\to$ Ejecuta `POST /api/v1/events` en estado `DRAFT`.
* **Live Catalog Preview (Columna Derecha):**
  * Renderiza en tiempo real la tarjeta del espectáculo con el banner, los badges de categoría, fechas y rango de precios, idéntica a la vista de `HomeView.vue` del cliente B2C.

### 3.5 Módulo 5: Directorio Central de Eventos (`EventsListView.vue`)
* Tabla con diseño de alta densidad para operadores:
  * Columnas: Evento & Banner, Recinto & Ciudad, Fechas, Aforo Ocupado / Total, Estado (`DRAFT`, `PUBLISHED`, `CANCELLED`), Acciones.
  * Barra de búsqueda por título o recinto y pestañas de filtro por estado.
  * Acciones por fila:
    * **Ver en Catálogo:** Abre el evento en `http://localhost:5173/events/{id}` en una pestaña nueva.
    * **Publicar:** Habilitado solo si el estado es `DRAFT` (invoca `POST /api/v1/events/{id}/publish`).
    * **Cancelar Evento:** Abre modal de confirmación solicitando el motivo reglamentario (invoca `DELETE /api/v1/events/{id}?reason=...`). Requiere `ROLE_ADMIN`.

---

## 4. Contratos de Integración Backend (REST APIs)

Todas las llamadas se enrutan a través del API Gateway en `http://localhost:8080`:

| Método | Endpoint Gateway | Microservicio Destino | Header de Seguridad | Descripción |
|---|---|---|---|---|
| `GET` | `/api/v1/events` | `catalog-service` (:8081) | Opcional | Listado público / administrativo de eventos |
| `GET` | `/api/v1/events/{id}` | `catalog-service` (:8081) | Opcional | Detalle de un evento específico |
| `POST` | `/api/v1/events` | `catalog-service` (:8081) | `Authorization: Bearer <token>` | Crea un evento (requiere `ROLE_ADMIN`) |
| `POST` | `/api/v1/events/{id}/publish` | `catalog-service` (:8081) | `Authorization: Bearer <token>` | Publica el evento (requiere `ROLE_ADMIN`) |
| `DELETE` | `/api/v1/events/{id}` | `catalog-service` (:8081) | `Authorization: Bearer <token>` | Cancela el evento (requiere `ROLE_ADMIN`) |
| `GET` | `/api/v1/venues` | `catalog-service` (:8081) | Opcional | Listado de recintos habilitados |
| `POST` | `http://localhost:4577/.../token` | `floci-az` (:4577) | Ninguno | Emisión de tokens ROPC en desarrollo local |

### 4.1 Ejemplo de Payload para Creación de Eventos (`POST /api/v1/events`)
```json
{
  "organizerId": "org-hibana-live",
  "venueId": "00000000-0000-0000-0000-000000000001",
  "slug": "cosquin-rock-2025",
  "title": "Cosquín Rock 2025 - Edición 25 Años",
  "category": "FESTIVALES",
  "genres": ["Rock", "Indie", "Fusión"],
  "doorsOpenAt": "2026-03-20T17:00:00Z",
  "startsAt": "2026-03-20T20:00:00Z",
  "endsAt": "2026-03-21T02:00:00Z",
  "timezone": "America/Argentina/Buenos_Aires",
  "media": "{\"heroBannerUrl\":\"https://images.unsplash.com/photo-1470225620780-dba8ba36b745\",\"posterUrl\":\"https://images.unsplash.com/photo-1514525253161-7a46d19cd819\"}",
  "salesPolicy": "{\"maxTicketsPerUser\":4,\"transferable\":true}",
  "sectors": [
    {
      "name": "Campo General",
      "type": "GENERAL_ADMISSION",
      "totalCapacity": 8000,
      "basePrice": 45000.0,
      "serviceFee": 4500.0,
      "currency": "ARS"
    },
    {
      "name": "Platea Preferencial",
      "type": "SEATED",
      "totalCapacity": 3000,
      "basePrice": 85000.0,
      "serviceFee": 8500.0,
      "currency": "ARS"
    }
  ]
}
```

---

## 5. Criterios de Aceptación y Verificación

1. **Gatekeeping de Seguridad:**
   * Al ingresar a `http://localhost:5174/events` sin sesión, el sistema redirige automáticamente a `/login`.
   * Al iniciar sesión como `Usuario Regular` (`user@hibana.tk`), cualquier intento de entrar a `/events` redirige a `/unauthorized`.
   * Al conmutar a `Alex Thorne` (`ROLE_ADMIN`), el acceso a todas las rutas y botones de publicación es inmediato.
2. **Ciclo de Creación End-to-End:**
   * Completar los 5 pasos del Asistente y pulsar "Publicar Evento".
   * El evento debe persistirse en PostgreSQL (`events`, `event_sectors`).
   * El evento debe reflejarse en la lista de eventos del Admin Panel (`EventsListView.vue`).
   * El evento debe aparecer inmediatamente disponible para la compra en el cliente B2C (`http://localhost:5173`).
3. **Consistencia Visual:**
   * Tema oscuro `Abyssal Luminescence` sin fuentes o colores discordantes.
   * Totalmente responsivo en monitores de escritorio (1440px y 1080px).

---

## 6. Prompt Listo para Delegar al Siguiente Agente

```text
Lee el plan detallado en @docs/plans/admin-portal-implementation-plan.md y la guía de autenticación en @docs/architecture/azure-authentication-floci-emulation.md.
Crea el proyecto frontend `ticketing-admin` utilizando Vue 3, Vite, TypeScript, Pinia y Tailwind CSS, implementando:
1. El layout Amaryllis Ops Portal con el tema Abyssal Luminescence de DESIGN.md.
2. El sistema de autenticación conectado al emulador Microsoft Entra ID de Floci-AZ (:4577) con el Dev Switcher (Alex Thorne / Operador / Regular) y los Router Guards RBAC.
3. El Asistente de Creación de Eventos en 5 Pasos con Live Catalog Preview conectando a EventAdminController a través del Gateway (:8080).
4. El listado de eventos con acciones para Publicar y Cancelar.
```
