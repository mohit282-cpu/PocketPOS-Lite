-- PocketPOS Lite: Supabase PostgreSQL Schema
-- Multi-tenant POS application schema with Row Level Security (RLS)
-- Idempotent script: Safe to run multiple times

-- 1. Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Profiles Table (Extends Supabase Auth users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    full_name TEXT,
    avatar_url TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Helper to safely create policies for profiles
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Public profiles are viewable by everyone') THEN
        CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles FOR SELECT USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can insert their own profile') THEN
        CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can update own profile') THEN
        CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
    END IF;
END $$;

-- 3. Businesses Table
CREATE TABLE IF NOT EXISTS public.businesses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    owner_id UUID REFERENCES public.profiles(id) NOT NULL,
    address TEXT,
    phone TEXT,
    email TEXT,
    logo_url TEXT,
    currency TEXT NOT NULL DEFAULT 'USD',
    invoice_prefix TEXT NOT NULL DEFAULT 'INV',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.businesses ENABLE ROW LEVEL SECURITY;

-- 4. Business Users (Membership & Roles)
CREATE TABLE IF NOT EXISTS public.business_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('owner', 'admin', 'staff')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    UNIQUE(business_id, user_id)
);

ALTER TABLE public.business_users ENABLE ROW LEVEL SECURITY;

-- Security Helper Functions (With SET search_path for safety and recursion breaking)
CREATE OR REPLACE FUNCTION public.get_my_business_ids()
RETURNS SETOF UUID AS $$
    -- This function runs with security definer to bypass RLS loops
    SELECT business_id FROM public.business_users WHERE user_id = auth.uid();
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

-- Policies for Businesses and Business Users
DO $$
BEGIN
    -- Cleanup potential old policy names
    DROP POLICY IF EXISTS "view_own_membership" ON public.business_users;
    DROP POLICY IF EXISTS "admins_view_members" ON public.business_users;
    DROP POLICY IF EXISTS "view_my_businesses" ON public.businesses;
    DROP POLICY IF EXISTS "owners_update_business" ON public.businesses;
    DROP POLICY IF EXISTS "business_select_policy" ON public.businesses;
    DROP POLICY IF EXISTS "business_update_policy" ON public.businesses;
    DROP POLICY IF EXISTS "membership_select_policy" ON public.business_users;
END $$;

-- For business_users: A user can see their own membership row or rows where they are owner/admin
CREATE POLICY "membership_select_policy" ON public.business_users
    FOR SELECT USING (
        auth.uid() = user_id OR
        business_id IN (SELECT id FROM public.businesses WHERE owner_id = auth.uid())
    );

-- For businesses: Users can see businesses they belong to
CREATE POLICY "business_select_policy" ON public.businesses
    FOR SELECT USING (
        owner_id = auth.uid() OR
        id IN (SELECT public.get_my_business_ids())
    );

-- Only owners can update their business
CREATE POLICY "business_update_policy" ON public.businesses
    FOR UPDATE USING (owner_id = auth.uid());

-- Trigger: Create profile and business on signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
    v_business_id UUID;
BEGIN
  -- 1. Create Profile
  INSERT INTO public.profiles (id, full_name)
  VALUES (new.id, new.raw_user_meta_data->>'full_name')
  ON CONFLICT (id) DO NOTHING;

  -- 2. Create Business (if business_name is provided in metadata)
  IF new.raw_user_meta_data->>'business_name' IS NOT NULL THEN
      INSERT INTO public.businesses (name, owner_id, phone)
      VALUES (
          new.raw_user_meta_data->>'business_name',
          new.id,
          new.raw_user_meta_data->>'phone'
      )
      RETURNING id INTO v_business_id;

      -- 3. Create Membership
      INSERT INTO public.business_users (business_id, user_id, role)
      VALUES (v_business_id, new.id, 'owner');
  END IF;

  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Safe Trigger creation
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'on_auth_user_created') THEN
        CREATE TRIGGER on_auth_user_created
        AFTER INSERT ON auth.users
        FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
    END IF;
END $$;

