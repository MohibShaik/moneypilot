-- V3__seed_system_categories.sql
-- Seeds shared system/default categories (user_id IS NULL).
--
-- The V2 categories table was designed for this: user_id is nullable and a
-- partial unique index uq_categories_system_name enforces uniqueness of system
-- category names. These rows are visible to every user (CategoryRepository
-- .findVisibleForUser returns user_id IS NULL) and are read-only to users.
--
-- Forward-only and safe to re-run against a partially seeded table: the
-- INSERT ... ON CONFLICT DO NOTHING no-ops on names that already exist as
-- system categories. id/created_at/updated_at use their column defaults.

INSERT INTO categories (name, category_type)
VALUES
    ('Food & Dining', 'EXPENSE'),
    ('Groceries',     'EXPENSE'),
    ('Transport',     'EXPENSE'),
    ('Shopping',      'EXPENSE'),
    ('Bills & Utilities', 'EXPENSE'),
    ('Health',        'EXPENSE'),
    ('Entertainment', 'EXPENSE'),
    ('Education',     'EXPENSE'),
    ('Travel',        'EXPENSE'),
    ('Other',         'EXPENSE'),
    ('Salary',        'INCOME'),
    ('Interest',      'INCOME'),
    ('Other Income',  'INCOME')
ON CONFLICT DO NOTHING;
