const WebSocket = require('ws');
const B = 'http://localhost:8080/digiq';
const PASS = 'Digiq@123';

let failures = 0;
function pass(m) { console.log('  PASS  ' + m); }
function fail(m) { failures++; console.log('  FAIL  ' + m); }

// --- tiny cookie jar ---
function jar() {
  return { cookie: '' };
}
/*
 * Redirects are followed by hand. Node's fetch with redirect:"follow" only exposes
 * the FINAL response's headers, so the Set-Cookie issued on the 302 that a login
 * returns would be thrown away and every later request would look signed out.
 */
async function req(j, path, { method = 'GET', body = null, json = false } = {}) {
  let url = path.startsWith('http') ? path : B + path;
  let res;
  for (let hop = 0; hop < 6; hop++) {
    const headers = {};
    if (j.cookie) headers['Cookie'] = j.cookie;
    if (body && method !== 'GET') headers['Content-Type'] = 'application/x-www-form-urlencoded';
    if (json) { headers['X-Requested-With'] = 'fetch'; headers['Accept'] = 'application/json'; }

    res = await fetch(url, { method, headers, body, redirect: 'manual' });

    const sc = res.headers.getSetCookie ? res.headers.getSetCookie() : [];
    for (const c of sc) {
      const m = c.match(/^(JSESSIONID=[^;]+)/);
      if (m) j.cookie = m[1];
    }

    if (res.status >= 300 && res.status < 400 && res.headers.get('location')) {
      url = new URL(res.headers.get('location'), url).toString();
      method = 'GET';
      body = null;
      continue;
    }
    break;
  }
  return res;
}
async function login(email) {
  const j = jar();
  await req(j, '/login');
  const res = await req(j, '/login', {
    method: 'POST',
    body: new URLSearchParams({ email, password: PASS }).toString()
  });
  const text = await res.text();
  return { jar: j, ok: res.status === 200, body: text };
}

(async () => {
  console.log('\n=== WEBSOCKET LIVE EVENT TEST ===');

  // 1. Customer books a token so the queue is not empty
  const cust = await login('joseph@mail.com');
  if (!cust.ok) return fail('customer login');
  pass('customer signed in');

  // 2. Open the socket and collect events
  const events = [];
  const ws = new WebSocket('ws://localhost:8080/digiq/ws/queue');
  const opened = new Promise((resolve, reject) => {
    ws.on('open', resolve);
    ws.on('error', reject);
    setTimeout(() => reject(new Error('open timeout')), 8000);
  });
  ws.on('message', (d) => {
    try { events.push(JSON.parse(d.toString())); } catch (e) { /* ignore */ }
  });

  try { await opened; pass('WebSocket connected to /ws/queue'); }
  catch (e) { return fail('WebSocket connect: ' + e.message); }

  await new Promise(r => setTimeout(r, 400));
  const hello = events.find(e => e.type === 'CONNECTED');
  if (hello) pass('received CONNECTED handshake (clients=' + hello.payload.clients + ')');
  else fail('no CONNECTED handshake');

  // 3. ping/pong keepalive
  events.length = 0;
  ws.send('ping');
  await new Promise(r => setTimeout(r, 400));
  if (events.find(e => e.type === 'PONG')) pass('ping -> PONG keepalive works');
  else fail('no PONG reply');

  // 4. Booking a token must broadcast TOKEN_ISSUED
  events.length = 0;
  const bookRes = await req(cust.jar, '/customer/book', {
    method: 'POST',
    body: new URLSearchParams({ serviceId: '1' }).toString()
  });
  const bookedUrl = bookRes.url;
  const tokenId = (bookedUrl.match(/id=(\d+)/) || [])[1];
  await new Promise(r => setTimeout(r, 600));
  const issued = events.find(e => e.type === 'TOKEN_ISSUED');
  if (issued) pass('TOKEN_ISSUED broadcast: ' + issued.payload.token.tokenNumber +
                   ' (waiting=' + issued.payload.waiting + ')');
  else fail('no TOKEN_ISSUED event');

  // 5. Staff calling next must broadcast TOKEN_CALLED
  const staff = await login('grace@digiq.com');
  if (!staff.ok) return fail('staff login');
  events.length = 0;
  const callRes = await req(staff.jar, '/staff/action', {
    method: 'POST', json: true,
    body: new URLSearchParams({ action: 'call' }).toString()
  });
  const callJson = await callRes.json();
  await new Promise(r => setTimeout(r, 600));
  const called = events.find(e => e.type === 'TOKEN_CALLED');
  if (called) pass('TOKEN_CALLED broadcast: ' + called.payload.token.tokenNumber +
                   ' -> ' + called.payload.counterName);
  else fail('no TOKEN_CALLED event (call said: ' + JSON.stringify(callJson) + ')');

  const qc = events.find(e => e.type === 'QUEUE_CHANGED');
  if (qc) pass('QUEUE_CHANGED broadcast for service ' + qc.payload.serviceId);
  else fail('no QUEUE_CHANGED event');

  // 6. Board payload must never leak a customer name
  const boardRes = await fetch(B + '/board/data');
  const board = await boardRes.json();
  const leaked = JSON.stringify(board).match(/Joseph|Mugisha|Sarah|Nabirye/i);
  if (!leaked) pass('board payload contains no customer names');
  else fail('board payload LEAKED a name: ' + leaked[0]);

  // 7. Completing must broadcast TOKEN_UPDATED
  events.length = 0;
  await req(staff.jar, '/staff/action', {
    method: 'POST', json: true,
    body: new URLSearchParams({ action: 'complete' }).toString()
  });
  await new Promise(r => setTimeout(r, 600));
  const upd = events.find(e => e.type === 'TOKEN_UPDATED');
  if (upd) pass('TOKEN_UPDATED broadcast: ' + upd.payload.token.tokenNumber +
                ' -> ' + upd.payload.token.status);
  else fail('no TOKEN_UPDATED event');

  ws.close();
  console.log(failures === 0 ? '\nWEBSOCKET: ALL PASSED' : '\nWEBSOCKET: ' + failures + ' FAILED');
  process.exit(failures === 0 ? 0 : 1);
})().catch(e => { console.error('CRASH: ' + e.stack); process.exit(1); });
