CREATE TABLE IF NOT EXISTS users (
  id CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(120),
  email VARCHAR(255),
  mobile VARCHAR(20) NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL,
  is_kyc_verified BOOLEAN NOT NULL DEFAULT FALSE,
  mobile_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS rides (
  id CHAR(36) NOT NULL PRIMARY KEY,
  owner_id CHAR(36) NOT NULL,
  origin_lat DECIMAL(10,7), origin_lng DECIMAL(10,7),
  destination_lat DECIMAL(10,7), destination_lng DECIMAL(10,7),
  from_location VARCHAR(255) NOT NULL, to_location VARCHAR(255) NOT NULL,
  start_time TIME, end_time TIME, date DATE NOT NULL,
  available_seats INT NOT NULL, total_seats INT NOT NULL, price DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_ride_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bookings (
  id CHAR(36) NOT NULL PRIMARY KEY,
  ride_id CHAR(36) NOT NULL, passenger_id CHAR(36) NOT NULL,
  status VARCHAR(20) NOT NULL, seats INT NOT NULL, booking_timestamp TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_booking_ride FOREIGN KEY (ride_id) REFERENCES rides(id),
  CONSTRAINT fk_booking_passenger FOREIGN KEY (passenger_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ratings (
  id CHAR(36) NOT NULL PRIMARY KEY,
  booking_id CHAR(36) NOT NULL UNIQUE, reviewer_id CHAR(36) NOT NULL, reviewee_id CHAR(36) NOT NULL,
  rating_score TINYINT NOT NULL, comments VARCHAR(1000), created_at TIMESTAMP NOT NULL,
  CONSTRAINT chk_rating_score CHECK (rating_score BETWEEN 1 AND 5),
  CONSTRAINT fk_rating_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT fk_rating_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_rating_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id)
);
