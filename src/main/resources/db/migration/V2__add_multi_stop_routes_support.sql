-- ============================================================================
-- Migration Script: Add Multi-Stop Routes with Segmented Pricing Support
-- Version: 2
-- Description: Add support for multi-stop routes with segment-level pricing
--              and seat availability management
-- ============================================================================

-- ============================================================================
-- Step 1: Update Rides Table - Add fields to support multi-stop routes
-- ============================================================================
ALTER TABLE rides ADD COLUMN is_multi_stop BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE rides ADD COLUMN total_stops INT DEFAULT 2;
ALTER TABLE rides ADD COLUMN pricing_type VARCHAR(20) DEFAULT 'FIXED' COMMENT 'FIXED or SEGMENTED';

-- Make the old fields nullable for multi-stop rides (use stops instead)
ALTER TABLE rides MODIFY from_location VARCHAR(150) NULL;
ALTER TABLE rides MODIFY to_location VARCHAR(150) NULL;
ALTER TABLE rides MODIFY start_time TIME NULL;
ALTER TABLE rides MODIFY end_time TIME NULL;

-- Create index for faster ride lookups by multi-stop flag
CREATE INDEX idx_rides_is_multi_stop ON rides(is_multi_stop);
CREATE INDEX idx_rides_pricing_type ON rides(pricing_type);

