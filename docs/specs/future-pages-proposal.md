# Propuesta de Nuevas Pantallas y Vistas del Sistema (Roadmap Arquitectónico)

**Fecha:** Octubre 2026  
**Contexto:** Basado en la especificación técnica general (`docs/Ticketing-app.md`), los estándares de microservicios con DDD (`GEMINI.md`) y la experiencia del cliente Front-Office B2C recientemente construida.

---

## 1. Resumen del Alcance Implementado (Front-Office B2C)

Se ha completado la arquitectura frontend para clientes finales en `ticketing-frontend`:
- **Catálogo & Descubrimiento con IA (`HomeView.vue`)**: Recomendaciones basadas en similitud de cosenos (`pgvector` HNSW) y búsqueda facetada.
- **Detalle de Espectáculo (`EventDetailView.vue`)**: Información de recintos, políticas y enrutamiento inteligente según demanda.
- **Sala de Espera Virtual (`QueueView.vue`)**: Emulación de colas FIFO basadas en Redis Sorted Sets (`queue:event:{id}`) con emisión del token `X-Checkout-Token`.
- **Selección de Butacas con Canvas (`SeatSelectionView.vue` & `SeatCanvas.vue`)**: Mapa de asientos interactivo en HTML5 Canvas con bloqueo atómico distribuido **Redlock** (`lock:event:{id}:seat:{id}`, TTL 10 min).
- **Checkout & Liquidación (`CheckoutView.vue`)**: Integración con Billetera Digital Pocket (`com.aieta.pocket`) mediante partida doble y pasarela PCI.
- **Mis Pases & Acceso (`DashboardTicketsView.vue` & `DynamicQrCode.vue`)**: Pases estilo ticket con corte perforado, código QR dinámico rotativo con TOTP (15s) y transferencia de titularidad criptográfica.
- **Centro de Ayuda & FAQ (`HelpCenterView.vue`)**: Base de conocimiento y acordeón accesible.
- **Asistente de Soporte en Vivo (`SupportChatView.vue`)**: Soporte conversacional RAG vía WebSockets/WebFlux y escalamiento a operadores humanos mediante Redis Pub/Sub.

---

## 2. Pantallas Pendientes Requeridas: Back-Office B2B & Operaciones

Para completar la solución de escala empresarial descrita en la arquitectura, se proponen las siguientes vistas y módulos de software adicionales:

### A. Back-Office B2B: Portal de Operaciones y Administración (`admin.hibana.tk`)
*Debe estar lógica y físicamente aislado del tráfico B2C de clientes.*

#### 1. Constructor de Recintos y Distribución de Butacas (Venue & Seating Layout Builder)
- **Propósito**: Permitir a promotores y administradores diseñar recintos desde cero o importar planos CAD/SVG.
- **Funcionalidad clave**:
  - Lienzo interactivo en **HTML5 Canvas** (no DOM/SVG masivo) para posicionar miles de butacas, definir filas curvas, palcos VIP y zonas de campo general.
  - Asignación de sectores tarifarios, pasillos de evacuación y orientación hacia el escenario.
  - Exportación de topología a Azure Cosmos DB NoSQL en formato JSON estructurado.

#### 2. Asistente de Creación y Ciclo de Vida del Evento (Event Creation Wizard)
- **Propósito**: Configurar la publicación de espectáculos y las reglas de venta.
- **Funcionalidad clave**:
  - Parámetros de preventa y activación de la Sala de Espera Virtual (umbral de concurrencia).
  - Reglas de negocio: límite de compras por DNI/Entra ID, cuotas de preventa bancaria, y tarifas por fases cronológicas (Early Bird, Preventa 1, Venta General).
  - Integración con Transactional Outbox para disparar el evento de dominio `EventCreated` a Azure Service Bus.

#### 3. Monitor de Tráfico en Tiempo Real & Control de Flujo (Waiting Room Telemetry Dashboard)
- **Propósito**: Panel de telemetría para incidentes y control de ventas masivas.
- **Funcionalidad clave**:
  - Métricas en vivo de la cola Redis: tasa de ingreso, tasa de drenaje (checkout rate), tiempo medio de espera y tasa de abandono.
  - "Kill Switch" y acelerador manual de admisiones para regular la presión hacia el microservicio de Ticketing.
  - Detección de patrones anómalos o sospecha de bots para bloqueo masivo de IPs a nivel Gateway.

