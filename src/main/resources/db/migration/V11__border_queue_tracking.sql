CREATE TABLE IF NOT EXISTS border_queue_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checkpoint VARCHAR(32) NOT NULL,
    lane VARCHAR(16) NOT NULL,
    live_count INT NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    INDEX idx_border_snap_checkpoint_lane_time (checkpoint, lane, captured_at)
);

CREATE TABLE IF NOT EXISTS border_queue_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checkpoint VARCHAR(32) NOT NULL,
    lane VARCHAR(16) NOT NULL,
    event_type VARCHAR(16) NOT NULL,
    delta INT NOT NULL,
    previous_count INT NOT NULL,
    new_count INT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    INDEX idx_border_evt_checkpoint_time (checkpoint, recorded_at),
    INDEX idx_border_evt_type_time (event_type, recorded_at)
);
