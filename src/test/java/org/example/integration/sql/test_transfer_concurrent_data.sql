DELETE FROM idempotency_keys;
DELETE FROM transactions;
DELETE FROM account_balance_audit;
DELETE FROM accounts;
DELETE FROM audit_log;
DELETE FROM users;

INSERT INTO users (id, email, first_name, last_name, password_hash, created_at, version)
VALUES
(1, 'test_1@example.com', 'Test_1', 'User', 'hash', now(), 0),
(2, 'test_2@example.com', 'Test_2', 'User', 'hash', now(), 0),
(3, 'test_3@example.com', 'Test_3', 'User', 'hash', now(), 0);

INSERT INTO accounts (id, user_id, account_number, balance, currency_code, account_type, opening_date, status, version)
VALUES
(1, 1, 'ACNT-001', 1000.00, 0, 0, now(), 'ACTIVE', 0),
(2, 2, 'ACNT-002', 500.00, 0, 0, now(), 'ACTIVE', 0),
(3, 3, 'ACNT-003', 400.00, 0, 0, now(), 'ACTIVE', 0);