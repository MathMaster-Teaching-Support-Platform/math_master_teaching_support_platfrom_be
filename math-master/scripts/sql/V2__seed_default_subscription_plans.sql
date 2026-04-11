-- =============================================================
-- Seed: 3 default subscription plans
-- Matches Pricing.tsx: Miễn phí | Giáo viên | Trường học
-- Idempotent: ON CONFLICT (slug) DO NOTHING
-- =============================================================

BEGIN;

DO $$
DECLARE
  v_now TIMESTAMPTZ := NOW();

  -- Fixed UUIDv7-like IDs for idempotency
  v_plan_free       UUID := '018f2b00-0000-7000-8000-000000000001';
  v_plan_teacher    UUID := '018f2b00-0000-7000-8000-000000000002';
  v_plan_enterprise UUID := '018f2b00-0000-7000-8000-000000000003';
BEGIN

  -- ---------------------------------------------------------------
  -- Plan 1: Miễn phí
  -- ---------------------------------------------------------------
  INSERT INTO subscription_plans (
    plan_id, name, slug, price, currency, billing_cycle,
    description, is_featured, is_public, status, features,
    created_at, updated_at
  ) VALUES (
    v_plan_free,
    'Miễn phí',
    'mien-phi',
    0,
    'VND',
    'FOREVER',
    'Phù hợp để trải nghiệm nền tảng',
    FALSE,
    TRUE,
    'ACTIVE',
    '["Tạo tối đa 10 bài giảng/tháng", "Lưu trữ 100MB", "Quản lý 1 lớp học", "AI trợ lý cơ bản", "Hỗ trợ email"]'::jsonb,
    v_now,
    v_now
  )
  ON CONFLICT (slug) DO NOTHING;

  -- ---------------------------------------------------------------
  -- Plan 2: Giáo viên  (isFeatured = true → "⭐ Phổ biến nhất")
  -- ---------------------------------------------------------------
  INSERT INTO subscription_plans (
    plan_id, name, slug, price, currency, billing_cycle,
    description, is_featured, is_public, status, features,
    created_at, updated_at
  ) VALUES (
    v_plan_teacher,
    'Giáo viên',
    'giao-vien',
    199000,
    'VND',
    'MONTH',
    'Dành cho giáo viên cá nhân muốn dạy không giới hạn',
    TRUE,
    TRUE,
    'ACTIVE',
    '["Tạo không giới hạn bài giảng", "Lưu trữ 10GB", "Quản lý không giới hạn lớp học", "AI trợ lý nâng cao", "Thư viện tài liệu cao cấp", "Xuất file không watermark", "Hỗ trợ ưu tiên"]'::jsonb,
    v_now,
    v_now
  )
  ON CONFLICT (slug) DO NOTHING;

  -- ---------------------------------------------------------------
  -- Plan 3: Trường học  (price = NULL → "Liên hệ")
  -- ---------------------------------------------------------------
  INSERT INTO subscription_plans (
    plan_id, name, slug, price, currency, billing_cycle,
    description, is_featured, is_public, status, features,
    created_at, updated_at
  ) VALUES (
    v_plan_enterprise,
    'Trường học',
    'truong-hoc',
    NULL,
    'VND',
    'CUSTOM',
    'Giải pháp toàn diện cho tổ chức giáo dục',
    FALSE,
    TRUE,
    'ACTIVE',
    '["Tất cả tính năng gói Giáo viên", "Không giới hạn tài khoản", "Lưu trữ không giới hạn", "Dashboard quản trị tập trung", "API tích hợp", "Đào tạo và hỗ trợ chuyên biệt", "Tùy chỉnh theo yêu cầu"]'::jsonb,
    v_now,
    v_now
  )
  ON CONFLICT (slug) DO NOTHING;

  RAISE NOTICE 'Seed completed: % plan(s) inserted or already existed.',
    (SELECT COUNT(*) FROM subscription_plans
     WHERE plan_id IN (v_plan_free, v_plan_teacher, v_plan_enterprise));
END $$;

COMMIT;
