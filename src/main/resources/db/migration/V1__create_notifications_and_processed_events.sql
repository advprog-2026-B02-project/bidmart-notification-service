-- V1: create notifications and processed_kafka_events tables

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    type varchar(50) NOT NULL,
    title varchar(160) NOT NULL,
    message varchar(500) NOT NULL,
    data text,
    is_read boolean NOT NULL DEFAULT false,
    read_at timestamp,
    related_auction_id uuid,
    related_order_id uuid,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS processed_kafka_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL,
    aggregate_type varchar(100) NOT NULL,
    aggregate_id varchar(100) NOT NULL,
    event_type varchar(100) NOT NULL,
    status varchar(20) NOT NULL,
    processed_at timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uk_processed_kafka_events_event_id UNIQUE (event_id)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications (created_at);
CREATE INDEX IF NOT EXISTS idx_processed_events_event_id ON processed_kafka_events (event_id);