-- 5. Categories
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    UNIQUE(business_id, name)
);

ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_categories') THEN
        CREATE POLICY "tenant_isolation_categories" ON public.categories
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 6. Products
CREATE TABLE IF NOT EXISTS public.products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    description TEXT,
    sku TEXT,
    barcode TEXT,
    price DECIMAL(12,2) NOT NULL DEFAULT 0,
    cost_price DECIMAL(12,2) DEFAULT 0,
    stock_quantity DECIMAL(12,2) DEFAULT 0,
    min_stock DECIMAL(12,2) DEFAULT 0,
    unit TEXT DEFAULT 'pcs',
    image_url TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    UNIQUE(business_id, sku)
);

ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_products') THEN
        CREATE POLICY "tenant_isolation_products" ON public.products
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 7. Customers
CREATE TABLE IF NOT EXISTS public.customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_customers') THEN
        CREATE POLICY "tenant_isolation_customers" ON public.customers
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 8. Sales (Transactions)
CREATE TABLE IF NOT EXISTS public.sales (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    cashier_id UUID REFERENCES public.profiles(id) NOT NULL,
    customer_id UUID REFERENCES public.customers(id) ON DELETE SET NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    tax_amount DECIMAL(12,2) DEFAULT 0,
    net_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status TEXT NOT NULL CHECK (status IN ('completed', 'pending', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.sales ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_sales') THEN
        CREATE POLICY "tenant_isolation_sales" ON public.sales
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 9. Sale Items
CREATE TABLE IF NOT EXISTS public.sale_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    sale_id UUID REFERENCES public.sales(id) ON DELETE CASCADE NOT NULL,
    product_id UUID REFERENCES public.products(id) ON DELETE RESTRICT NOT NULL,
    quantity DECIMAL(12,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.sale_items ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_sale_items') THEN
        CREATE POLICY "tenant_isolation_sale_items" ON public.sale_items
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 10. Payments
CREATE TABLE IF NOT EXISTS public.payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id UUID REFERENCES public.sales(id) ON DELETE CASCADE NOT NULL,
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('cash', 'bank', 'qr', 'card', 'online', 'credit', 'other')),
    status TEXT NOT NULL CHECK (status IN ('success', 'failed', 'pending')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_payments') THEN
        CREATE POLICY "tenant_isolation_payments" ON public.payments
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 11. Expenses
CREATE TABLE IF NOT EXISTS public.expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    description TEXT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category TEXT,
    expense_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_expenses') THEN
        CREATE POLICY "tenant_isolation_expenses" ON public.expenses
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 12. Inventory Movements
CREATE TABLE IF NOT EXISTS public.inventory_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID REFERENCES public.products(id) ON DELETE CASCADE NOT NULL,
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE NOT NULL,
    quantity_change DECIMAL(12,2) NOT NULL,
    movement_type TEXT NOT NULL CHECK (movement_type IN ('in', 'out', 'adjustment', 'sale')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.inventory_movements ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'tenant_isolation_inventory_movements') THEN
        CREATE POLICY "tenant_isolation_inventory_movements" ON public.inventory_movements
        FOR ALL USING (business_id IN (SELECT public.get_my_business_ids()));
    END IF;
END $$;

-- 13. Atomic Sale Creation Function
CREATE OR REPLACE FUNCTION public.create_sale(
    p_business_id UUID,
    p_cashier_id UUID,
    p_customer_id UUID,
    p_total_amount DECIMAL,
    p_discount_amount DECIMAL,
    p_tax_amount DECIMAL,
    p_net_amount DECIMAL,
    p_payment_method TEXT,
    p_items JSONB
) RETURNS UUID AS $$
DECLARE
    v_sale_id UUID;
    v_item JSONB;
BEGIN
    -- 1. Create Sale Header
    INSERT INTO public.sales (business_id, cashier_id, customer_id, total_amount, discount_amount, tax_amount, net_amount, status)
    VALUES (p_business_id, p_cashier_id, p_customer_id, p_total_amount, p_discount_amount, p_tax_amount, p_net_amount, 'completed')
    RETURNING id INTO v_sale_id;

    -- 2. Process Items
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        -- Insert Line Item (Now including business_id explicitly)
        INSERT INTO public.sale_items (business_id, sale_id, product_id, quantity, unit_price, subtotal)
        VALUES (p_business_id, v_sale_id, (v_item->>'product_id')::UUID, (v_item->>'quantity')::DECIMAL, (v_item->>'unit_price')::DECIMAL, (v_item->>'subtotal')::DECIMAL);

        -- Update Product Stock (with check for negative inventory)
        UPDATE public.products
        SET stock_quantity = stock_quantity - (v_item->>'quantity')::DECIMAL
        WHERE id = (v_item->>'product_id')::UUID AND business_id = p_business_id AND stock_quantity >= (v_item->>'quantity')::DECIMAL;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Insufficient stock or invalid product %', (v_item->>'product_id');
        END IF;

        -- Record Inventory Log
        INSERT INTO public.inventory_movements (product_id, business_id, quantity_change, movement_type, notes)
        VALUES ((v_item->>'product_id')::UUID, p_business_id, -(v_item->>'quantity')::DECIMAL, 'sale', 'Sale #' || v_sale_id);
    END LOOP;

    -- 3. Create Payment Record
    INSERT INTO public.payments (sale_id, business_id, amount, payment_method, status)
    VALUES (v_sale_id, p_business_id, p_net_amount, p_payment_method, 'success');

    RETURN v_sale_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 14. Indexes for Performance (Safe creation)
CREATE INDEX IF NOT EXISTS idx_products_business_id ON public.products(business_id);
CREATE INDEX IF NOT EXISTS idx_sales_business_id ON public.sales(business_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_product_id ON public.inventory_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON public.sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_business_id ON public.sale_items(business_id);
CREATE INDEX IF NOT EXISTS idx_business_users_user_id ON public.business_users(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_sale_id ON public.payments(sale_id);
CREATE INDEX IF NOT EXISTS idx_expenses_business_id ON public.expenses(business_id);
