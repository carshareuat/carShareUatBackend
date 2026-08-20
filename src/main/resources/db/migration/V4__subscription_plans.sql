create table if not exists subscription_plans (
    id char(36) primary key,
    code varchar(40) not null unique,
    name varchar(120) not null,
    amount_paise int not null,
    currency varchar(10) not null,
    duration_months int not null,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp
);