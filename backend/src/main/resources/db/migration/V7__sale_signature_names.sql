-- Sales invoice signature fields: cashier display name + received by

ALTER TABLE sales
    ADD COLUMN cashier_name VARCHAR(150),
    ADD COLUMN received_by VARCHAR(150);

UPDATE sales s
SET cashier_name = TRIM(CONCAT(u.first_name, ' ', u.last_name))
FROM users u
WHERE u.username = s.cashier_username
  AND u.deleted_at IS NULL
  AND s.cashier_name IS NULL;
