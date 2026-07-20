# Database Schema Layout

## Tables

### `clients`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `email` | VARCHAR(32) | UNIQUE NOT NULL |
| `password` | VARCHAR(128) | NOT NULL |
| `is_admin` | BOOLEAN | NOT NULL DEFAULT false |
| `created_at` | TIMESTAMP WITH TIME ZONE | DEFAULT current_timestamp NOT NULL |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | DEFAULT current_timestamp NOT NULL |

### `apps`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `owner_id` | INTEGER | REFERENCES `clients(id)` ON DELETE CASCADE NOT NULL |
| `name` | VARCHAR(32) | NOT NULL |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |

*Constraint*: UNIQUE(`owner_id`, `name`)

### `users`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `app_id` | INT | REFERENCES `apps(id)` ON DELETE CASCADE NOT NULL |
| `email` | VARCHAR(32) | NOT NULL |
| `password` | VARCHAR(128) | NOT NULL |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |

*Constraint*: UNIQUE(`app_id`, `email`)

### `files`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `filename` | VARCHAR(64) | NOT NULL |
| `filepath` | VARCHAR(128) | UNIQUE NOT NULL |
| `file_size` | INTEGER | |
| `created_at` | TIMESTAMP WITH time ZONE | NOT NULL DEFAULT current_timestamp |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |
| `app_id` | INTEGER | REFERENCES `apps(id)` ON DELETE CASCADE NOT NULL |
| `owner_id` | INTEGER | REFERENCES `users(id)` ON DELETE CASCADE |

### `actions`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `app_id` | INTEGER | REFERENCES `apps(id)` ON DELETE CASCADE NOT NULL |
| `name` | VARCHAR(32) | NOT NULL |
| `script` | TEXT | |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |

*Constraint*: UNIQUE(`app_id`, `name`)

### `app_memberships`
| Column | Type | Constraints |
| --- | --- | --- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT NOT NULL |
| `app_id` | INTEGER | REFERENCES `apps(id)` ON DELETE CASCADE NOT NULL |
| `client_id` | INTEGER | REFERENCES `clients(id)` ON DELETE CASCADE NOT NULL |
| `role` | VARCHAR(32) | NOT NULL |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |
| `last_updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT current_timestamp |

## Triggers

The following triggers automatically update the `last_updated_at` timestamp on each corresponding table during an UPDATE operation:

- `update_clients_timestamp` on `clients`
- `update_apps_timestamp` on `apps`
- `update_users_timestamp` on `users`
- `update_files_timestamp` on `files`
- `update_actions_timestamp` on `actions`
- `update_app_memberships_timestamp` on `app_memberships`
