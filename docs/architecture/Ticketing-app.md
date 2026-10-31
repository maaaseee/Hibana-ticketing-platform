# Especificación Técnica, Arquitectónica, Operativa y de Inteligencia Artificial de Software

## Plataforma de Gestión y Venta de Entradas
*Documento Extendido de Diseño de Sistemas, Decisiones Arquitectónicas y Operaciones B2B*

---

## 1. Visión General del Sistema y Objetivos de Negocio

El presente documento expande de manera exhaustiva las directrices de diseño para la plataforma de venta de entradas. El sistema está concebido como una solución nativa de la nube (*Cloud-Native*) en Microsoft Azure capaz de soportar alta concurrencia, garantizando la consistencia transaccional y ofreciendo una experiencia de usuario fluida bajo la identidad institucional de Hibana Software. 

La arquitectura elegida aborda los desafíos de escalabilidad asimétrica (picos masivos de lectura vs. escrituras críticas de reserva y pago) y aísla de manera estricta las operaciones de administración (*Back-Office*) del tráfico de usuarios públicos (*Front-Office*).

---

## 2. Stack Tecnológico Integral y Arquitectura de Servicios Cloud

El ecosistema tecnológico ha sido diseñado para maximizar el rendimiento, la resiliencia y la escalabilidad, apoyándose en componentes nativos de Microsoft Azure y estándares modernos de desarrollo de software:

* **Backend:** Java 21 y Spring Boot 3, utilizando Spring Cloud Gateway como puerta de enlace, Resilience4j para patrones de resiliencia y tolerancia a fallos, y Spring Data JPA para la capa de acceso a datos.
* **Frontend:** Vue 3 (Composition API, Pinia, Vite), estructurado en dos aplicaciones web totalmente independientes (Front-Office B2C y Back-Office B2B). En el Back-Office se emplea HTML5 Canvas para el renderizado masivo y de alto rendimiento del mapa interactivo de butacas.
* **Base de Datos y Persistencia:** 
  * Azure Database for PostgreSQL (Flexible Server, reforzado con la extensión `pgvector` para el almacenamiento y búsqueda vectorial de embeddings).
  * Azure Cosmos DB for NoSQL (orientado al catálogo flexible y de baja latencia de recintos y eventos).
* **Mensajería y Event-Driven:** Azure Service Bus mediante Topics y Subscriptions, facilitando la coreografía de microservicios y la gestión de transacciones distribuidas bajo el patrón Saga.
* **Caché y Concurrencia:** Azure Cache for Redis, utilizado para el control de concurrencia mediante bloqueos distribuidos (algoritmo Redlock), la orquestación de la sala de espera virtual (*Virtual Waiting Room*) mediante *Sorted Sets*, y canales Pub/Sub para la distribución de sesiones WebSockets.
* **Seguridad e IAM:** Microsoft Entra ID con *Managed Identities* para la autenticación e identidad corporativa sin contraseñas inter-servicio, e integración con Azure Key Vault para la gestión centralizada de secretos y certificados.
* **Observabilidad y Monitoreo:** Suite nativa de Azure con Azure Monitor, Application Insights (para APM y rastreo distribuido con correlación de operaciones) y Log Analytics (para agregación e indexación de logs).
* **Inteligencia Artificial y MLOps:** Azure OpenAI (`text-embedding-3-small` / GPT-4o) para la generación de embeddings y arquitectura RAG, complementado con MLflow para el seguimiento del ciclo de vida y experimentos de los modelos.
* **Entorno Local y Costo Cero:** Uso de **Floci-AZ** para la emulación local completa de los servicios de Azure durante la fase de desarrollo, complementado con Ollama/LocalAI para embeddings locales, eliminando costos operativos pre-despliegue.

---

## 3. Diseño Guiado por el Dominio (Domain-Driven Design)

Para evitar el acoplamiento y la creación de un monolito distribuido, el sistema se divide utilizando *Bounded Contexts* estrictos. Cada microservicio es dueño absoluto de sus datos y de su lógica de negocio.

### 3.1 Mapeo de Contextos (Bounded Contexts)

