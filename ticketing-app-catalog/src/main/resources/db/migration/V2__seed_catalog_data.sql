-- V2__seed_catalog_data.sql: Initial seed data for Venues, Organizers, Events, and Sectors
INSERT INTO organizers (id, name, legal_name, contact_email)
VALUES 
    ('org_amaryllis_live', 'Amaryllis Live Productions', 'Amaryllis Entertainment SpA', 'contact@amaryllis.live'),
    ('org_df_entertainment', 'DF Entertainment', 'DF Entertainment S.A.', 'produccion@dfentertainment.com')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    legal_name = EXCLUDED.legal_name,
    contact_email = EXCLUDED.contact_email;

INSERT INTO venues (id, name, city, country, address, latitude, longitude, seat_map_url, image_url)
VALUES 
    ('ven_estadio_nacional', 'Estadio Nacional Julio Martínez Prádanos', 'Santiago', 'Chile', 'Av. Grecia 2001, Ñuñoa', -33.464167, -70.610556, 'http://localhost:4577/devstoreaccount1/venues/estadio-nacional-seatmap.json', 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80'),
    ('estadio-monumental', 'Estadio Monumental (Buenos Aires)', 'Buenos Aires', 'Argentina', 'Av. Figueroa Alcorta 7597, Núñez, CABA', -34.545278, -58.449722, 'http://localhost:4577/devstoreaccount1/venues/estadio-monumental.json', 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80'),
    ('arena-buenos-aires', 'Arena Buenos Aires (Buenos Aires)', 'Buenos Aires', 'Argentina', 'Humboldt 450, Villa Crespo, CABA', -34.593611, -58.446111, 'http://localhost:4577/devstoreaccount1/venues/arena-buenos-aires.json', '/images/venues/arena_buenos_aires.jpg'),
    ('teatro-rex', 'Teatro Rex (Buenos Aires)', 'Buenos Aires', 'Argentina', 'Av. Corrientes 857, San Nicolás, CABA', -34.603333, -58.378889, 'http://localhost:4577/devstoreaccount1/venues/teatro-rex.json', 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    city = EXCLUDED.city,
    country = EXCLUDED.country,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    seat_map_url = EXCLUDED.seat_map_url,
    image_url = EXCLUDED.image_url;

INSERT INTO events (
    id, organizer_id, venue_id, slug, title, status, category, genres,
    doors_open_at, starts_at, ends_at, timezone, media, sales_policy, created_at
) VALUES (
    'evt_coldplay_2026',
    'org_amaryllis_live',
    'ven_estadio_nacional',
    'coldplay-music-of-the-spheres-2026',
    'Coldplay - Music of the Spheres Tour 2026',
    'PUBLISHED',
    'CONCERT',
    ARRAY['Rock', 'Pop', 'Alternative'],
    '2026-10-27 18:00:00-03',
    '2026-10-27 21:00:00-03',
    '2026-10-28 00:00:00-03',
    'America/Santiago',
    '{"bannerUrl":"/images/events/coldplay-tour.jpg","posterUrl":"/images/events/coldplay-tour.jpg"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":true}'::jsonb,
    '2026-05-15 10:00:00-03'
),
(
    'river-stadium-rockfest',
    'org_df_entertainment',
    'estadio-monumental',
    'river-stadium-rockfest-2026',
    'River Stadium Rock Festival 2026',
    'PUBLISHED',
    'CONCERT',
    ARRAY['Rock', 'Alternative', 'Hard Rock'],
    '2026-11-05 18:00:00-03',
    '2026-11-05 21:00:00-03',
    '2026-11-06 01:00:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=1200&q=80","posterUrl":"https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=800&q=80"}'::jsonb,
    '{"maxTicketsPerUser":6,"waitingRoomEnabled":true}'::jsonb,
    '2026-06-10 11:30:00-03'
),
(
    'symphony-of-lights',
    'org_amaryllis_live',
    'arena-buenos-aires',
    'symphony-of-lights-2026',
    'Symphony of Lights 2026',
    'PUBLISHED',
    'CONCERT',
    ARRAY['Orchestral', 'Indie', 'Visuals'],
    '2026-11-15 18:00:00-03',
    '2026-11-15 20:00:00-03',
    '2026-11-15 23:30:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80","posterUrl":"https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=800&q=80"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":false}'::jsonb,
    '2026-06-25 16:00:00-03'
),
(
    'chronos-fest-2026',
    'org_amaryllis_live',
    'arena-buenos-aires',
    'neon-nights-electronic-festival-2026',
    'Neon Nights: Electronic Festival',
    'PUBLISHED',
    'CONCERT',
    ARRAY['Electronic', 'Techno', 'Melodic'],
    '2026-11-24 18:30:00-03',
    '2026-11-24 21:00:00-03',
    '2026-11-25 04:00:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80","posterUrl":"https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":true}'::jsonb,
    '2026-10-21 15:00:00-03'
),
(
    'opera-clasica-verano',
    'org_amaryllis_live',
    'teatro-rex',
    'gala-lirica-opera-clasica-verano',
    'Gala Lírica: Grandes Clásicos de la Ópera',
    'PUBLISHED',
    'THEATER',
    ARRAY['Opera', 'Classical', 'Acoustic'],
    '2026-12-10 19:00:00-03',
    '2026-12-10 20:30:00-03',
    '2026-12-10 23:00:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80","posterUrl":"https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=800&q=80"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":false}'::jsonb,
    '2026-10-22 17:00:00-03'
),
(
    'cloud-native-devcon',
    'org_amaryllis_live',
    'teatro-rex',
    'cloud-native-ai-devcon-2027',
    'Cloud Native & AI DevCon 2027',
    'PUBLISHED',
    'CONFERENCE',
    ARRAY['Technology', 'AI', 'Cloud'],
    '2027-05-22 08:00:00-03',
    '2027-05-22 09:30:00-03',
    '2027-05-22 18:00:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80","posterUrl":"https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80"}'::jsonb,
    '{"maxTicketsPerUser":2,"waitingRoomEnabled":false}'::jsonb,
    '2026-10-23 20:00:00-03'
),
(
    'ado-hibana-world-tour',
    'org_amaryllis_live',
    'arena-buenos-aires',
    'ado-world-tour-2026-hibana',
    'Ado World Tour 2026: Hibana',
    'PUBLISHED',
    'CONCERT',
    ARRAY['J-Pop', 'Rock', 'Electronic', 'Anime'],
    '2026-11-18 18:00:00-03',
    '2026-11-18 20:30:00-03',
    '2026-11-18 23:30:00-03',
    'America/Argentina/Buenos_Aires',
    '{"bannerUrl":"/images/events/ado-hibana-world-tour.png","posterUrl":"/images/events/ado-hibana-world-tour.png"}'::jsonb,
    '{"maxTicketsPerUser":4,"waitingRoomEnabled":true,"functions":[{"id":"fn-ado-1","dateIso":"2026-11-18T20:30:00","formattedDate":"Miércoles 18 de Noviembre de 2026","doorsOpen":"18:00 hs","showStart":"20:30 hs","status":"AVAILABLE"},{"id":"fn-ado-2","dateIso":"2026-11-19T20:30:00","formattedDate":"Jueves 19 de Noviembre de 2026","doorsOpen":"18:00 hs","showStart":"20:30 hs","status":"AVAILABLE"}]}'::jsonb,
    '2026-10-24 02:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET 
    title = EXCLUDED.title,
    slug = EXCLUDED.slug,
    status = EXCLUDED.status,
    category = EXCLUDED.category,
    genres = EXCLUDED.genres,
    doors_open_at = EXCLUDED.doors_open_at,
    starts_at = EXCLUDED.starts_at,
    ends_at = EXCLUDED.ends_at,
    timezone = EXCLUDED.timezone,
    media = EXCLUDED.media,
    sales_policy = EXCLUDED.sales_policy,
    created_at = EXCLUDED.created_at;

INSERT INTO event_sectors (id, event_id, name, type, total_capacity, currency, base_price, service_fee)
VALUES 
    -- Coldplay (Estadio Nacional)
    ('sec_coldplay_vip', 'evt_coldplay_2026', 'Cancha VIP Front', 'GENERAL_ADMISSION', 5000, 'ARS', 180.00, 27.00),
    ('sec_coldplay_andes', 'evt_coldplay_2026', 'Tribuna Andes', 'NUMBERED_SEATS', 12000, 'ARS', 85.00, 12.75),

    -- River Stadium Rock Festival (Estadio Monumental)
    ('river__sec-monumental-campo-vip', 'river-stadium-rockfest', 'Campo Delantero VIP', 'VIP', 12000, 'ARS', 121739.13, 18260.87),
    ('river__sec-monumental-campo-general', 'river-stadium-rockfest', 'Campo General Trasero', 'GENERAL_ADMISSION', 16000, 'ARS', 43478.26, 6521.74),
    ('river__sec-monumental-platea-alta-sanmartin-norte', 'river-stadium-rockfest', 'Platea San Martín Alta (Visión Reducida)', 'NUMBERED_SEATS', 3000, 'ARS', 56521.74, 8478.26),
    ('river__sec-monumental-platea-alta-sanmartin', 'river-stadium-rockfest', 'Platea San Martín Alta', 'NUMBERED_SEATS', 8000, 'ARS', 73913.04, 11086.96),
    ('river__sec-monumental-platea-baja-sanmartin-norte', 'river-stadium-rockfest', 'Platea San Martín Baja (Visión Reducida)', 'NUMBERED_SEATS', 2500, 'ARS', 65217.39, 9782.61),
    ('river__sec-monumental-platea-baja-sanmartin', 'river-stadium-rockfest', 'Platea San Martín Baja / Media', 'NUMBERED_SEATS', 6500, 'ARS', 95652.17, 14347.83),
    ('river__sec-monumental-platea-alta-belgrano-norte', 'river-stadium-rockfest', 'Platea Belgrano Alta (Visión Reducida)', 'NUMBERED_SEATS', 3000, 'ARS', 56521.74, 8478.26),
    ('river__sec-monumental-platea-alta-belgrano', 'river-stadium-rockfest', 'Platea Belgrano Alta', 'NUMBERED_SEATS', 8000, 'ARS', 73913.04, 11086.96),
    ('river__sec-monumental-platea-baja-belgrano-norte', 'river-stadium-rockfest', 'Platea Belgrano Baja (Visión Reducida)', 'NUMBERED_SEATS', 2500, 'ARS', 65217.39, 9782.61),
    ('river__sec-monumental-platea-baja-belgrano', 'river-stadium-rockfest', 'Platea Belgrano Baja / Media', 'NUMBERED_SEATS', 6500, 'ARS', 95652.17, 14347.83),
    ('river__sec-monumental-centenario-baja', 'river-stadium-rockfest', 'Tribuna Centenario Media / Baja', 'GENERAL_ADMISSION', 8000, 'ARS', 39130.43, 5869.57),
    ('river__sec-monumental-centenario-alta', 'river-stadium-rockfest', 'Tribuna Centenario Alta', 'GENERAL_ADMISSION', 8567, 'ARS', 33043.48, 4956.52),

    -- Symphony of Lights 2026 (Arena Buenos Aires)
    ('symphony__sec-arena-campo-vip', 'symphony-of-lights', 'Campo VIP Frontal', 'VIP', 3000, 'ARS', 82608.70, 12391.30),
    ('symphony__sec-arena-campo-general', 'symphony-of-lights', 'Campo General', 'GENERAL_ADMISSION', 3500, 'ARS', 56521.74, 8478.26),
    ('symphony__sec-arena-platea-baja-lateral-oeste', 'symphony-of-lights', 'Platea Baja Oeste', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('symphony__sec-arena-platea-baja-lateral-este', 'symphony-of-lights', 'Platea Baja Este', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('symphony__sec-arena-platea-baja-cabecera', 'symphony-of-lights', 'Platea Baja Sur', 'NUMBERED_SEATS', 1500, 'ARS', 69565.22, 10434.78),
    ('symphony__sec-arena-platea-alta-lateral-oeste', 'symphony-of-lights', 'Platea Alta Oeste', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('symphony__sec-arena-platea-alta-lateral-este', 'symphony-of-lights', 'Platea Alta Este', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('symphony__sec-arena-platea-alta-cabecera', 'symphony-of-lights', 'Platea Alta Sur', 'NUMBERED_SEATS', 1400, 'ARS', 39130.43, 5869.57),
    ('symphony__sec-arena-platea-alta-cabecera-a2', 'symphony-of-lights', 'Platea Alta Sur (Zona A2 Central)', 'NUMBERED_SEATS', 700, 'ARS', 42000.00, 6300.00),
    ('symphony__sec-arena-platea-alta-cabecera-a1', 'symphony-of-lights', 'Platea Alta Sur (Zona A1 Oeste)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00),
    ('symphony__sec-arena-platea-alta-cabecera-a3', 'symphony-of-lights', 'Platea Alta Sur (Zona A3 Este)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00),

    -- Neon Nights: Chronos Fest (Arena Buenos Aires)
    ('chronos__sec-arena-campo-vip', 'chronos-fest-2026', 'Campo VIP Frontal', 'VIP', 3000, 'ARS', 82608.70, 12391.30),
    ('chronos__sec-arena-campo-general', 'chronos-fest-2026', 'Campo General', 'GENERAL_ADMISSION', 3500, 'ARS', 56521.74, 8478.26),
    ('chronos__sec-arena-platea-baja-lateral-oeste', 'chronos-fest-2026', 'Platea Baja Oeste', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('chronos__sec-arena-platea-baja-lateral-este', 'chronos-fest-2026', 'Platea Baja Este', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('chronos__sec-arena-platea-baja-cabecera', 'chronos-fest-2026', 'Platea Baja Sur', 'NUMBERED_SEATS', 1500, 'ARS', 69565.22, 10434.78),
    ('chronos__sec-arena-platea-alta-lateral-oeste', 'chronos-fest-2026', 'Platea Alta Oeste', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('chronos__sec-arena-platea-alta-lateral-este', 'chronos-fest-2026', 'Platea Alta Este', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('chronos__sec-arena-platea-alta-cabecera', 'chronos-fest-2026', 'Platea Alta Sur', 'NUMBERED_SEATS', 1400, 'ARS', 39130.43, 5869.57),
    ('chronos__sec-arena-platea-alta-cabecera-a2', 'chronos-fest-2026', 'Platea Alta Sur (Zona A2 Central)', 'NUMBERED_SEATS', 700, 'ARS', 42000.00, 6300.00),
    ('chronos__sec-arena-platea-alta-cabecera-a1', 'chronos-fest-2026', 'Platea Alta Sur (Zona A1 Oeste)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00),
    ('chronos__sec-arena-platea-alta-cabecera-a3', 'chronos-fest-2026', 'Platea Alta Sur (Zona A3 Este)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00),

    -- Gala Lírica: Grandes Clásicos de la Ópera (Teatro Rex)
    ('opera__sec-rex-palcos-izq', 'opera-clasica-verano', 'Palcos Bajos Izquierda', 'VIP', 100, 'ARS', 78260.87, 11739.13),
    ('opera__sec-rex-platea-platino', 'opera-clasica-verano', 'Platea Platino (Filas 1–12)', 'VIP', 700, 'ARS', 69565.22, 10434.78),
    ('opera__sec-rex-platea-oro', 'opera-clasica-verano', 'Platea Oro (Filas 13–25)', 'NUMBERED_SEATS', 850, 'ARS', 56521.74, 8478.26),
    ('opera__sec-rex-palcos-der', 'opera-clasica-verano', 'Palcos Bajos Derecha', 'VIP', 100, 'ARS', 78260.87, 11739.13),
    ('opera__sec-rex-super-pullman', 'opera-clasica-verano', 'Super Pullman (Primer Piso)', 'NUMBERED_SEATS', 650, 'ARS', 43478.26, 6521.74),
    ('opera__sec-rex-pullman', 'opera-clasica-verano', 'Pullman (Segundo Nivel)', 'NUMBERED_SEATS', 600, 'ARS', 33043.48, 4956.52),
    ('opera__sec-rex-tertulia', 'opera-clasica-verano', 'Tertulia Superior', 'NUMBERED_SEATS', 262, 'ARS', 21739.13, 3260.87),

    -- Cloud Native DevCon (Teatro Rex)
    ('devcon__sec-rex-platea-platino', 'cloud-native-devcon', 'Platea Platino (VIP Pass)', 'VIP', 700, 'ARS', 69565.22, 10434.78),
    ('devcon__sec-rex-platea-oro', 'cloud-native-devcon', 'Platea Oro (Standard Pass)', 'NUMBERED_SEATS', 850, 'ARS', 56521.74, 8478.26),
    ('devcon__sec-rex-super-pullman', 'cloud-native-devcon', 'Super Pullman (Student Pass)', 'NUMBERED_SEATS', 650, 'ARS', 30434.78, 4565.22),
    ('devcon__sec-rex-tertulia', 'cloud-native-devcon', 'Tertulia Superior (General Pass)', 'NUMBERED_SEATS', 262, 'ARS', 21739.13, 3260.87),

    -- Ado World Tour 2026: Hibana (Arena Buenos Aires)
    ('ado__sec-arena-campo-vip', 'ado-hibana-world-tour', 'Campo VIP Frontal', 'VIP', 3000, 'ARS', 82608.70, 12391.30),
    ('ado__sec-arena-campo-general', 'ado-hibana-world-tour', 'Campo General', 'GENERAL_ADMISSION', 3500, 'ARS', 56521.74, 8478.26),
    ('ado__sec-arena-platea-baja-lateral-oeste', 'ado-hibana-world-tour', 'Platea Baja Oeste', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('ado__sec-arena-platea-baja-lateral-este', 'ado-hibana-world-tour', 'Platea Baja Este', 'NUMBERED_SEATS', 1500, 'ARS', 73913.04, 11086.96),
    ('ado__sec-arena-platea-baja-cabecera', 'ado-hibana-world-tour', 'Platea Baja Sur', 'NUMBERED_SEATS', 1500, 'ARS', 69565.22, 10434.78),
    ('ado__sec-arena-platea-alta-lateral-oeste', 'ado-hibana-world-tour', 'Platea Alta Oeste', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('ado__sec-arena-platea-alta-lateral-este', 'ado-hibana-world-tour', 'Platea Alta Este', 'NUMBERED_SEATS', 1300, 'ARS', 43478.26, 6521.74),
    ('ado__sec-arena-platea-alta-cabecera', 'ado-hibana-world-tour', 'Platea Alta Sur', 'NUMBERED_SEATS', 1400, 'ARS', 39130.43, 5869.57),
    ('ado__sec-arena-platea-alta-cabecera-a2', 'ado-hibana-world-tour', 'Platea Alta Sur (Zona A2 Central)', 'NUMBERED_SEATS', 700, 'ARS', 42000.00, 6300.00),
    ('ado__sec-arena-platea-alta-cabecera-a1', 'ado-hibana-world-tour', 'Platea Alta Sur (Zona A1 Oeste)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00),
    ('ado__sec-arena-platea-alta-cabecera-a3', 'ado-hibana-world-tour', 'Platea Alta Sur (Zona A3 Este)', 'NUMBERED_SEATS', 350, 'ARS', 35000.00, 5250.00)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    total_capacity = EXCLUDED.total_capacity,
    currency = EXCLUDED.currency,
    base_price = EXCLUDED.base_price,
    service_fee = EXCLUDED.service_fee;