-- ============================================================================
-- Step 2: Create Ride Stops Table
-- ============================================================================
CREATE TABLE ride_stops (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID',
    ride_id CHAR(36) NOT NULL COMMENT 'Foreign key to rides table',
    stop_order INT NOT NULL COMMENT 'Sequential order of stops (0 = origin, 1,2,3... = intermediate/destination)',
    location_name VARCHAR(150) NOT NULL COMMENT 'Name of the stop location',
    latitude DECIMAL(10, 8) NULL COMMENT 'GPS latitude',
    longitude DECIMAL(11, 8) NULL COMMENT 'GPS longitude',
    arrival_time TIME NULL COMMENT 'Expected arrival time at this stop (NULL for first stop)',
    departure_time TIME NOT NULL COMMENT 'Expected departure time from this stop',
    stop_duration_minutes INT DEFAULT 0 COMMENT 'Duration to spend at this stop',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ride_stops_ride_id FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    CONSTRAINT uk_ride_stops_order UNIQUE KEY(ride_id, stop_order),
    INDEX idx_ride_stops_ride_id (ride_id),
    INDEX idx_ride_stops_stop_order (stop_order),
    INDEX idx_ride_stops_location (location_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Represents each stop in a multi-stop ride route';

-- ============================================================================
-- Step 3: Create Ride Segments Table
-- ============================================================================
-- A segment is a connection between two consecutive or non-consecutive stops
-- For a 4-stop ride (A->B->C->D), valid segments are:
-- A->B, A->C, A->D, B->C, B->D, C->D (6 segments = n*(n-1)/2)
CREATE TABLE ride_segments (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID',
    ride_id CHAR(36) NOT NULL COMMENT 'Foreign key to rides table',
    from_stop_id CHAR(36) NOT NULL COMMENT 'Starting stop',
    to_stop_id CHAR(36) NOT NULL COMMENT 'Ending stop',
    segment_order INT NOT NULL COMMENT 'Segment sequence number for ordering',
    price DECIMAL(10, 2) NOT NULL COMMENT 'Price for traveling this segment',
    available_seats INT NOT NULL COMMENT 'Current available seats for this segment',
    total_seats INT NOT NULL COMMENT 'Total seats allocated to this ride',
    distance_km DECIMAL(10, 2) NULL COMMENT 'Distance in kilometers (optional)',
    duration_minutes INT NULL COMMENT 'Expected travel duration in minutes',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ride_segments_ride_id FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_segments_from_stop_id FOREIGN KEY (from_stop_id) REFERENCES ride_stops(id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_segments_to_stop_id FOREIGN KEY (to_stop_id) REFERENCES ride_stops(id) ON DELETE CASCADE,
    CONSTRAINT uk_ride_segments UNIQUE KEY(ride_id, from_stop_id, to_stop_id),
    INDEX idx_ride_segments_ride_id (ride_id),
    INDEX idx_ride_segments_from_stop (from_stop_id),
    INDEX idx_ride_segments_to_stop (to_stop_id),
    INDEX idx_ride_segments_price (price),
    INDEX idx_ride_segments_available_seats (available_seats)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Represents journey segments between stops with pricing and seat availability';

-- ============================================================================
-- Step 4: Create Ride Segment Bookings Table
-- ============================================================================
-- Tracks which passenger booked which segment to manage seat availability
CREATE TABLE ride_segment_bookings (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID',
    ride_id CHAR(36) NOT NULL COMMENT 'Foreign key to rides table',
    booking_id CHAR(36) NOT NULL COMMENT 'Foreign key to bookings table',
    from_stop_id CHAR(36) NOT NULL COMMENT 'Booking start stop',
    to_stop_id CHAR(36) NOT NULL COMMENT 'Booking end stop',
    seat_count INT NOT NULL COMMENT 'Number of seats booked',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ride_seg_bookings_ride_id FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_seg_bookings_booking_id FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_seg_bookings_from_stop FOREIGN KEY (from_stop_id) REFERENCES ride_stops(id) ON DELETE CASCADE,
    CONSTRAINT fk_ride_seg_bookings_to_stop FOREIGN KEY (to_stop_id) REFERENCES ride_stops(id) ON DELETE CASCADE,
    INDEX idx_ride_seg_bookings_ride (ride_id),
    INDEX idx_ride_seg_bookings_booking (booking_id),
    INDEX idx_ride_seg_bookings_from_stop (from_stop_id),
    INDEX idx_ride_seg_bookings_to_stop (to_stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Junction table tracking passenger segment bookings for seat availability management';

-- ============================================================================
-- Step 5: Update Bookings Table - Add segment-specific fields
-- ============================================================================
ALTER TABLE bookings ADD COLUMN from_stop_id CHAR(36) NULL COMMENT 'Starting stop for segment booking';
ALTER TABLE bookings ADD COLUMN to_stop_id CHAR(36) NULL COMMENT 'Ending stop for segment booking';
ALTER TABLE bookings ADD COLUMN segment_price DECIMAL(10, 2) NULL COMMENT 'Price for the booked segment';

-- Add foreign keys to bookings table
ALTER TABLE bookings 
ADD CONSTRAINT fk_bookings_from_stop FOREIGN KEY (from_stop_id) REFERENCES ride_stops(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_bookings_to_stop FOREIGN KEY (to_stop_id) REFERENCES ride_stops(id) ON DELETE SET NULL;

-- Create indexes on bookings for faster segment lookups
CREATE INDEX idx_bookings_from_stop ON bookings(from_stop_id);
CREATE INDEX idx_bookings_to_stop ON bookings(to_stop_id);
CREATE INDEX idx_bookings_segment_price ON bookings(segment_price);

-- ============================================================================
-- Step 6: Create Indexes for Search Performance
-- ============================================================================
-- Optimize search for rides by date and stops
CREATE INDEX idx_rides_date ON rides(date);
CREATE INDEX idx_ride_stops_location_date ON ride_stops(location_name);

-- Optimize segment search with availability
CREATE INDEX idx_ride_segments_availability ON ride_segments(ride_id, available_seats);

-- Optimize booking searches
CREATE INDEX idx_ride_seg_bookings_segment ON ride_segment_bookings(from_stop_id, to_stop_id);

-- ============================================================================
-- Step 7: Add Audit Columns (if not already present)
-- ============================================================================
-- These may already exist, so we wrap in conditional (if supported by your DB version)
-- Most tables inherit from BaseEntity which has created_at and updated_at

-- ============================================================================
-- End of Migration
-- ============================================================================
