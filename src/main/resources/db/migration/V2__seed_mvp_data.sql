INSERT IGNORE INTO users (id, role, mobile, active)
VALUES
('11111111-1111-1111-1111-111111111111', 'OWNER', '+919800000001', true),
('22222222-2222-2222-2222-222222222222', 'OWNER', '+919800000002', true);

INSERT IGNORE INTO owner_profiles (id, user_id, name, mobile, verified, verification_status, preferences, average_rating, ratings_count)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', 'Ravi Kumar', '+919800000001', false, 'PENDING', 'Non-smoking rides', 4.60, 5),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '22222222-2222-2222-2222-222222222222', 'Sita Sharma', '+919800000002', true, 'VERIFIED', 'Women-friendly rides', 4.90, 12);

INSERT IGNORE INTO rides (id, owner_id, from_location, to_location, date, start_time, end_time, price, car_model, total_seats, available_seats, status)
VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'MG Road, Bangalore', 'Whitefield, Bangalore', current_date + interval 1 day, '09:00:00', '10:00:00', 120.00, 'Hyundai i20', 4, 4, 'ACTIVE'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Indiranagar, Bangalore', 'Electronic City, Bangalore', current_date + interval 1 day, '18:00:00', '19:00:00', 150.00, 'Honda City', 3, 3, 'ACTIVE');
