-- name: AddHistoryEntry :exec
INSERT INTO history (url) VALUES (?);

-- name: GetHistoryLines :many
SELECT * FROM history ORDER BY accessed DESC;
