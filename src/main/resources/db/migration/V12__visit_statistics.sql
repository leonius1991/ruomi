CREATE TABLE IF NOT EXISTS visit_statistics (
    id TINYINT NOT NULL PRIMARY KEY,
    total_visits BIGINT NOT NULL DEFAULT 0,
    today_visits BIGINT NOT NULL DEFAULT 0,
    stat_date DATE NOT NULL
);

INSERT INTO visit_statistics (id, total_visits, today_visits, stat_date)
VALUES (1, 0, 0, CURDATE())
ON DUPLICATE KEY UPDATE id = id;