---

### B. Módulo de Operaciones de Acceso en Campo (Field Operations & Turnstiles)

#### 4. Consola de Validación de Molinetes / App PWA para Control de Accesos (Turnstile Gate Scanner)
- **Propósito**: Terminal de alta velocidad para el personal de control en las puertas de ingreso al estadio/teatro.
- **Funcionalidad clave**:
  - Lectura de cámara a 60 FPS con decodificación del código QR dinámico.
  - Validación local offline/online de la firma **Ed25519** y verificación de la ventana temporal TOTP de 15 segundos para impedir capturas de pantalla duplicadas.
  - Feedback sonoro y lumínico instantáneo (Verde: Ingreso Permitido / Rojo: Pase ya utilizado o código caducado).
  - Estadísticas de aforo en tiempo real por puerta y sector de ingreso.

---

### C. Módulo Financiero, Auditoría y Contabilidad Inmutable

#### 5. Libro Mayor Contable & Conciliación (`Pocket Ledger Explorer`)
- **Propósito**: Auditoría fiscal, seguimiento de fondos en escrow y liquidaciones a organizadores.
- **Funcionalidad clave**:
  - Visualización del libro diario de **partida doble** (`com.aieta.pocket`) en PostgreSQL Flexible Server.
  - Trazabilidad de transacciones entre cuentas de clientes, cuentas escrow retenidas y comisiones cobradas por servicio.
  - Liquidación automática y payouts programados a promotores una vez transcurrido el evento.

#### 6. Centro de Disputas & Compensaciones Saga (Dispute & Chargeback Operations)
- **Propósito**: Gestión de contracargos bancarios y reembolsos por cancelaciones imprevistas.
- **Funcionalidad clave**:
  - Disparo manual o automatizado de Sagas coreografiadas de compensación (`CancelReservationSaga`).
  - Reversión atómica de inventario (asientos vuelven a estado `AVAILABLE`) y reembolso crediticio en la billetera virtual.
  - Registro de auditoría con `Trace-ID` unificado a través de Application Insights.

---

### D. Módulo de Inteligencia de Negocios y Machine Learning

#### 7. Tablero de Experimentación y Modelos de Recomendación (AI Recommendation & MLflow Desk)
- **Propósito**: Supervisión de los modelos de embeddings y conversión de ventas cruzadas.
- **Funcionalidad clave**:
  - Monitoreo del espacio vectorial `pgvector` con métricas de similitud de cosenos (`1 - (embedding <=> :userVector)`).
  - Pruebas A/B sobre las recomendaciones generadas en la página principal para medir el CTR (Click-Through Rate).
  - Métricas de pérdida y tracking de experimentos con **MLflow**.

---

## 3. Matriz de Roles y Control de Acceso (RBAC) para Nuevas Vistas

| Módulo / Pantalla Proyectada | `ROLE_USER` | `ROLE_OPERATOR` | `ROLE_ADMIN` | Protocolo de Comunicación |
|---|:---:|:---:|:---:|---|
| **Front-Office B2C** (Actual) | Acceso Total | Acceso Total | Acceso Total | HTTPS / WebSocket / Redis |
| **Constructor de Recintos** | Denegado | Solo Lectura | Control Total | REST API + Cosmos DB NoSQL |
| **Asistente de Eventos** | Denegado | Solo Lectura | Control Total | REST + Transactional Outbox |
| **Telemetría de Sala de Espera** | Denegado | Solo Lectura | Control Total | WebSocket + Redis Monitor |
| **Control de Accesos (Molinete)** | Denegado | Control Total | Control Total | PWA Offline + Ed25519 Verify |
| **Libro Mayor Pocket** | Denegado | Denegado | Auditoría / Admin | PostgreSQL ACID Read-Only |
| **Consola de Disputas** | Denegado | Control Total | Control Total | Azure Service Bus (Saga) |
| **MLflow & Recomendaciones** | Denegado | Denegado | Data Engineer | MLflow API / pgvector Admin |
