-- Тёплая тема оформления "Ruomi" (вариант 2: бирюзово-коралловый)
-- Обновляем существующую строку темы (id = 1), если она есть.
UPDATE site_theme
SET primary_color       = '#0E7C66',
    secondary_color     = '#6B7C75',
    success_color       = '#2E9E6B',
    danger_color        = '#E0573F',
    warning_color       = '#E0A23B',
    info_color          = '#2A9D8F',
    border_radius       = '0.85rem',
    container_max_width = '1200px',
    hero_gradient_start = '#0E7C66',
    hero_gradient_end   = '#2E9E6B'
WHERE id = 1;

-- Если строки темы ещё нет (свежая база) — создаём её с тёплой палитрой.
INSERT INTO site_theme (
    id, primary_color, secondary_color, success_color, danger_color,
    warning_color, info_color, base_font_size, heading_font_size,
    small_font_size, large_font_size, border_radius, box_shadow,
    navbar_height, container_max_width, hero_gradient_start, hero_gradient_end
)
SELECT 1, '#0E7C66', '#6B7C75', '#2E9E6B', '#E0573F',
       '#E0A23B', '#2A9D8F', '16px', '2rem',
       '0.875rem', '1.25rem', '0.85rem', '0 0.125rem 0.5rem rgba(31, 61, 53, 0.08)',
       '68px', '1200px', '#0E7C66', '#2E9E6B'
WHERE NOT EXISTS (SELECT 1 FROM site_theme WHERE id = 1);