| Contexto | Frontera de Responsabilidad | Eventos de Dominio Publicados |
| :--- | :--- | :--- |
| **Identity Access Management (IAM)** | Autenticación e identidad corporativa con Microsoft Entra ID, gestión de roles (RBAC), asignación de Managed Identities y emisión de tokens OAuth2/OIDC. | `UserRegistered`, `UserBanned`, `RoleUpdated` |
| **Event & Venue Catalog** | Gestión de recintos, creación de espectáculos, fechas, sectores y capacidades. | `EventCreated`, `EventCancelled`, `VenueCapacityChanged` |
| **Ticketing & Inventory** | Gestión del ciclo de vida del asiento, bloqueos de concurrencia temporal y asignación final. | `SeatLocked`, `TicketIssued`, `TicketRefunded` |
| **Order & Financial Engine** | Gestión de carritos, orquestación de pagos, billeteras virtuales y contabilidad. | `OrderCreated`, `PaymentApproved`, `PaymentFailed` |
| **Customer Support & Ops** | Gestión de incidencias, chat en vivo cliente-operador asistido por RAG, y seguimiento de reembolsos. | `ChatSessionStarted`, `TicketDisputed` |
| **Recommendation Engine (IA)** | Cálculo de afinidad mediante similitud vectorial de cosenos y generación de sugerencias personalizadas. | `UserRecommendationsReady` |

---

## 4. Ecosistema de Clientes Web (Front-Office vs Back-Office)

Una decisión arquitectónica fundamental es la separación física y lógica de las interfaces de usuario. La plataforma no despliega una única aplicación web monolítica, sino dos clientes en Vue 3 completamente aislados.

### 4.1 Front-Office: Aplicación B2C (Public Client)
* **Propósito:** Portal de consumo masivo para el usuario final. Navegación de catálogo, interacción con la sala de espera virtual, selección interactiva de asientos y checkout.
* **Dominio:** `www.hibana.tk`
* **Características:** Optimizada para rendimiento y SEO (Vite/SSR opcional), carga diferida (*lazy loading*) de rutas, consumo de CDN para assets estáticos y alta tolerancia a degradaciones temporales. En picos de 100.000 usuarios concurrentes, este cliente absorbe el impacto directo.

### 4.2 Back-Office: Portal de Administración B2B (Admin Client)
* **Propósito:** Centro de control para productores, administradores y operadores de soporte técnico.
* **Dominio:** `admin.hibana.tk`
* **Justificación de Separación:** Si el Front-Office experimenta una caída, saturación o un ataque de denegación de servicio (DDoS) debido a la venta de un evento de alta demanda, el equipo de operaciones y soporte no debe verse afectado. Al separar los clientes, el Back-Office se conecta a un Gateway o red interna diferente (VNet privada), garantizando que el personal operativo siempre mantenga acceso para pausar ventas, auditar servidores o resolver disputas por chat en vivo.

---

## 5. Sistema de Roles y Control de Acceso (RBAC)

La gestión de identidad se delega integralmente a Microsoft Entra ID, aprovechando *Managed Identities* para la autenticación sin contraseña entre servicios dentro de Azure. Los *claims* del token OIDC/JWT definen las capacidades del usuario a través de todo el clúster:

* `ROLE_USER`: Permisos estándar de lectura de catálogo público, ingreso a salas de espera y emisión/pago de órdenes de compra.
* `ROLE_ADMIN` (Productor): Permisos de escritura masiva. Capacidad de crear recintos, configurar eventos, definir mapas de precios, pausar ventas y cancelar espectáculos.
* `ROLE_OPERATOR` (Soporte): Permisos granulares. Puede consultar (`GET`) el estado de cualquier orden para resolver disputas, autorizar reembolsos controlados y acceder a los túneles WebSocket de chat de soporte. No tiene permisos para crear eventos ni alterar precios de inventario.

---

## 6. Arquitectura de Operaciones Complejas (Flujos del Back-Office)

### 6.1 Flujo Asíncrono de Creación de Eventos
La creación de un evento de estadio o teatro masivo requiere inicializar miles de asientos físicos para el motor transaccional. El flujo opera de forma asíncrona:

```
[Admin UI] 
    │  (1) POST /api/events (HTTP 202 Accepted)
    ▼
[Event & Venue Service] 
    │  (2) Persiste Metadata en Cosmos DB
    │  (3) Publica 'EventCreated'
    ▼
[Azure Service Bus: Topic 'event-lifecycle']
    │
    ▼ (Subscription: 'ticketing-seat-generator')
[Ticketing Service] 
    │  (4) Batch BULK INSERT (15.000+ asientos con estado 'AVAILABLE')
    ▼
[Azure Database for PostgreSQL]
    │
    ▼ (5) Notificación WebSocket
[Admin UI: Evento 'Listo para la Venta']
```

