DELETE FROM files WHERE filepath='%{filepath}' RETURNING *;
