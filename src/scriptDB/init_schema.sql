-- ====================================================================================================
-- PROJECT: TECHMART — ELECTRONIC STORE MANAGEMENT SYSTEM
-- SCRIPT: init_schema.sql (MASTER DATABASE DDL SPECIFICATION)
-- DATABASE ENGINE: PostgreSQL 16+
-- DESCRIPTION: Complete schema definition for 16 core tables, constraints, extensions, and indexes.
-- NOTE: Seed data excluded per plan — data will be populated and tested via REST APIs during execution.
-- ====================================================================================================

-- ====================================================================================================
-- PART 1: CORE EXTENSIONS
-- ====================================================================================================

-- pg_trgm: Provides trigram operators (gin_trgm_ops) for ultra-fast fuzzy and substring search (< 30ms) on product names.
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- uuid-ossp: Provides standard UUID generation functions (e.g. uuid_generate_v4()) for manual database operations.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ====================================================================================================
-- PART 2: CORE TABLES & INTEGRITY CONSTRAINTS
-- ====================================================================================================

-- ----------------------------------------------------------------------------------------------------
-- TABLE 1: users
-- PURPOSE & BUSINESS LOGIC:
--   - Stores account credentials and profiles for all user roles (ADMIN, STAFF, CUSTOMER).
--   - Supports temporary account lockout after 5 consecutive failed login attempts (failed_login_attempts & locked_until).
--   - Implements soft delete via 'deleted' flag to preserve foreign key references in historical orders.
--   - DYNAMIC ADDRESS BOOK (user_address_info): JSONB array storing up to 10 delivery addresses.
--     Schema: [{"id": "UUID", "name": "...", "phone": "...", "street": "...", "province": "...", "city": "...", "isDefault": boolean}]
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    user_address_info JSONB NOT NULL DEFAULT '[]'::jsonb,
    avatar_url VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'STAFF', 'CUSTOMER')),
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 2: refresh_tokens
-- PURPOSE & BUSINESS LOGIC:
--   - Manages JWT Refresh Token lifecycle with 7-day expiration and Refresh Token Rotation (RTR).
--   - Stores SHA-256 hash (token_hash) instead of raw token strings to prevent token leakage on database dump.
--   - Replay Attack Detection: If a revoked token is used, all active sessions for the user are immediately revoked.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_refresh_tokens_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 3: categories
-- PURPOSE & BUSINESS LOGIC:
--   - Hierarchical product classification tree (e.g. Mobile > iPhone, Components > RAM > RAM 16GB).
--   - Combines Adjacency List (parent_id) with Materialized Path (path column) for O(1) subtree querying via LIKE '/mobile/%'.
--   - Enforces cycle-free tree structure (parent_id != id) and depth limit of at most 4 levels (level <= 4).
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id UUID,
    level INTEGER NOT NULL DEFAULT 1,
    path VARCHAR(500) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_categories_level CHECK (level >= 1 AND level <= 4),
    CONSTRAINT chk_categories_no_self_parent CHECK (parent_id IS NULL OR parent_id != id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 4: brands
-- PURPOSE & BUSINESS LOGIC:
--   - Stores electronic hardware, device, and computer manufacturers (Apple, ASUS, Dell, Samsung...).
--   - Normalizes relationship: 1 Brand has Many Products (Brand 1 - N Product).
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS brands (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_brands_name UNIQUE (name),
    CONSTRAINT fk_brands_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_brands_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 5: branches
-- PURPOSE & BUSINESS LOGIC:
--   - Manages physical store locations and fulfillment warehouses.
--   - quantity_available: Aggregate counter representing total available stock units across all products at this branch.
--   - Granular inventory per variant is strictly tracked in the 'inventory' table.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS branches (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(20),
    quantity_available INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_branches_name UNIQUE (name),
    CONSTRAINT chk_branches_quantity_available CHECK (quantity_available >= 0),
    CONSTRAINT fk_branches_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_branches_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 6: products
-- PURPOSE & BUSINESS LOGIC:
--   - Parent product entity (Catalog Identity: Product Name, Unique SEO Slug, Brand, Category, Thumbnail, Description).
--   - Invariant: Does NOT store static SKU or price. Pricing and stock are decoupled to child product variants.
--   - Soft delete via status: ACTIVE, INACTIVE, DELETED.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 7: product_variants
-- PURPOSE & BUSINESS LOGIC:
--   - Retail sellable variant entity (configuration, storage, color).
--   - Holds globally unique SKU, retail price (> 0), and discount percentage (0 - 100).
--   - Dynamic JSONB attributes: e.g. {"color": "Natural Titanium", "storage": "256GB"} or {"ram": "16GB", "bus": "3200MHz"}.
--   - Inventory, shopping carts, and order items link directly to variant_id.
--   - Status lifecycle: ACTIVE, INACTIVE, DISCONTINUED.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(50) NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    price DECIMAL(15, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT chk_product_variants_price CHECK (price > 0),
    CONSTRAINT chk_product_variants_discount CHECK (discount_percent >= 0.00 AND discount_percent <= 100.00),
    CONSTRAINT chk_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_variants_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_product_variants_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 8: product_images
-- PURPOSE & BUSINESS LOGIC:
--   - Media collection gallery for parent products.
--   - display_order controls gallery display sequence; is_primary indicates primary showcase thumbnail.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_images_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_product_images_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 9: inventory
-- PURPOSE & BUSINESS LOGIC:
--   - Multi-branch inventory tracking per product variant (Unique pair: branch_id + variant_id).
--   - quantity_on_hand: Actual physical units present in the warehouse.
--   - quantity_reserved: Committed units currently reserved for PENDING customer orders.
--   - Critical Invariant: quantity_reserved <= quantity_on_hand.
--   - Available stock formula: quantity_available = quantity_on_hand - quantity_reserved (Always >= 0).
--   - version: Optimistic lock version counter.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 5,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_inventory_branch_variant UNIQUE (branch_id, variant_id),
    CONSTRAINT chk_inventory_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reserved CHECK (quantity_reserved >= 0),
    CONSTRAINT chk_inventory_reserved_lte_hand CHECK (quantity_reserved <= quantity_on_hand),
    CONSTRAINT chk_inventory_reorder_level CHECK (reorder_level >= 0),
    CONSTRAINT fk_inventory_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 10: inventory_history
-- PURPOSE & BUSINESS LOGIC:
--   - Append-Only immutable audit log for internal inventory auditing and reconciliation.
--   - Change types: STOCK_IN, STOCK_OUT, ADJUSTMENT, RESERVE, RELEASE, SALE_DEDUCT.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_history (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    quantity_change INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    reference_type VARCHAR(30),
    reference_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_inv_history_change_type CHECK (
        change_type IN ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RESERVE', 'RELEASE', 'SALE_DEDUCT')
    ),
    CONSTRAINT fk_inv_history_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inv_history_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inv_history_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_inv_history_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 11: carts
-- PURPOSE & BUSINESS LOGIC:
--   - Persistent database-backed shopping cart with 1:1 relationship per customer (customer_id UNIQUE).
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_carts_customer UNIQUE (customer_id),
    CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_carts_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_carts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 12: cart_items
-- PURPOSE & BUSINESS LOGIC:
--   - Items placed inside customer carts linked to product_variants.
--   - Invariant: Does NOT store static price. Price is dynamically loaded from variant at inquiry time.
--   - Constraint: Each variant appears at most once per cart (UNIQUE cart_id + variant_id).
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, variant_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_cart_items_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 13: orders
-- PURPOSE & BUSINESS LOGIC:
--   - Immutable commercial sales orders. Never physically deleted.
--   - Snapshot Pattern: Freezes snapshot of shipping address text and customer contact at checkout time.
--   - Single-Direction State Machine: PENDING -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED / CANCELLED.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    customer_id UUID NOT NULL,
    branch_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(15, 2) NOT NULL,
    shipping_address TEXT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    note TEXT,
    cancelled_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')
    ),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 14: order_items
-- PURPOSE & BUSINESS LOGIC:
--   - Immutable line-item financial snapshots: Freezes product_name, variant_sku, variant_attributes (JSONB),
--     unit_price, and discount_percent as of order creation time.
--   - Ensures historical accounting consistency when future catalog prices or product specs change.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    variant_id UUID,
    product_name VARCHAR(255) NOT NULL,
    variant_sku VARCHAR(50) NOT NULL,
    variant_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    unit_price DECIMAL(15, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 1,
    subtotal DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_order_items_discount CHECK (discount_percent >= 0.00 AND discount_percent <= 100.00),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL,
    CONSTRAINT fk_order_items_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_order_items_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 15: idempotency_keys
-- PURPOSE & BUSINESS LOGIC:
--   - Protects mission-critical order placement transactions against duplicate execution caused by client double-clicks
--     or automated network retries.
--   - Co-located in PostgreSQL within the same ACID transaction boundary as orders and inventory reservations.
--   - Lifecycle Statuses:
--       * 'IN_PROGRESS': Processing ongoing; concurrent duplicates immediately rejected with 409 Conflict.
--       * 'COMPLETED': Successfully finished; replay requests with matching SHA-256 payload return cached response snapshot.
--       * 'FAILED': Execution encountered fatal error; allow safe retries.
--   - Enforces 24-hour expiration (expires_at) to ensure clean temporal state.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    response_status INTEGER,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_idempotency_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------------------------------
-- TABLE 16: banners
-- PURPOSE & BUSINESS LOGIC:
--   - Promotional banners displayed on the homepage and landing pages.
--   - Managed by STAFF / ADMIN, publicly queryable for active items.
--   - display_order controls sequence (ASC), followed by created_at DESC.
-- ----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS banners (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    target_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_banners_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_banners_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ====================================================================================================
-- PART 3: PERFORMANCE INDEXES (B-TREE, GIN JSONB, GIN TRIGRAM)
-- ====================================================================================================

-- 1. Indexes on 'users'
-- Optimizes active account lookup during authentication while allowing soft-deleted email reuse.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_active 
    ON users (email) 
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_role 
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_created_at 
    ON users (created_at);

-- GIN index for fast query and element inspection inside user_address_info JSONB array.
CREATE INDEX IF NOT EXISTS idx_users_address_info 
    ON users USING gin (user_address_info);

-- 2. Indexes on 'refresh_tokens'
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash 
    ON refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked 
    ON refresh_tokens (user_id, revoked);

-- 3. Indexes on 'categories'
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_parent_name 
    ON categories (COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), name);

CREATE INDEX IF NOT EXISTS idx_categories_parent_id 
    ON categories (parent_id);

-- Optimizes subtree querying using LIKE '/parent-path/%' via Materialized Path.
CREATE INDEX IF NOT EXISTS idx_categories_path 
    ON categories (path);

CREATE INDEX IF NOT EXISTS idx_categories_active 
    ON categories (active);

-- 4. Indexes on 'brands' & 'branches'
CREATE INDEX IF NOT EXISTS idx_brands_active 
    ON brands (active);

CREATE INDEX IF NOT EXISTS idx_branches_active 
    ON branches (active);

-- 5. Indexes on 'products'
CREATE INDEX IF NOT EXISTS idx_products_status_category 
    ON products (status, category_id);

CREATE INDEX IF NOT EXISTS idx_products_status_brand 
    ON products (status, brand_id);

-- GIN Trigram index: Powers fuzzy and substring search under 30ms across millions of records.
CREATE INDEX IF NOT EXISTS idx_products_name_trgm 
    ON products USING gin (name gin_trgm_ops);

-- 6. Indexes on 'product_variants'
CREATE INDEX IF NOT EXISTS idx_product_variants_product_status 
    ON product_variants (product_id, status);

-- GIN index for filtering products by arbitrary dynamic attributes (e.g. RAM, storage, color).
CREATE INDEX IF NOT EXISTS idx_product_variants_attributes 
    ON product_variants USING gin (attributes);

-- 7. Indexes on 'product_images'
CREATE INDEX IF NOT EXISTS idx_product_images_product_primary 
    ON product_images (product_id, is_primary);

-- 8. Indexes on 'inventory' & 'inventory_history'
CREATE INDEX IF NOT EXISTS idx_inventory_branch_variant 
    ON inventory (branch_id, variant_id);

CREATE INDEX IF NOT EXISTS idx_inventory_variant_id 
    ON inventory (variant_id);

CREATE INDEX IF NOT EXISTS idx_inventory_branch_low_stock 
    ON inventory (branch_id, (quantity_on_hand - quantity_reserved));

CREATE INDEX IF NOT EXISTS idx_inv_history_branch_variant 
    ON inventory_history (branch_id, variant_id, created_at DESC);

-- 9. Indexes on 'cart_items'
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id 
    ON cart_items (cart_id);

-- 10. Indexes on 'orders' & 'order_items'
CREATE INDEX IF NOT EXISTS idx_orders_customer_created 
    ON orders (customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_created 
    ON orders (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_branch_status 
    ON orders (branch_id, status);

CREATE INDEX IF NOT EXISTS idx_orders_created_at 
    ON orders (created_at);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id 
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_variant_id 
    ON order_items (variant_id);

-- 11. Indexes on 'idempotency_keys'
CREATE INDEX IF NOT EXISTS idx_idempotency_lookup 
    ON idempotency_keys (idempotency_key, user_id);

CREATE INDEX IF NOT EXISTS idx_idempotency_expiry 
    ON idempotency_keys (expires_at);

-- 12. Indexes on 'banners'
CREATE INDEX IF NOT EXISTS idx_banners_active_order_created 
    ON banners (active, display_order ASC, created_at DESC);


