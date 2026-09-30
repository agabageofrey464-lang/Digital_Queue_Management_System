/*
 * Proves the access model the project intends:
 *
 *   - anyone can reach the public pages and create their own account
 *   - self-registration ALWAYS produces a customer, never staff or admin
 *   - a self-registered account cannot reach /admin/* or /staff/*
 *   - the sign-in page does not hand out the administrator password
 *   - staff and admin accounts can only be made from the admin console
 */
const B = 'http://localhost:8080/digiq';
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
const titleOf = b => (b.match(/<title>([^<]*)<\/title>/) || [])[1] || '?';

(async () => {
  console.log('\n=== PUBLIC ACCESS MODEL ===');

  // --- open to everyone, no sign-in ---
  for (const p of ['/', '/login', '/register', '/board']) {
    const r = await fetch(B + p, { redirect: 'manual' });
    if (r.status === 200) pass('public without signing in: ' + p);
    else fail(p + ' returned HTTP ' + r.status);
  }

  // --- the sign-in page must not leak the admin password ---
  const loginPage = await (await fetch(B + '/login')).text();
  if (!/Digiq@123/.test(loginPage) && !/admin@digiq\.com/.test(loginPage)) {
    pass('sign-in page does not list demo credentials');
  } else {
    fail('sign-in page still prints demo credentials - anyone could sign in as admin');
  }
  if (/Create a customer account|register/i.test(loginPage)) pass('sign-in page links to registration');
  else fail('no route to registration from the sign-in page');

  // --- anyone can create their own account ---
  const email = 'public' + Date.now() + '@example.com';
  const j = jar();
  await req(j, '/register');
  let r = await req(j, '/register', { method: 'POST', body: form({
    fullName: 'Public Visitor', email, password: 'Visitor@2026', confirmPassword: 'Visitor@2026' }) });
  let body = await r.text();
  if (/Available services|Book a token/.test(body)) pass('a stranger registered and was signed straight in');
  else { fail('registration failed: ' + titleOf(body)); process.exit(1); }

  // --- and lands as a CUSTOMER, not anything else ---
  const home = await (await req(j, '/customer/home')).text();
  if (/Book a token|Available services/.test(home)) pass('new account can use the customer area');
  else fail('new account cannot reach /customer/home');

  // --- but cannot reach the restricted areas ---
  for (const p of ['/admin/dashboard', '/admin/users', '/admin/analytics', '/staff/console']) {
    const res = await req(j, p);
    const t = await res.text();
    const reached = /Operations dashboard|Counter console|<h1 class="h1">Users<|<h1 class="h1">Analytics</.test(t);
    if (!reached) pass('blocked from ' + p);
    else fail('SELF-REGISTERED USER REACHED ' + p);
  }

  // --- role cannot be escalated by posting one ---
  const esc = 'escalate' + Date.now() + '@example.com';
  const j2 = jar();
  await req(j2, '/register');
  await req(j2, '/register', { method: 'POST', body: form({
    fullName: 'Would Be Admin', email: esc, password: 'Escalate@2026',
    confirmPassword: 'Escalate@2026', role: 'ADMIN' }) });   // <-- injected field
  const after = await req(j2, '/admin/dashboard');
  const afterBody = await after.text();
  if (!/Operations dashboard/.test(afterBody)) pass('posting role=ADMIN during registration is ignored');
  else fail('PRIVILEGE ESCALATION: role=ADMIN in the registration form was honoured');

  // --- the admin area still works for a real admin ---
  const aj = jar();
  await req(aj, '/login');
  const ar = await req(aj, '/login', { method: 'POST', body: form({
    email: 'admin@digiq.com', password: 'Digiq@123' }) });
  if (/Dashboard/.test(titleOf(await ar.text()))) pass('a real administrator still reaches the dashboard');
  else fail('administrator can no longer sign in');

  console.log(failures === 0 ? '\nACCESS MODEL: ALL PASSED' : '\nACCESS MODEL: ' + failures + ' FAILED');
  process.exit(failures === 0 ? 0 : 1);
})().catch(e => { console.error('CRASH: ' + e.stack); process.exit(1); });
