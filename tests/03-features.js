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
  return { jar: j, body: await r.text() };
}

(async () => {
  /* ---------------- Registration ---------------- */
  console.log('\n=== REGISTRATION ===');
  const uniq = 'test' + Date.now() + '@mail.com';
  const nj = jar();
  await req(nj, '/register');
  let r = await req(nj, '/register', { method: 'POST', body: form({
    fullName: 'Test Person', email: uniq, phone: '+256700111222',
    password: 'Testing@123', confirmPassword: 'Testing@123' }) });
  let body = await r.text();
  if (body.match(/Available services|Book a token/)) pass('new account registered and signed in');
  else fail('registration did not land on the customer home');

  // duplicate email must be rejected
  const dj = jar();
  await req(dj, '/register');
  r = await req(dj, '/register', { method: 'POST', body: form({
    fullName: 'Dupe', email: uniq, password: 'Testing@123', confirmPassword: 'Testing@123' }) });
  body = await r.text();
  if (body.match(/already exists/i)) pass('duplicate email rejected');
  else fail('duplicate email was NOT rejected');

  // mismatched passwords must be rejected
  const mj = jar();
  await req(mj, '/register');
  r = await req(mj, '/register', { method: 'POST', body: form({
    fullName: 'Mismatch', email: 'mm' + Date.now() + '@mail.com',
    password: 'Testing@123', confirmPassword: 'Different@123' }) });
  body = await r.text();
  if (body.match(/do not match/i)) pass('password mismatch rejected');
  else fail('password mismatch was NOT rejected');

  /* ---------------- Wrong password ---------------- */
  console.log('\n=== AUTH HARDENING ===');
  const wj = jar();
  await req(wj, '/login');
  r = await req(wj, '/login', { method: 'POST', body: form({ email: 'admin@digiq.com', password: 'wrong' }) });
  body = await r.text();
  if (body.match(/did not match an active account/)) pass('wrong password rejected');
  else fail('wrong password was accepted');

  /* ---------------- Admin CRUD ---------------- */
  console.log('\n=== ADMIN CRUD ===');
  const admin = await login('admin@digiq.com');
  if (!admin.body.match(/Operations dashboard/)) return fail('admin login');
  pass('admin signed in');

  // create a service
  const code = 'T' + String(Date.now()).slice(-2);
  await req(admin.jar, '/admin/services', { method: 'POST', body: form({
    action: 'save', id: '0', name: 'Test Service', code, description: 'Created by the test',
    avgServiceMinutes: '7', active: 'on' }) });
  let page = await (await req(admin.jar, '/admin/services')).text();
  if (page.includes('Test Service') && page.includes(code)) pass('service created (' + code + ')');
  else fail('service was not created');

  const svcId = (page.match(new RegExp('data-field-id="(\\d+)"[^>]*data-field-name="Test Service"')) || [])[1]
             || (page.match(/data-field-id="(\d+)"\s+data-field-name="Test Service"/) || [])[1];

  // duplicate code must be rejected
  const dupRes = await req(admin.jar, '/admin/services', { method: 'POST', body: form({
    action: 'save', id: '0', name: 'Clash', code, avgServiceMinutes: '5', active: 'on' }) });
  const dupBody = await dupRes.text();
  if (dupBody.match(/already in use/)) pass('duplicate token code rejected');
  else fail('duplicate token code was NOT rejected');

  // create a user
  const staffEmail = 'teststaff' + Date.now() + '@digiq.com';
  await req(admin.jar, '/admin/users', { method: 'POST', body: form({
    action: 'save', id: '0', fullName: 'Test Staff', email: staffEmail,
    phone: '+256700333444', role: 'STAFF', password: 'Testing@123', active: 'on' }) });
  page = await (await req(admin.jar, '/admin/users')).text();
  if (page.includes(staffEmail)) pass('staff account created');
  else fail('staff account was not created');

  // the new staff member can sign in
  const ns = await login(staffEmail, 'Testing@123');
  if (ns.body.match(/No counter is assigned|Counter console/)) pass('new staff can sign in (no counter yet)');
  else fail('new staff could not sign in');

  /* ---------------- Priority ordering ---------------- */
  console.log('\n=== PRIORITY ORDERING ===');
  // Service 2 (Cash Deposit) is served by Counter 2 / Daniel
  const c1 = await login('joseph@mail.com');
  const c2 = await login('sarah@mail.com');

  await req(c1.jar, '/customer/book', { method: 'POST', body: form({ serviceId: '2' }) });
  const normalRes = await req(c1.jar, '/customer/tokens');
  await req(c2.jar, '/customer/book', { method: 'POST', body: form({ serviceId: '2', priority: 'on' }) });

  const staff2 = await login('daniel@digiq.com');
  await req(staff2.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'open' }) });
  let callJson = await (await req(staff2.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) })).json();
  if (callJson.ok && callJson.customerName === 'Sarah Nabirye') {
    pass('priority token jumped the queue (' + callJson.tokenNumber + ' / ' + callJson.customerName + ')');
  } else {
    fail('priority ordering wrong -> ' + JSON.stringify(callJson));
  }
  await req(staff2.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'complete' }) });
  callJson = await (await req(staff2.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) })).json();
  if (callJson.ok && callJson.customerName === 'Joseph Mugisha') pass('normal token served next');
  else fail('second call wrong -> ' + JSON.stringify(callJson));
  await req(staff2.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'complete' }) });

  /* ---------------- Notifications ---------------- */
  console.log('\n=== NOTIFICATIONS ===');
  const notif = await (await req(c2.jar, '/customer/notifications', { json: true })).json();
  const types = (notif.items || []).map(n => n.type);
  if (types.includes('CALLED')) pass('customer got a CALLED alert');
  else fail('no CALLED alert (' + types.join(',') + ')');
  if (types.includes('COMPLETED')) pass('customer got a COMPLETED alert');
  else fail('no COMPLETED alert (' + types.join(',') + ')');
  if (notif.unread > 0) pass('unread count is ' + notif.unread);
  else fail('unread count is zero');
  await req(c2.jar, '/customer/notifications', { method: 'POST', json: true });
  const after = await (await req(c2.jar, '/customer/notifications', { json: true })).json();
  if (after.unread === 0) pass('mark-all-read clears the count');
  else fail('mark-all-read left ' + after.unread);

  /* ---------------- Cancel ---------------- */
  console.log('\n=== CANCEL ===');
  await req(c1.jar, '/customer/book', { method: 'POST', body: form({ serviceId: '4' }) });
  const homePage = await (await req(c1.jar, '/customer/home')).text();
  const cancelId = (homePage.match(/name="tokenId" value="(\d+)"/) || [])[1];
  if (cancelId) {
    await req(c1.jar, '/customer/cancel', { method: 'POST', body: form({ tokenId: cancelId }) });
    const st = await (await req(c1.jar, '/customer/status?id=' + cancelId, { json: true })).json();
    if (st.status === 'CANCELLED') pass('pending token cancelled');
    else fail('cancel left status ' + st.status);
  } else fail('could not find a cancellable token');

  /* ---------------- Concurrent call (SKIP LOCKED) ---------------- */
  console.log('\n=== CONCURRENCY: two counters, one queue ===');
  // Point Counter 2 at Account Opening so Grace and Daniel share service 1
  await req(admin.jar, '/admin/counters', { method: 'POST', body: form({
    action: 'save', id: '2', name: 'Counter 2', serviceId: '1', staffId: '3', status: 'OPEN' }) });

  // queue up four tokens on service 1
  for (let i = 0; i < 4; i++) {
    await req(i % 2 ? c1.jar : c2.jar, '/customer/book', { method: 'POST', body: form({ serviceId: '1' }) });
  }
  const grace = await login('grace@digiq.com');
  const daniel = await login('daniel@digiq.com');
  await req(grace.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'open' }) });
  await req(daniel.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'open' }) });

  // fire both calls at the same instant
  const [ga, da] = await Promise.all([
    req(grace.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) }),
    req(daniel.jar, '/staff/action', { method: 'POST', json: true, body: form({ action: 'call' }) })
  ]);
  const gj = await ga.json(), dj2 = await da.json();
  if (gj.ok && dj2.ok && gj.tokenNumber !== dj2.tokenNumber) {
    pass('simultaneous calls got DIFFERENT tokens: ' + gj.tokenNumber + ' vs ' + dj2.tokenNumber);
  } else if (gj.ok && dj2.ok) {
    fail('DOUBLE-CALL BUG: both counters got ' + gj.tokenNumber);
  } else {
    fail('a call failed -> grace=' + JSON.stringify(gj) + ' daniel=' + JSON.stringify(dj2));
  }

  // restore Counter 2 to Cash Deposit
  await req(admin.jar, '/admin/counters', { method: 'POST', body: form({
    action: 'save', id: '2', name: 'Counter 2', serviceId: '2', staffId: '3', status: 'OPEN' }) });

  /* ---------------- Cleanup ---------------- */
  if (svcId) {
    await req(admin.jar, '/admin/services', { method: 'POST', body: form({ action: 'delete', id: svcId }) });
    pass('test service deleted');
  }

  console.log(failures === 0 ? '\nALL PASSED' : '\n' + failures + ' FAILED');
  process.exit(failures === 0 ? 0 : 1);
})().catch(e => { console.error('CRASH: ' + e.stack); process.exit(1); });
