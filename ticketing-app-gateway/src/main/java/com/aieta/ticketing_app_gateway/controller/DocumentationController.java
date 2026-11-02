package com.aieta.ticketing_app_gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Native, non-blocking documentation controller running directly on the Spring Cloud Gateway.
 * Serves aggregated OpenAPI 3.1 REST contracts, AsyncAPI 3.0 Event Mesh specifications,
 * and an interactive developer portal UI on {@code http://localhost:8080/docs}.
 */
@RestController
public class DocumentationController {

    private final Resource openApiSpec;
    private final Resource asyncApiSpec;

    public DocumentationController(
            @Value("classpath:specs/openapi.yaml") Resource openApiSpec,
            @Value("classpath:specs/asyncapi.yaml") Resource asyncApiSpec) {
        this.openApiSpec = openApiSpec;
        this.asyncApiSpec = asyncApiSpec;
    }

    @GetMapping(value = {"/docs", "/docs/", "/docs/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> getDocumentationUi() {
        String html = """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Hibana Ticketing — Microservices Developer Portal</title>
                <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🎫</text></svg>">
                <style>
                  :root {
                    --bg-primary: #0f172a;
                    --bg-secondary: #1e293b;
                    --text-main: #f8fafc;
                    --text-muted: #94a3b8;
                    --accent-hibana: #f43f5e;
                    --accent-cyan: #06b6d4;
                    --border-color: #334155;
                  }
                  * { box-sizing: border-box; margin: 0; padding: 0; }
                  body {
                    background-color: var(--bg-primary);
                    color: var(--text-main);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    overflow: hidden;
                    height: 100vh;
                    display: flex;
                    flex-direction: column;
                  }
                  header {
                    background: var(--bg-secondary);
                    border-bottom: 1px solid var(--border-color);
                    padding: 0.75rem 1.5rem;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    flex-shrink: 0;
                  }
                  .brand {
                    display: flex;
                    align-items: center;
                    gap: 0.75rem;
                  }
                  .badge-logo {
                    background: var(--accent-hibana);
                    color: white;
                    font-weight: 800;
                    font-size: 0.85rem;
                    padding: 0.25rem 0.6rem;
                    border-radius: 6px;
                    letter-spacing: 0.05em;
                  }
                  .title {
                    font-size: 1.1rem;
                    font-weight: 600;
                  }
                  .nav-tabs {
                    display: flex;
                    gap: 0.5rem;
                  }
                  .tab-btn {
                    background: transparent;
                    border: 1px solid transparent;
                    color: var(--text-muted);
                    padding: 0.45rem 1rem;
                    border-radius: 6px;
                    font-size: 0.875rem;
                    font-weight: 500;
                    cursor: pointer;
                    transition: all 0.15s ease-in-out;
                  }
                  .tab-btn:hover {
                    color: var(--text-main);
                    background: rgba(255, 255, 255, 0.05);
                  }
                  .tab-btn.active {
                    background: #2563eb;
                    color: #ffffff;
                    border-color: #3b82f6;
                  }
                  .actions {
                    display: flex;
                    gap: 0.5rem;
                  }
                  .spec-link {
                    background: var(--border-color);
                    color: var(--text-main);
                    text-decoration: none;
                    font-size: 0.75rem;
                    padding: 0.35rem 0.7rem;
                    border-radius: 4px;
                    font-weight: 600;
                    transition: background 0.15s;
                  }
                  .spec-link:hover {
                    background: #475569;
                  }
                  .view-container {
                    flex: 1;
                    height: calc(100vh - 60px);
                    position: relative;
                  }
                  .tab-content {
                    display: none;
                    width: 100%;
                    height: 100%;
                    overflow: auto;
                  }
                  .tab-content.active {
                    display: block;
                  }
                  /* Service Matrix Styling */
                  .matrix-container {
                    padding: 2rem;
                    max-width: 1400px;
                    margin: 0 auto;
                  }
                  .matrix-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 1rem;
                    font-size: 0.85rem;
                  }
                  .matrix-table th, .matrix-table td {
                    border: 1px solid var(--border-color);
                    padding: 0.75rem 1rem;
                    text-align: left;
                  }
                  .matrix-table th {
                    background: var(--bg-secondary);
                    color: var(--accent-cyan);
                    text-transform: uppercase;
                    font-size: 0.75rem;
                    letter-spacing: 0.05em;
                  }
                  .matrix-table tr:nth-child(even) {
                    background: rgba(255, 255, 255, 0.02);
                  }
                  .method {
                    font-weight: 700;
                    font-size: 0.75rem;
                    padding: 0.2rem 0.4rem;
                    border-radius: 4px;
                    display: inline-block;
                  }
                  .method-get { background: #065f46; color: #34d399; }
                  .method-post { background: #1e40af; color: #93c5fd; }
                  .method-delete { background: #991b1b; color: #fca5a5; }
                  .code-pill {
                    background: #334155;
                    padding: 0.2rem 0.5rem;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 0.8rem;
                  }
                </style>
              </head>
              <body>
                <header>
                  <div class="brand">
                    <span class="badge-logo">HIBANA</span>
                    <span class="title">Developer Portal — Edge Gateway (:8080)</span>
                  </div>
                  <nav class="nav-tabs">
                    <button class="tab-btn active" onclick="switchTab('rest-tab', this)">⚡ REST API (OpenAPI 3.1)</button>
                    <button class="tab-btn" onclick="switchTab('events-tab', this)">📡 Event Mesh & WebSockets (AsyncAPI 3.0)</button>
                    <button class="tab-btn" onclick="switchTab('matrix-tab', this)">📋 Gateway Routing Matrix</button>
                  </nav>
                  <div class="actions">
                    <a class="spec-link" href="/docs/openapi.yaml" target="_blank" download="openapi.yaml">OpenAPI YAML</a>
                    <a class="spec-link" href="/docs/asyncapi.yaml" target="_blank" download="asyncapi.yaml">AsyncAPI YAML</a>
                  </div>
                </header>

                <div class="view-container">
                  <!-- TAB 1: REST API (Scalar Interactive Console) -->
                  <div id="rest-tab" class="tab-content active">
                    <script id="api-reference" data-url="/docs/openapi.yaml" data-configuration='{"theme":"purple","layout":"modern","defaultHttpClient":{"targetKey":"javascript","clientKey":"fetch"}}'></script>
                    <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                  </div>

                  <!-- TAB 2: ASYNCAPI & EVENT BUS -->
                  <div id="events-tab" class="tab-content" style="padding: 2rem;">
                    <div style="max-width: 1200px; margin: 0 auto;">
                      <h2 style="color: var(--accent-cyan); margin-bottom: 0.5rem;">Azure Service Bus & Transactional Outbox Event Bus</h2>
                      <p style="color: var(--text-muted); margin-bottom: 2rem;">
                        Every cross-service mutation guarantees at-least-once delivery through PostgreSQL Transactional Outbox tables
                        before relaying to Azure Service Bus Topics. Consumers implement idempotent deduplication using the <code class="code-pill">eventId</code> UUIDv7 header.
                      </p>

                      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 1.5rem;">
                        <!-- Topic 1 -->
                        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1.5rem;">
                          <h3 style="color: #38bdf8; margin-bottom: 0.5rem;">topic/catalog.events</h3>
                          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;">Published by Catalog Service upon spectacle mutations.</p>
                          <ul style="list-style: none; font-size: 0.85rem; display: flex; flex-direction: column; gap: 0.5rem;">
                            <li><span class="code-pill">EventCreatedEvent</span> &rarr; Consumed by Inventory to create physical seat records.</li>
                            <li><span class="code-pill">EventPublishedEvent</span> &rarr; Triggers CDN cache warm-up and enables public sales.</li>
                            <li><span class="code-pill">EventCancelledEvent</span> &rarr; Triggers automated refund Saga and seat releases.</li>
                          </ul>
                        </div>

                        <!-- Topic 2 -->
                        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1.5rem;">
                          <h3 style="color: #34d399; margin-bottom: 0.5rem;">topic/inventory.events</h3>
                          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;">Published by Inventory Service under Redlock distributed lock.</p>
                          <ul style="list-style: none; font-size: 0.85rem; display: flex; flex-direction: column; gap: 0.5rem;">
                            <li><span class="code-pill">SeatHeldEvent</span> &rarr; Notifies seat lock acquisition with 10-minute TTL.</li>
                            <li><span class="code-pill">SeatReleasedEvent</span> &rarr; Fired on TTL expiration or active user cancellation.</li>
                            <li><span class="code-pill">ReservationConfirmedEvent</span> &rarr; Converts hold into permanent ticket issuance.</li>
                          </ul>
                        </div>

                        <!-- Topic 3 -->
                        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1.5rem;">
                          <h3 style="color: #f472b6; margin-bottom: 0.5rem;">topic/orders.events</h3>
                          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;">Published by Orders & Financial Engine during Checkout Saga.</p>
                          <ul style="list-style: none; font-size: 0.85rem; display: flex; flex-direction: column; gap: 0.5rem;">
                            <li><span class="code-pill">OrderCreatedEvent</span> &rarr; Registers uncommitted ledger balance.</li>
                            <li><span class="code-pill">OrderPaidEvent</span> &rarr; Authorizes ticket generation and email dispatch.</li>
                            <li><span class="code-pill">PaymentFailedEvent</span> &rarr; Triggers Saga compensation to release held seat.</li>
                            <li><span class="code-pill">OrderCancelledEvent</span> &rarr; Compensating transaction for refunds.</li>
                          </ul>
                        </div>

                        <!-- WebSocket Channel -->
                        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1.5rem;">
                          <h3 style="color: #fbbf24; margin-bottom: 0.5rem;">/ws/chat/{ticketId} (WebSocket)</h3>
                          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;">Spring WebFlux WebSocket with Redis Pub/Sub backend.</p>
                          <ul style="list-style: none; font-size: 0.85rem; display: flex; flex-direction: column; gap: 0.5rem;">
                            <li><span class="code-pill">ChatMessageSent</span> &rarr; Bi-directional frame (USER, OPERATOR, AI_ASSISTANT).</li>
                            <li><span class="code-pill">ChatSessionStarted</span> &rarr; Dispute session lifecycle and handoff.</li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- TAB 3: GATEWAY ROUTING MATRIX -->
                  <div id="matrix-tab" class="tab-content">
                    <div class="matrix-container">
                      <h2 style="color: var(--accent-cyan); margin-bottom: 0.5rem;">Spring Cloud Gateway Policy & Routing Matrix</h2>
                      <p style="color: var(--text-muted); margin-bottom: 1.5rem;">
                        All external traffic is processed through Gateway filters before reaching downstream microservices.
                      </p>
                      <table class="matrix-table">
                        <thead>
                          <tr>
                            <th>Method</th>
                            <th>Public Gateway Path</th>
                            <th>Downstream Target</th>
                            <th>Port</th>
                            <th>Required Security & Headers</th>
                            <th>Rate Limit</th>
                            <th>Circuit Breaker</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td><span class="method method-post">POST</span></td>
                            <td><code class="code-pill">/api/v1/queue/join</code></td>
                            <td>Gateway Native</td>
                            <td>8080</td>
                            <td>Public</td>
                            <td>100 req/min</td>
                            <td>N/A</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/queue/status</code></td>
                            <td>Gateway Native</td>
                            <td>8080</td>
                            <td>Public</td>
                            <td>100 req/min</td>
                            <td>N/A</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/queue/stream</code></td>
                            <td>Gateway Native (SSE)</td>
                            <td>8080</td>
                            <td>Public</td>
                            <td>Stream</td>
                            <td>N/A</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/events/**</code></td>
                            <td>Catalog Service</td>
                            <td>8081</td>
                            <td>Public</td>
                            <td>100 req/min</td>
                            <td>catalogCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/events/{id}/seats</code></td>
                            <td>Inventory Service</td>
                            <td>8082</td>
                            <td>Public</td>
                            <td>100 req/min</td>
                            <td>inventoryCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-post">POST</span></td>
                            <td><code class="code-pill">/api/v1/reservations</code></td>
                            <td>Inventory Service</td>
                            <td>8082</td>
                            <td><strong style="color: #f43f5e;">X-Checkout-Token</strong> (JWT)</td>
                            <td>100 req/min</td>
                            <td>inventoryCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-delete">DELETE</span></td>
                            <td><code class="code-pill">/api/v1/reservations/{id}</code></td>
                            <td>Inventory Service</td>
                            <td>8082</td>
                            <td>BearerAuth / System</td>
                            <td>100 req/min</td>
                            <td>inventoryCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-post">POST</span></td>
                            <td><code class="code-pill">/api/v1/orders</code></td>
                            <td>Orders Service</td>
                            <td>8083</td>
                            <td><code class="code-pill">X-User-Id</code>, <code class="code-pill">Trace-ID</code></td>
                            <td>100 req/min</td>
                            <td>ordersCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/orders/{id}</code></td>
                            <td>Orders Service</td>
                            <td>8083</td>
                            <td><code class="code-pill">X-User-Id</code></td>
                            <td>100 req/min</td>
                            <td>ordersCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-post">POST</span></td>
                            <td><code class="code-pill">/api/v1/orders/{id}/pay</code></td>
                            <td>Orders Service</td>
                            <td>8083</td>
                            <td><code class="code-pill">X-User-Id</code>, <code class="code-pill">Trace-ID</code></td>
                            <td>100 req/min</td>
                            <td>ordersCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-get">GET</span></td>
                            <td><code class="code-pill">/api/v1/orders/{id}/invoice</code></td>
                            <td>Orders Service</td>
                            <td>8083</td>
                            <td><code class="code-pill">X-User-Id</code> (PDF)</td>
                            <td>100 req/min</td>
                            <td>ordersCircuitBreaker</td>
                          </tr>
                          <tr>
                            <td><span class="method method-post">POST</span></td>
                            <td><code class="code-pill">/api/v1/webhooks/mercadopago</code></td>
                            <td>Orders Service</td>
                            <td>8083</td>
                            <td>Provider Signature</td>
                            <td>Unthrottled</td>
                            <td>N/A</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>

                <script>
                  function switchTab(tabId, btn) {
                    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
                    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
                    document.getElementById(tabId).classList.add('active');
                    btn.classList.add('active');
                  }
                </script>
              </body>
            </html>
            """;
        return Mono.just(ResponseEntity.ok(html));
    }

    @GetMapping(value = {"/docs/openapi.yaml", "/v3/api-docs.yaml"}, produces = "text/yaml;charset=UTF-8")
    public Mono<ResponseEntity<String>> getOpenApiYaml() {
        return readResourceContent(openApiSpec);
    }

    @GetMapping(value = "/docs/asyncapi.yaml", produces = "text/yaml;charset=UTF-8")
    public Mono<ResponseEntity<String>> getAsyncApiYaml() {
        return readResourceContent(asyncApiSpec);
    }

    private Mono<ResponseEntity<String>> readResourceContent(Resource resource) {
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return Mono.just(ResponseEntity.ok(content));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.internalServerError().body("# Error reading specification: " + e.getMessage()));
        }
    }
}
