-- Create table to store latest passenger locations (MySQL-compatible)
CREATE TABLE passenger_locations (
  id CHAR(36) NOT NULL PRIMARY KEY,
  passenger_id CHAR(36) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX idx_passenger_locations_passenger ON passenger_locations(passenger_id);
