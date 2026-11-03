package com.aieta.ticketing_app_gateway.controller;

import com.aieta.ticketing_app_gateway.dto.QueueJoinRequest;
import com.aieta.ticketing_app_gateway.dto.QueueStatusResponse;
import com.aieta.ticketing_app_gateway.service.WaitingRoomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
public class WaitingRoomController {

    private static final Logger log = LoggerFactory.getLogger(WaitingRoomController.class);

    public static final String COOKIE_QUEUE_SESSION = "HIBANA_QUEUE_SESSION";
    public static final String HEADER_QUEUE_SESSION = "X-Queue-Session";
    public static final String HEADER_QUEUE_TOKEN = "X-Queue-Token";

    private final WaitingRoomService waitingRoomService;
    private final ObjectMapper objectMapper;

    public WaitingRoomController(WaitingRoomService waitingRoomService) {
        this(waitingRoomService, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WaitingRoomController(WaitingRoomService waitingRoomService, ObjectMapper objectMapper) {
        this.waitingRoomService = waitingRoomService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "guest-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return userId;
    }

    @PostMapping("/join")
    public Mono<ResponseEntity<QueueStatusResponse>> joinQueue(
            @RequestBody QueueJoinRequest request,
            ServerWebExchange exchange) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        String sessionToken = waitingRoomService.mintQueueSessionToken(eventId, userId);

        ResponseCookie cookie = ResponseCookie.from(COOKIE_QUEUE_SESSION, sessionToken)
                .path("/api/v1/queue")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(WaitingRoomService.QUEUE_SESSION_TTL)
                .build();
        exchange.getResponse().addCookie(cookie);

        return waitingRoomService.joinQueue(eventId, userId)
                .map(res -> ResponseEntity.ok()
                        .header(HEADER_QUEUE_SESSION, sessionToken)
                        .body(res));
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<?>> getQueueStatus(
            @RequestParam("eventId") String eventId,
            @RequestParam("userId") String userId,
            ServerWebExchange exchange
    ) {
        if (!isUserAuthorizedForQueue(eventId, userId, exchange)) {
            log.warn("[WAITING-ROOM] Unauthorized queue status query for event={} user={}", eventId, userId);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Access to queue status requires a valid queue session for the specified user and event."
            );
            problem.setType(URI.create("https://hibana.tk/errors/forbidden"));
            problem.setTitle("Queue State Access Denied");
            problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));
            problem.setProperty("timestamp", Instant.now().toString());

            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem));
        }

        return waitingRoomService.getQueueStatus(eventId, userId)
                .map(res -> ResponseEntity.ok(res));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<QueueStatusResponse>> streamQueueStatus(
            @RequestParam("eventId") String eventId,
            @RequestParam("userId") String userId,
            ServerWebExchange exchange
    ) {
        if (!isUserAuthorizedForQueue(eventId, userId, exchange)) {
            log.warn("[WAITING-ROOM] Unauthorized queue stream attempt for event={} user={}", eventId, userId);
            return Flux.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access to queue stream requires a valid queue session for the specified user and event."
            ));
        }

        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .concatMap(tick -> waitingRoomService.getQueueStatus(eventId, userId))
                .map(status -> ServerSentEvent.<QueueStatusResponse>builder()
                        .event("queue-status")
                        .data(status)
                        .build()
                )
                .takeUntil(sse -> sse.data() != null && "ADMITTED".equalsIgnoreCase(sse.data().status()));
    }

    @PostMapping("/leave")
    public Mono<ResponseEntity<Void>> leaveQueue(
            @RequestBody QueueJoinRequest request,
            ServerWebExchange exchange) {
        String eventId = request.eventId() != null ? request.eventId() : "default-event";
        String userId = resolveUserId(request.userId());

        ResponseCookie clearCookie = ResponseCookie.from(COOKIE_QUEUE_SESSION, "")
                .path("/api/v1/queue")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        exchange.getResponse().addCookie(clearCookie);

        return waitingRoomService.leaveQueue(eventId, userId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    private boolean isUserAuthorizedForQueue(String eventId, String userId, ServerWebExchange exchange) {
        // 1. Check HTTP-only session cookie
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_QUEUE_SESSION);
        if (cookie != null && waitingRoomService.validateQueueSessionToken(eventId, userId, cookie.getValue())) {
            return true;
        }

        // 2. Check X-Queue-Session or X-Queue-Token header
        String headerToken = exchange.getRequest().getHeaders().getFirst(HEADER_QUEUE_SESSION);
        if (headerToken == null || headerToken.isBlank()) {
            headerToken = exchange.getRequest().getHeaders().getFirst(HEADER_QUEUE_TOKEN);
        }
        if (headerToken != null && !headerToken.isBlank() && waitingRoomService.validateQueueSessionToken(eventId, userId, headerToken)) {
            return true;
        }

        // 3. Check query param sessionToken or queueToken
        String queryToken = exchange.getRequest().getQueryParams().getFirst("sessionToken");
        if (queryToken == null || queryToken.isBlank()) {
            queryToken = exchange.getRequest().getQueryParams().getFirst("queueToken");
        }
        if (queryToken != null && !queryToken.isBlank() && waitingRoomService.validateQueueSessionToken(eventId, userId, queryToken)) {
            return true;
        }

        // 4. Check Authorization: Bearer <jwt> claim match
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authHeader.substring(7).trim();
            if (isBearerTokenMatchingUser(userId, token)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBearerTokenMatchingUser(String userId, String token) {
        try {
            int firstDot = token.indexOf('.');
            int secondDot = token.indexOf('.', firstDot + 1);
            if (firstDot > 0 && secondDot > firstDot) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(token.substring(firstDot + 1, secondDot)), StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(payloadJson);
                String sub = node.path("sub").asText(null);
                String oid = node.path("oid").asText(null);
                String preferredUsername = node.path("preferred_username").asText(null);
                String email = node.path("email").asText(null);
                String upn = node.path("upn").asText(null);

                return userId.equalsIgnoreCase(sub)
                        || userId.equalsIgnoreCase(oid)
                        || userId.equalsIgnoreCase(preferredUsername)
                        || userId.equalsIgnoreCase(email)
                        || userId.equalsIgnoreCase(upn);
            }
        } catch (Exception e) {
            log.debug("Error inspecting bearer token for queue ownership check: {}", e.getMessage());
        }
        return false;
    }
}
