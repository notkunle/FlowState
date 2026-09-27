-- focus decisions per hour per session, kept up to date by Timescale
-- (grouped by session_id because continuous aggregates can't do COUNT(DISTINCT))
CREATE MATERIALIZED VIEW hourly_focus
WITH (timescaledb.continuous, timescaledb.materialized_only = false) AS
SELECT time_bucket('1 hour', decided_at)       AS bucket,
       session_id,
       count(*) FILTER (WHERE state = 'focus') AS focus_count,
       count(*)                                AS decision_count
FROM state_decision
GROUP BY bucket, session_id
WITH NO DATA;

-- materialized_only = false: the newest, not-yet-refreshed hours are computed on the fly
SELECT add_continuous_aggregate_policy('hourly_focus',
       start_offset      => INTERVAL '30 days',
       end_offset        => INTERVAL '1 hour',
       schedule_interval => INTERVAL '30 minutes');