1. Un **ADMIN** diseña el evento en el panel de Back-Office y envía la petición al API Gateway.
2. El **Event Service** recibe la solicitud, valida la estructura geométrica del recinto y guarda los metadatos en Azure Cosmos DB for NoSQL. Responde un código HTTP 202 (*Accepted*) al administrador, indicando que el proceso por lotes ha comenzado.
3. El **Event Service** publica el evento `EventCreated` (con la definición de sectores, filas y capacidades) en un Topic de Azure Service Bus.
4. El **Ticketing Service** consume dicho mensaje y dispara un trabajo por lotes (*Batch Processing*) que ejecuta operaciones `BULK INSERT` en Azure Database for PostgreSQL, generando los registros individuales con estado `AVAILABLE`.
5. Finalizado el proceso, se publica una notificación vía WebSocket hacia el panel de Back-Office informando que el inventario está completamente inicializado y "Listo para la Venta".

### 6.2 Servicio de Soporte al Cliente (Customer Support Chat)
Para la atención de incidencias en tiempo real, se implementa una arquitectura distribuida tolerante a desconexiones:

* **Comunicaciones Bidireccionales:** WebSockets mediante Spring WebFlux para mantener conexiones dúplex eficientes con baja sobrecarga de memoria.
* **Patrón de Enrutamiento (Azure Cache for Redis Pub/Sub):** Al escalar horizontalmente en pods de Azure Kubernetes Service (AKS), el Usuario A puede estar conectado a la instancia `Pod-1`, mientras que el Operador de soporte asignado se encuentra en la instancia `Pod-3`. Cada mensaje enviado se publica en un canal de Redis (`chat.room.{ticketId}`). Todos los pods suscritos reciben la carga útil y el pod que contiene la sesión WebSocket activa del destinatario despacha el paquete en milisegundos.

---

## 7. Arquitectura de Infraestructura y Redes

### 7.1 Patrón API Gateway (Spring Cloud Gateway)
Toda petición externa atraviesa inicialmente la puerta de enlace, cuyas responsabilidades se limitan a aspectos transversales:

* **Terminación SSL/TLS:** Descifra el tráfico HTTPS perimetralmente, liberando ciclos de CPU en los microservicios internos.
* **Rate Limiting:** Implementado con el algoritmo *Token Bucket* respaldado por Azure Cache for Redis, limitando el tráfico a 100 peticiones por minuto por IP para clientes públicos para neutralizar ataques DDoS y scraping malicioso.
* **Filtros de Seguridad Entra ID / JWT:** Valida la firma criptográfica, la vigencia y los roles del token de Microsoft Entra ID antes de enrutar el tráfico al backend.

### 7.2 Comunicación Interna (Síncrona vs Asíncrona)
* **Consultas (Queries) - Síncronas:** Lecturas inter-servicio de baja latencia mediante clientes declarativos Spring Cloud OpenFeign respaldados por service discovery interno en Kubernetes (CoreDNS).
* **Comandos (Commands) - Asíncronos:** Mutaciones de estado inter-servicio utilizando Azure Service Bus (Topics y Subscriptions) para orquestar la mensajería y garantizar resiliencia mediante el patrón Saga coreografiado.

---

## 8. Diseño Profundo de Microservicios Críticos

### 8.1 Ticketing Service y Control de Concurrencia
Representa el núcleo de alto rendimiento del sistema, protegiendo los recursos frente a condiciones de carrera extremas:

* **Distributed Lock Engine (Azure Cache for Redis):** Implementación del algoritmo Redlock mediante Redis administrado. La reserva temporal atómica se gestiona con la clave `lock:event:{eventId}:seat:{seatId}` aplicando un TTL estricto de 10 minutos.
* **Mecanismos de Desbloqueo:**
  1. *Expiración pasiva:* Si el usuario abandona la compra, el TTL de Redis expira y el asiento vuelve a estar disponible sin intervención manual.
  2. *Compensación activa:* Si el pago falla o es cancelado en la Saga, el servicio emite la liberación inmediata eliminando la clave de Redis.

### 8.2 Financial & Order Engine (Orquestación Financiera)
El microservicio financiero implementa un registro de doble entrada inmutable para cada transacción:

