-- Seed data for Venue and Event Catalog development
INSERT INTO organizers (id, name, legal_name, contact_email)
VALUES ('org_amaryllis_live', 'Amaryllis Live Productions', 'Amaryllis Entertainment SpA', 'contact@amaryllis.live')
ON CONFLICT (id) DO NOTHING;

INSERT INTO venues (id, name, city, country, address, latitude, longitude, seat_map_url)
VALUES ('ven_estadio_nacional', 'Estadio Nacional Julio Martínez Prádanos', 'Santiago', 'Chile', 'Av. Grecia 2001, Ñuñoa', -33.464167, -70.610556, 'http://localhost:4577/devstoreaccount1/venues/estadio-nacional-seatmap.json')
ON CONFLICT (id) DO NOTHING;

INSERT INTO events (
    id, organizer_id, venue_id, slug, title, status, category, genres,
    doors_open_at, starts_at, ends_at, timezone, media, sales_policy
) VALUES (
    'evt_coldplay_2026',
    'org_amaryllis_live',
    'ven_estadio_nacional',
    'coldplay-music-of-the-spheres-2026',
    'Coldplay - Music of the Spheres Tour 2026',
    'PUBLISHED',
    'CONCERT',
    ARRAY['Rock', 'Pop', 'Alternative'],
    NOW() + INTERVAL '10 days',
    NOW() + INTERVAL '10 days' + INTERVAL '4 hours',
    NOW() + INTERVAL '10 days' + INTERVAL '7 hours',
    'America/Santiago',
    '{"bannerUrl":"https://cdn.amaryllis-tickets.com/events/coldplay/banner.webp","posterUrl":"https://cdn.amaryllis-tickets.com/events/coldplay/poster.webp"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":true}'::jsonb
) ON CONFLICT (id) DO NOTHING;

INSERT INTO event_sectors (id, event_id, name, type, total_capacity, currency, base_price, service_fee)
VALUES 
    ('sec_coldplay_vip', 'evt_coldplay_2026', 'Cancha VIP Front', 'GENERAL_ADMISSION', 5000, 'ARS', 180.00, 27.00),
    ('sec_coldplay_andes', 'evt_coldplay_2026', 'Tribuna Andes', 'NUMBERED_SEATS', 12000, 'ARS', 85.00, 12.75)
ON CONFLICT (id) DO NOTHING;
