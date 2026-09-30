/*
 * Does `... ORDER BY priority DESC, issued_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED`
 * hand the SECOND caller the next row, or nothing at all?
 *
 * Session A locks the head of the queue and holds it. Session B then runs the
 * identical statement. If B comes back empty while rows are plainly available,
 * LIMIT is being applied before the skip and the query is unsafe.
 */
const mysql = require('mysql2/promise');

const CFG = { host: '127.0.0.1', port: 3306, user: 'root', database: 'digiq' };
const SQL_LIMIT1 =
  "SELECT id, token_number FROM tokens " +
  "WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING' " +
  "ORDER BY priority DESC, issued_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED";
const SQL_BATCH =
  "SELECT id, token_number FROM tokens " +
  "WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING' " +
  "ORDER BY priority DESC, issued_at ASC LIMIT 10 FOR UPDATE SKIP LOCKED";

(async () => {
  const setup = await mysql.createConnection(CFG);

  // Make sure there are plenty of PENDING rows on service 1
  const [[{ n }]] = await setup.query(
    "SELECT COUNT(*) n FROM tokens WHERE service_id=1 AND service_date=CURDATE() AND status='PENDING'");
  console.log('PENDING tokens on service 1: ' + n);
  if (n < 5) {
    console.log('seeding a few more...');
    for (let i = 0; i < 6; i++) {
      await setup.query(
        "INSERT INTO tokens (token_number, qr_payload, service_id, customer_id, status, priority, service_date) " +
        "VALUES (CONCAT('ACC-', LPAD((SELECT COALESCE(MAX(CAST(SUBSTRING(token_number,5) AS UNSIGNED)),0)+1 " +
        "FROM tokens t2 WHERE t2.service_id=1 AND t2.service_date=CURDATE()),4,'0')), UUID(), 1, 5, 'PENDING', 0, CURDATE())");
    }
  }
  await setup.end();

  for (const [label, sql] of [['LIMIT 1 ', SQL_LIMIT1], ['LIMIT 10', SQL_BATCH]]) {
    let empties = 0;
    const TRIALS = 8;
    for (let t = 0; t < TRIALS; t++) {
      const a = await mysql.createConnection(CFG);
      const b = await mysql.createConnection(CFG);
      await a.beginTransaction();
      await b.beginTransaction();

      const [ra] = await a.query(sql, [1]);          // A locks the head
      const [rb] = await b.query(sql, [1]);          // B tries while A holds it

      if (t === 0) {
        console.log(`\n  ${label}: A got [${ra.map(r => r.token_number).join(',')}]`);
        console.log(`  ${label}: B got [${rb.map(r => r.token_number).join(',')}]`);
      }
      if (rb.length === 0) empties++;

      await a.rollback(); await b.rollback();
      await a.end(); await b.end();
    }
    console.log(`  ${label}: second caller came back EMPTY in ${empties}/${TRIALS} trials`);
  }
  process.exit(0);
})().catch(e => { console.error(e); process.exit(1); });
