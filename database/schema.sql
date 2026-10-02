-- =====================================================================
--  DigiQ - Digital Queue Management System
--  MySQL 8.x schema + seed data
--
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS digiq;
CREATE DATABASE digiq DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE digiq;

-- ---------------------------------------------------------------------
--  users : one table for all three roles
-- ---------------------------------------------------------------------
CREATE TABLE users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(120)  NOT NULL,
  email         VARCHAR(150)  NOT NULL UNIQUE,
  phone         VARCHAR(30),
  password_hash VARCHAR(100)  NOT NULL,
  role          ENUM('ADMIN','STAFF','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
  active        TINYINT(1)    NOT NULL DEFAULT 1,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  services : what a customer can queue for
-- ---------------------------------------------------------------------
CREATE TABLE services (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  name                VARCHAR(120) NOT NULL,
  code                VARCHAR(8)   NOT NULL UNIQUE,
  description         VARCHAR(255),
  avg_service_minutes INT NOT NULL DEFAULT 10,
  active              TINYINT(1) NOT NULL DEFAULT 1,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  counters : physical service points, each bound to one service
-- ---------------------------------------------------------------------
CREATE TABLE counters (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(60) NOT NULL,
  service_id INT NOT NULL,
  staff_id   INT NULL,
  status     ENUM('OPEN','PAUSED','CLOSED') NOT NULL DEFAULT 'CLOSED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_counter_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
  CONSTRAINT fk_counter_staff   FOREIGN KEY (staff_id)   REFERENCES users(id)    ON DELETE SET NULL,
  INDEX idx_counter_service (service_id),
  INDEX idx_counter_staff (staff_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  tokens : the queue itself
-- ---------------------------------------------------------------------
CREATE TABLE tokens (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  token_number VARCHAR(20) NOT NULL,
  qr_payload   CHAR(36)    NOT NULL UNIQUE,
  service_id   INT NOT NULL,
  customer_id  INT NOT NULL,
  counter_id   INT NULL,
  -- EXPIRED exists because queues are per-day: a token not reached before the
  -- branch closed can never be called, so it must stop reporting as waiting.
  status       ENUM('PENDING','IN_SERVICE','COMPLETED','CANCELLED','NO_SHOW','EXPIRED')
               NOT NULL DEFAULT 'PENDING',
  priority     TINYINT NOT NULL DEFAULT 0,
  service_date DATE NOT NULL,
  issued_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  called_at    TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  CONSTRAINT fk_token_service  FOREIGN KEY (service_id)  REFERENCES services(id),
  CONSTRAINT fk_token_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT fk_token_counter  FOREIGN KEY (counter_id)  REFERENCES counters(id) ON DELETE SET NULL,
  UNIQUE KEY uq_token_day (service_id, service_date, token_number),
  -- The trailing columns are DESC/ASC on purpose, to match the exact ordering of
  -- the "call next customer" query (priority DESC, issued_at ASC).
  --
  -- With a plain ascending index that mixed ordering needs a filesort, and a
  -- filesort has to read - and therefore lock - EVERY matching row before it can
  -- return one. Two counters calling at the same moment then behave badly: the
  -- first locks the whole queue, and the second's SKIP LOCKED skips all of it and
  -- reports "nobody is waiting" while customers are plainly still queued.
  --
  -- Ordering the index the same way the query does removes the filesort, so the
  -- scan stops at the first row it can lock and holds exactly that one.
  -- Requires MySQL 8.0+ (descending indexes).
  INDEX idx_token_queue (service_id, service_date, status, priority DESC, issued_at ASC),
  INDEX idx_token_customer (customer_id, service_date),
  INDEX idx_token_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  service_logs : immutable audit trail of every status transition
-- ---------------------------------------------------------------------
CREATE TABLE service_logs (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  token_id    INT NOT NULL,
  counter_id  INT NULL,
  staff_id    INT NULL,
  action      VARCHAR(40) NOT NULL,
  from_status VARCHAR(20),
  to_status   VARCHAR(20),
  note        VARCHAR(255),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_log_token FOREIGN KEY (token_id) REFERENCES tokens(id) ON DELETE CASCADE,
  INDEX idx_log_token (token_id),
  INDEX idx_log_created (created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  notifications : turn-approaching alerts
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  token_id   INT NULL,
  title      VARCHAR(120) NOT NULL,
  message    VARCHAR(255) NOT NULL,
  type       ENUM('INFO','APPROACHING','CALLED','COMPLETED') NOT NULL DEFAULT 'INFO',
  read_flag  TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_notif_token FOREIGN KEY (token_id) REFERENCES tokens(id) ON DELETE CASCADE,
  INDEX idx_notif_user (user_id, read_flag, created_at)
) ENGINE=InnoDB;

-- =====================================================================
--  SEED DATA
--  Every seeded account uses the password:  Digiq@123
-- =====================================================================
SET @pw = '$2a$10$w5BmSEktogWTjGo7Oo7dt.LJUKCwkN6PIfKpgIPGNQs73A1GXEWfe';

INSERT INTO users (full_name, email, phone, password_hash, role) VALUES
  ('System Administrator', 'admin@digiq.com',  '+256700000001', @pw, 'ADMIN'),
  ('Grace Nakato',         'grace@digiq.com',  '+256700000002', @pw, 'STAFF'),
  ('Daniel Okello',        'daniel@digiq.com', '+256700000003', @pw, 'STAFF'),
  ('Miriam Achieng',       'miriam@digiq.com', '+256700000004', @pw, 'STAFF'),
  ('Joseph Mugisha',       'joseph@mail.com',  '+256700000005', @pw, 'CUSTOMER'),
  ('Sarah Nabirye',        'sarah@mail.com',   '+256700000006', @pw, 'CUSTOMER'),
  ('Peter Ssempa',         'peter@mail.com',   '+256700000007', @pw, 'CUSTOMER');

INSERT INTO services (name, code, description, avg_service_minutes) VALUES
  ('Account Opening',   'ACC', 'New account registration and KYC verification', 15),
  ('Cash Deposit',      'DEP', 'Over-the-counter cash and cheque deposits',      5),
  ('Cash Withdrawal',   'WDR', 'Over-the-counter cash withdrawals',              6),
  ('Loan Consultation', 'LON', 'Loan advisory, application and appraisal',      25),
  ('Customer Support',  'SUP', 'Complaints, card issues and general enquiries', 10);

INSERT INTO counters (name, service_id, staff_id, status) VALUES
  ('Counter 1', 1, 2, 'OPEN'),
  ('Counter 2', 2, 3, 'OPEN'),
  ('Counter 3', 3, 4, 'OPEN'),
  ('Counter 4', 4, NULL, 'CLOSED'),
  ('Counter 5', 5, NULL, 'CLOSED');

-- ---------------------------------------------------------------------
--  Demo history so the analytics dashboard has data to plot.
--  Creates tokens across the previous 14 days.
-- ---------------------------------------------------------------------
DELIMITER //
CREATE PROCEDURE seed_history()
BEGIN
  DECLARE d INT DEFAULT 14;
  DECLARE n INT;
  DECLARE svc INT;
  DECLARE cust INT;
  DECLARE day_date DATE;
  DECLARE issued DATETIME;
  DECLARE called DATETIME;
  DECLARE done DATETIME;
  DECLARE seq INT;
  DECLARE roll DOUBLE;
  DECLARE st VARCHAR(20);

  WHILE d >= 1 DO
    SET day_date = DATE_SUB(CURDATE(), INTERVAL d DAY);
    SET svc = 1;
    WHILE svc <= 5 DO
      SET seq = 0;
      SET n = 6 + FLOOR(RAND() * 12);
      WHILE seq < n DO
        SET seq = seq + 1;
        SET cust = 5 + FLOOR(RAND() * 3);
        SET issued = TIMESTAMP(day_date, SEC_TO_TIME(28800 + FLOOR(RAND() * 28800)));
        SET called = issued + INTERVAL (3 + FLOOR(RAND() * 25)) MINUTE;
        SET done   = called + INTERVAL (3 + FLOOR(RAND() * 22)) MINUTE;
        SET roll = RAND();
        SET st = CASE WHEN roll < 0.90 THEN 'COMPLETED'
                      WHEN roll < 0.96 THEN 'CANCELLED'
                      ELSE 'NO_SHOW' END;
        INSERT INTO tokens (token_number, qr_payload, service_id, customer_id, counter_id,
                            status, priority, service_date, issued_at, called_at, completed_at)
        VALUES (CONCAT((SELECT code FROM services WHERE id = svc), '-', LPAD(seq, 4, '0')),
                UUID(), svc, cust, svc, st,
                IF(RAND() < 0.12, 1, 0), day_date, issued, called,
                IF(st = 'COMPLETED', done, NULL));
      END WHILE;
      SET svc = svc + 1;
    END WHILE;
    SET d = d - 1;
  END WHILE;
END//
DELIMITER ;

CALL seed_history();
DROP PROCEDURE seed_history;

INSERT INTO service_logs (token_id, counter_id, staff_id, action, from_status, to_status, created_at)
SELECT id, counter_id, NULL, 'COMPLETED', 'IN_SERVICE', status, completed_at
FROM tokens WHERE completed_at IS NOT NULL;

SELECT CONCAT('Seeded ', COUNT(*), ' historical tokens.') AS result FROM tokens;
