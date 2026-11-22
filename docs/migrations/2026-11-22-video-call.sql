-- Apply once to an existing master-branch MySQL schema before starting video-call.
-- Review first if Hibernate has already added any of these columns locally.
-- The old Hibernate-generated enum only admits STROKE, ERASE and CLEAR.
-- VARCHAR preserves old values and admits SHAPE and TEXT operations.
ALTER TABLE board_operation MODIFY COLUMN type VARCHAR(16) NOT NULL;
ALTER TABLE board_operation MODIFY COLUMN points_json TEXT NULL;
ALTER TABLE board_publication MODIFY COLUMN event_json TEXT NOT NULL;
ALTER TABLE board_snapshot MODIFY COLUMN strokes_json LONGTEXT NOT NULL;

-- Hibernate ddl-auto=update can add these columns locally, but production
-- deployments should run explicit migrations and disable automatic DDL.
ALTER TABLE board_operation ADD COLUMN shape VARCHAR(16) NULL;
ALTER TABLE board_operation ADD COLUMN board_text TEXT NULL;

CREATE TABLE video_call (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    started_by VARCHAR(255) NOT NULL,
    CONSTRAINT fk_video_call_room FOREIGN KEY (room_id) REFERENCES learning_room(id),
    INDEX idx_video_call_room_started (room_id, started_at)
);

CREATE TABLE video_call_note (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    call_id BIGINT NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_video_call_note_call FOREIGN KEY (call_id) REFERENCES video_call(id),
    CONSTRAINT uk_call_note_author_request UNIQUE (call_id, author_id, request_id)
);

ALTER TABLE ai_session_job ADD COLUMN call_id BIGINT NULL;
ALTER TABLE ai_session_job ADD CONSTRAINT fk_ai_session_job_call
    FOREIGN KEY (call_id) REFERENCES video_call(id);
CREATE INDEX idx_ai_job_call_id ON ai_session_job(call_id, id);
