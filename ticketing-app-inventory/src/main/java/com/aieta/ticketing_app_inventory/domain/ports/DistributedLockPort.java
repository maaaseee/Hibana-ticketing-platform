package com.aieta.ticketing_app_inventory.domain.ports;

import java.time.Duration;

public interface DistributedLockPort {
    boolean acquireSeatLock(String eventId, String seatId, String userId, Duration ttl);
    boolean releaseSeatLock(String eventId, String seatId, String userId);
    void forceReleaseSeatLock(String eventId, String seatId);
}
