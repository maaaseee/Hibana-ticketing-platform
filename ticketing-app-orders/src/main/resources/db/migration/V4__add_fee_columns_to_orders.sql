-- V4__add_fee_columns_to_orders.sql: Add subtotal_amount and service_fee_amount to orders
SET search_path = orders;

ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS subtotal_amount NUMERIC(10,2) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS service_fee_amount NUMERIC(10,2) DEFAULT NULL;
