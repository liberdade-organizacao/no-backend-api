# Scripting Engine Integration Tests

Optional integration tests that exercise the scripting engine via the BAAS API
`/actions/run` and `/health` surfaces. Default-off: the suite is unreachable from
`lein test` until a maintainer uncomments it. The engine is a real external
`no-backend-scripting-engine` reached through `SCRIPTING_ENGINE_URL`.

Context:
- Engine is reached only via `POST /actions/run` (`api.clj:367`/`452` → `proxies.clj:8`)
  and via the `scripting` field of `GET /health` (`api.clj:61` → `proxies.clj:25`).
- `proxies/scripting-engine-url` (`proxies.clj:6`) is a plain `def` evaluated once at
  namespace load from `SCRIPTING_ENGINE_URL` (default `http://localhost:7781`). The env var
  must be set before `lein` loads the `proxies` namespace.
- `/actions/run` returns a 2-field object `{result, error}`.
- No test currently exercises a real engine call.

## Action items

### 1. Gate — `#_`-commented `deftest`, off by default
- [ ] Create `test/br/bsb/liberdade/baas/scripting_engine_test.clj`.
- [ ] Prefix the suite's entry `deftest` with `#_` so it registers only when uncommented:
  ```clojure
  #_(deftest run-action-endpoint
        (use-fixtures :each th/scripting-engine-fixture)
        (testing "…")
         <body>)
  ```
- [ ] Confirm default `lein test` loads the namespace but never registers the commented `deftest`.

### 2. Helpers in `test/br/bsb/liberdade/baas/test_helpers.clj`
- [ ] Add `run-action` wrapper, mirroring existing wrappers (lines 154–306), POSTing to
  `/actions/run` with `user_auth_key`/`app_auth_key`/`action_name`/`action_param`.
- [ ] Add `scripting-engine-reachable?` — short-timeout `http/get
  (str proxies/scripting-engine-url "/health")`, catch → `false`.
- [ ] Add `scripting-engine-fixture [test-fn]` extending `integration-fixture`
  (lines 69–82): setup sqlite + start BAAS server, then call `scripting-engine-reachable?`;
  if the engine is down, throw a clear `ex-info` instead of hanging on the `"KO"` fallback.

### 3. Test cases in the new file (reuse `th/*base-url*` in-process server)
- [ ] Happy path: `signup-client` → `create-app` → `signup-user` → `create-action`
      (persist script) → `th/run-action`. Assert response is 200, `:result` present, `:error` nil.
- [ ] Health reporting: `GET /health` and assert `(:scripting …)` equals the engine's live
      `/health` body (proves `api.clj:61` → `proxies.clj:25` wiring).
- [ ] Failure path: run with a nonexistent `action_name`; assert `:error`
      is present and `:result` nil, and `check-scripting-engine-health` returns `"KO"`.

### 4. Documentation
- [ ] Add to `README.md` "Usage": engine must be running at `SCRIPTING_ENGINE_URL`; to enable
      the suite, uncomment the `deftest` in `scripting_engine_test.clj` and run
      `SCRIPTING_ENGINE_URL=<url> lein test`. No `make` target (opt-in is manual uncomment only).

### 5. Verify
- [ ] `make lint` (cljfmt) passes on the new/edited files.
- [ ] Default `make test` passes with no engine running and the suite uncollected.
- [ ] With engine up + uncommented + `SCRIPTING_ENGINE_URL` set, all 3 cases pass.

## Open dependency
- `proxies/run-action` sends only `user_id`/`app_id`/`action_name`/`action_param`
  (`proxies.clj:13–16`) and no script, so the engine must fetch the script out-of-band from
  the shared DB. Write happy-path assertions against the `{result, error}` shape and flag the
  fetch contract for review.
