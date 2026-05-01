-- Add HLS transcoding columns to course_lessons
-- hlsStatus tracks the FFmpeg pipeline lifecycle (PENDING → PROCESSING → READY | FAILED)
-- hlsPlaylistKey stores the MinIO object key for the generated playlist.m3u8

ALTER TABLE course_lessons
    ADD COLUMN IF NOT EXISTS hls_status     VARCHAR(20)  DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS hls_playlist_key VARCHAR(512);

-- Existing rows default to PENDING so they qualify for lazy transcoding if needed
-- (or the fallback .mp4 presign path will serve them until they are re-uploaded)
