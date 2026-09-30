const B = 'http://localhost:8080/digiq';
const PASS = 'Digiq@123';
let failures = 0;
const pass = m => console.log('  PASS  ' + m);
const fail = m => { failures++; console.log('  FAIL  ' + m); };

function jar() { return { cookie: '' }; }
async function req(j, path, { method = 'GET', body = null, json = false } = {}) {
  let url = path.startsWith('http') ? path : B + path;
  let res, m = method, b = body;
  for (let hop = 0; hop < 6; hop++) {
    const headers = {};
    if (j.cookie) headers['Cookie'] = j.cookie;
    if (b && m !== 'GET') headers['Content-Type'] = 'application/x-www-form-urlencoded';
    if (json) { headers['X-Requested-With'] = 'fetch'; headers['Accept'] = 'application/json'; }
    res = await fetch(url, { method: m, headers, body: b, redirect: 'manual' });
    for (const c of (res.headers.getSetCookie ? res.headers.getSetCookie() : [])) {
      const mm = c.match(/^(JSESSIONID=[^;]+)/); if (mm) j.cookie = mm[1];
    }
    if (res.status >= 300 && res.status < 400 && res.headers.get('location')) {
      url = new URL(res.headers.get('location'), url).toString(); m = 'GET'; b = null; continue;
    }
    break;
  }
  return res;
}
const form = o => new URLSearchParams(o).toString();
async function login(email, password = PASS) {
  const j = jar();
  await req(j, '/login');
  const r = await req(j, '/login', { method: 'POST', body: form({ email, password }) });
  const body = await r.text();
  const title = (body.match(/<title>([^<]*)<\/title>/) || [])[1] || '?';
  return { jar: j, body, title };
}

(async () => {
  console.log('\n=== CONCURRENCY: two counters draining one queue ===');

  const admin = await login('admin@digiq.com');
  if (!/Dashboard/.test(admin.title)) return fail('admin login (title=' + admin.title + ')');
  pass('admin signed in');

  // 1. Point Counter 2 at Account Opening (service 1), staffed by Daniel (id 3)
  let r = await req(admin.jar, '/admin/counters', { method: 'POST', body: form({
    action: 'save', id: '2', name: 'Counter 2', serviceId: '1', staffId: '3', status: 'OPEN' }) });
  let page = await r.text();
  if (/Counter updated/.test(page)) pass('Counter 2 repointed to Account Opening');
  else return fail('counter update did not report success');

  const m2 = page.match(/data-field-id="2"\s+data-field-name="[^"]*"\s+data-field-serviceId="(\d+)"/i);
  if (m2 && m2[1] === '1') pass('confirmed Counter 2 serviceId = 1');
  else fail('Counter 2 serviceId is ' + (m2 ? m2[1] : '?') + ', expected 1');

  // 2. Queue up six tokens on service 1
  const c1 = await login('joseph@mail.com');
  const c2 = await login('sarah@mail.com');
  const booked = [];
  for (let i = 0; i < 6; i++) {
    const res = await req(i % 2 ? c1.jar : c2.jar, '/customer/book', {
      method: 'POST', body: form({ serviceId: '1' }) });
    const t = (await res.text()).match(/ACC-\d{4}/);
    if (t) booked.push(t[0]);
  }
  if (booked.length === 6) pass('queued 6 tokens: ' + booked.join(', '));
  else fail('only booked ' + booked.length + ' tokens');

  // 3. Both staff open their counters, clearing anything left in service
  const grace = await login('grace@digiq.com');
  const daniel = await login('daniel@digiq.com');
  for (const [who, s] of [['Grace', grace], ['Daniel', daniel]]) {
    const j = await (await req(s.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'open' }) })).json();
    if (j.ok) pass(who + ': counter open'); else fail(who + ' open -> ' + j.message);
    // a token left IN_SERVICE by an earlier run would block the first call
    await req(s.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'complete' }) });
  }

  // 4. THREE simultaneous rounds of paired calls
  const seen = [];
  for (let round = 1; round <= 3; round++) {
    const [ga, da] = await Promise.all([
      req(grace.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) }),
      req(daniel.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) })
    ]);
    const gj = await ga.json(), dj = await da.json();
    if (!gj.ok || !dj.ok) {
      fail('round ' + round + ': grace=' + JSON.stringify(gj) + ' daniel=' + JSON.stringify(dj));
    } else if (gj.tokenNumber === dj.tokenNumber) {
      fail('round ' + round + ': DOUBLE-CALL BUG - both got ' + gj.tokenNumber);
    } else {
      pass('round ' + round + ': ' + gj.tokenNumber + ' (Grace) vs ' + dj.tokenNumber + ' (Daniel) - distinct');
      seen.push(gj.tokenNumber, dj.tokenNumber);
    }
    // clear both counters for the next round
    await Promise.all([
      req(grace.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'complete' }) }),
      req(daniel.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'complete' }) })
    ]);
  }

  const dupes = seen.filter((v, i) => seen.indexOf(v) !== i);
  if (dupes.length === 0) pass('no token was served twice across all rounds (' + seen.length + ' served)');
  else fail('tokens served more than once: ' + dupes.join(', '));

  // 5. Restore Counter 2
  await req(admin.jar, '/admin/counters', { method: 'POST', body: form({
    action: 'save', id: '2', name: 'Counter 2', serviceId: '2', staffId: '3', status: 'OPEN' }) });
  pass('Counter 2 restored to Cash Deposit');

  console.log(failures === 0 ? '\nCONCURRENCY: ALL PASSED' : '\nCONCURRENCY: ' + failures + ' FAILED');
  process.exit(failures === 0 ? 0 : 1);
})().catch(e => { console.error('CRASH: ' + e.stack); process.exit(1); });
