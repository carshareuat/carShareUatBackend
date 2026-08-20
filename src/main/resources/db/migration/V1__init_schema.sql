create table users (
    id char(36) primary key,
    role varchar(20) not null,
    mobile varchar(20) not null unique,
    password_hash varchar(255),
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table owner_profiles (
    id char(36) primary key,
    user_id char(36) not null unique,
    name varchar(120) not null,
    mobile varchar(20) not null,
    verified boolean not null default false,
    verification_status varchar(20) not null,
    profile_photo_url varchar(500),
    preferences varchar(1000),
    average_rating decimal(4,2) not null default 0.00,
    ratings_count bigint not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_owner_user foreign key (user_id) references users(id)
);

create table rides (
    id char(36) primary key,
    owner_id char(36) not null,
    from_location varchar(150) not null,
    to_location varchar(150) not null,
    date date not null,
    start_time time not null,
    end_time time not null,
    price decimal(10,2) not null,
    car_model varchar(100),
    total_seats int not null,
    available_seats int not null,
    status varchar(20) not null,
    cancellation_reason varchar(255),
    cancellation_note varchar(500),
    cancelled_at timestamp null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_ride_owner foreign key (owner_id) references owner_profiles(id)
);

create table bookings (
    id char(36) primary key,
    ride_id char(36) not null,
    passenger_id char(36) not null,
    passenger_mobile varchar(20) not null,
    seats int not null,
    status varchar(20) not null,
    cancellation_reason varchar(255),
    cancellation_note varchar(500),
    cancelled_by varchar(20),
    cancelled_at timestamp null,
    needs_rating boolean not null default false,
    rated boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_booking_ride foreign key (ride_id) references rides(id),
    constraint fk_booking_passenger foreign key (passenger_id) references users(id)
);

create table ratings (
    id char(36) primary key,
    booking_id char(36) not null unique,
    owner_id char(36) not null,
    passenger_id char(36) not null,
    rating int not null,
    note varchar(500),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_rating_booking foreign key (booking_id) references bookings(id),
    constraint fk_rating_owner foreign key (owner_id) references owner_profiles(id),
    constraint fk_rating_passenger foreign key (passenger_id) references users(id)
);

create table notifications (
    id char(36) primary key,
    user_id char(36) not null,
    type varchar(40) not null,
    title varchar(255) not null,
    body varchar(1000) not null,
    is_read boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_notification_user foreign key (user_id) references users(id)
);

create table subscriptions (
    id char(36) primary key,
    owner_id char(36) not null,
    amount int not null,
    currency varchar(10) not null,
    provider varchar(20) not null,
    provider_payment_id varchar(120),
    status varchar(20) not null,
    starts_at timestamp null,
    expires_at timestamp null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_subscription_owner foreign key (owner_id) references owner_profiles(id)
);

create table kyc_documents (
    id char(36) primary key,
    owner_id char(36) not null,
    type varchar(20) not null,
    storage_path varchar(500) not null,
    private_file boolean not null,
    mime_type varchar(100) not null,
    size_bytes bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_kyc_owner foreign key (owner_id) references owner_profiles(id)
);

create table refresh_tokens (
    id char(36) primary key,
    user_id char(36) not null,
    token_hash varchar(128) not null unique,
    expires_at timestamp not null,
    revoked boolean not null default false,
    ip_address varchar(45),
    user_agent varchar(255),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_refresh_user foreign key (user_id) references users(id)
);

create table audit_logs (
    id char(36) primary key,
    action varchar(80) not null,
    actor_id varchar(36) not null,
    target_id varchar(36),
    details varchar(1000) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp
);

create index idx_users_mobile on users(mobile);
create index idx_owner_user on owner_profiles(user_id);
create index idx_rides_owner on rides(owner_id);
create index idx_rides_status_date on rides(status, date);
create index idx_bookings_ride on bookings(ride_id);
create index idx_bookings_passenger on bookings(passenger_id);
create index idx_bookings_status on bookings(status);
create index idx_ratings_owner on ratings(owner_id);
create index idx_notifications_user on notifications(user_id);
create index idx_subscriptions_owner on subscriptions(owner_id);
create index idx_subscriptions_provider_payment on subscriptions(provider_payment_id);
create index idx_kyc_owner on kyc_documents(owner_id);