* Basado en la arquitectura contable `com.aieta.pocket`, registrando entradas (*incomes*), salidas (*expenses*) y balances de billeteras virtuales.
* Todas las operaciones financieras se persisten en Azure Database for PostgreSQL bajo aislamiento transaccional estricto (ACID), utilizando *Managed Identities* para evitar almacenar cadenas de conexión en texto plano.

---

## 9. Consistencia Eventual y Tolerancia a Fallos

### 9.1 Transactional Outbox Pattern
Para solucionar el problema de doble escritura (*Dual Write Problem*) entre la base de datos local y el broker de mensajería:

* Cada microservicio escribe la mutación de la entidad de negocio y el evento a emitir en una tabla `outbox_events` dentro de la **misma transacción local ACID**.
* Un conector CDC (Change Data Capture) o un worker desacoplado lee las inserciones en `outbox_events` y las despacha de forma segura hacia Azure Service Bus con garantía de entrega *at-least-once*.

### 9.2 Circuit Breaker (Resilience4j)
Las llamadas síncronas entre microservicios se encuentran envueltas en disyuntores de Resilience4j. Si un servicio dependiente experimenta alta latencia o fallos repetidos, el circuito se abre de inmediato, retornando una respuesta de fallback y evitando el agotamiento de los hilos de ejecución del contenedor.

---

## 10. Observabilidad y Monitoreo Continuo

* **Gestión de Cargas y Secretos en AKS:** Los microservicios corren en Azure Kubernetes Service (AKS). Secretos, certificados y claves de API se almacenan en Azure Key Vault y se sincronizan dentro del clúster mediante el *Azure Key Vault Provider for Secrets Store CSI Driver*.
* **Observabilidad y APM Integral:** Telemetría centralizada con Azure Monitor y Application Insights. Las peticiones reciben un `Trace-ID` en el Gateway que se propaga a lo largo de las llamadas HTTP y eventos de Service Bus.
* **Centralización de Logs y Métricas:** Recolección unificada en Log Analytics mediante consultas KQL (*Kusto Query Language*) y dashboards integrados en Azure Monitor.

---

## 11. Gestión de Tráfico: Sala de Espera Virtual (Virtual Waiting Room)

Para gestionar eventos con sobredemanda sin degradar la infraestructura transaccional, se implementa una sala de espera virtual que coopera con el Rate Limiter del API Gateway.

### 11.1 Comparativa: Rate Limiter vs. Cola Virtual

| Característica | Rate Limiter (Gateway) | Cola Virtual (Virtual Waiting Room) |
| :--- | :--- | :--- |
| **Objetivo Principal** | Proteger la infraestructura de red bloqueando tráfico excesivo o malicioso. | Ordenar a los compradores legítimos garantizando justicia estricta (FIFO). |
| **Experiencia de Usuario (UX)** | Retorna error HTTP 429 (*Too Many Requests*). | Interfaz gráfica interactiva mostrando posición y tiempo estimado de espera. |
| **Estructura Técnica** | Algoritmo Token Bucket en Redis. | *Sorted Sets* (`ZSET`) en Azure Cache for Redis. |

### 11.2 Estructura en Azure Cache for Redis
* **Ingreso a la Fila:** Cuando el usuario solicita unirse, el backend ejecuta:
  ```redis
  ZADD queue:event:{eventId} {timestamp_ms} {userId}
  ```
  El *timestamp* en milisegundos actúa como *score*, preservando el orden de llegada.
* **Consulta en Tiempo Real:** El cliente Vue 3 escucha vía WebSocket la actualización de su posición consultada mediante:
  ```redis
  ZRANK queue:event:{eventId} {userId}
  ```
* **Emisión del Pase (Checkout Token):** Un proceso programado (`@Scheduled`) extrae lotes autorizados mediante `ZPOPMIN` y emite un JWT firmado temporal (válido por 15 minutos).

