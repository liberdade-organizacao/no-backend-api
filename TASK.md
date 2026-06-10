The goal of the current task is to rewrite the tests to act more like behavioural tests
instead of unit tests for particular functions. Please review the suite of tests for the
business logic on `test/br/bsb/liberdade/baas/business_test.clj` and the application entrypoint
at `src/br/bsb/liberdade/baas/api.clj`. Then, write a plan at `TODO.md` to write a new suite of
tests that will start the application in a background thread, make the necessary API calls to
the application, and verify if the responses from the API and the database state is consistent
after the test. Please take your time reading through the code and evaluating possible
solutions to this problem.