### 11.3 Filtro Personalizado en Spring Cloud Gateway
El Gateway intercepta los endpoints críticos de reserva mediante una fábrica de filtros personalizada:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ticketing-service-checkout
          uri: lb://TICKETING-SERVICE
          predicates:
            - Path=/api/tickets/reserve/**
          filters:
            - name: CheckoutTokenFilterFactory
```

Si la petición no incluye el encabezado `X-Checkout-Token` o si el token expiró, el Gateway bloquea el acceso con HTTP 403 (*Forbidden*), redirigiendo al cliente a la sala de espera.

---

## 12. Arquitectura MLOps y Motor de Recomendaciones con IA

### 12.1 Generación y Almacenamiento de Embeddings
El sistema genera representaciones vectoriales densas utilizando modelos de Azure OpenAI:

* **Item Embeddings:** Vectores generados a partir de los atributos semánticos del espectáculo (género, descripción, artistas similares, ubicación).
* **User Embeddings:** Vector continuo que combina señales híbridas del usuario:
  * Señales explícitas (géneros seleccionados en el perfil).
  * Señales implícitas fuertes (historial de entradas adquiridas).
  * Señales implícitas suaves (visitas reiteradas a páginas de eventos).
* **Persistencia Vectorial:** Almacenamiento en Azure Database for PostgreSQL utilizando la extensión `pgvector` con índices HNSW (*Hierarchical Navigable Small World*) para búsquedas ultrarrápidas.

### 12.2 Flujo de Notificaciones y Similitud del Coseno
El motor ejecuta periódicamente consultas de similitud del coseno en SQL:

$$ \text{Cosine Similarity} = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\| \|\mathbf{v}\|} = 1 - (\mathbf{u} \Leftrightarrow \mathbf{v}) $$

```sql
SELECT event_id, 1 - (embedding <=> :userVector) AS similarity
FROM event_embeddings
WHERE event_date > NOW()
ORDER BY embedding <=> :userVector ASC
LIMIT 3;
```

Una vez seleccionados los 3 espectáculos con mayor afinidad, el motor publica un mensaje en Azure Service Bus hacia el servicio de notificaciones para despachar correos personalizados.

### 12.3 Ciclo de Vida MLOps con MLflow
El ciclo de vida del modelo predictivo y los pipelines de embeddings se gestionan con MLflow:
* Registro y versionado de datasets de entrenamiento y configuraciones de prompts.
* Trazabilidad de hiperparámetros y métricas de recomendación (*precision@k*, *recall@k*, CTR).
* Detección de desviación de datos (*data drift*) para reentrenamiento automatizado.

---

## 13. Estrategia de Pruebas de Estrés y Carga Masiva (Stress Testing)

### 13.1 Estrategia Shift-Left Testing
* **Pruebas Locales (Costo Cero):** Ejecución de scripts en **Grafana k6** contra la infraestructura emulada localmente con **Floci-AZ**, validando algoritmos de concurrencia y bloqueos atómicos sin incurrir en costos cloud.
* **Pruebas Cloud Scaling:** Pruebas masivas con **Azure Load Testing** apuntando al clúster AKS en entornos staging/pre-prod, correlacionando métricas del cliente con telemetría de CPU, memoria y saturación de base de datos en Azure Monitor.

### 13.2 Los 3 Escenarios Críticos de Carga
1. **Avalancha a la Sala de Espera (Surge Traffic):** Simulación de 100.000 usuarios concurrentes ingresando en un intervalo de 60 segundos para validar la absorción de escrituras en los *Sorted Sets* de Redis.
2. **Condición de Carrera (Race Condition):** 500 bots concurrentes intentando reservar la **misma butaca en el mismo milisegundo** (`seatId: 104`). Criterio de éxito: exactamente 1 usuario recibe HTTP 200 y 499 usuarios reciben HTTP 409/423 (cero sobreventa).
3. **Saturación y Detección Anti-Bot:** Inyección masiva de ráfagas HTTP no autorizadas para comprobar que el filtro del Gateway responde con HTTP 429 sin degradar los pods de backend en AKS.

### 13.3 Enfoque Híbrido de Pruebas (95/5)
* **95% Nivel Protocolo (Grafana k6):** Carga masiva sobre peticiones HTTP y conexiones persistentes WebSocket simulando la cola virtual y la reserva.
* **5% Navegador Real End-to-End (Playwright):** Automatización con navegadores headless reales para interactuar directamente sobre el `<canvas>` del mapa de asientos, verificando tiempos de renderizado en el DOM y fluidez visual bajo estrés de red.

### 13.4 Métricas Críticas y Umbrales de Aceptación

| Métrica | Umbral de Aceptación |
| :--- | :--- |
| **Latencia P95** | $< 300\text{ ms}$ en reservas y consultas críticas |
| **Tasa de Errores Inesperados (5xx)** | $< 1\%$ bajo carga máxima sostenida |
| **Handshake y Estabilidad WebSocket** | Latencia de conexión inicial $< 500\text{ ms}$ |
| **Integridad de Inventario** | $0\%$ sobreventas (*double booking*) en pruebas de colisión |