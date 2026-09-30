const fs = require('fs'), path = require('path');
const JAVA  = path.resolve(__dirname, '..', 'src', 'main', 'java');
const VIEWS = path.resolve(__dirname, '..', 'src', 'main', 'webapp', 'WEB-INF', 'views');

function walk(d, ext, out = []) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p, ext, out); else if (e.name.endsWith(ext)) out.push(p);
  }
  return out;
}

// Every bean property exposed anywhere in the model/dao layer
const props = new Set();
for (const f of walk(JAVA, '.java')) {
  const src = fs.readFileSync(f, 'utf8');
  const re = /public\s+[\w<>\[\],.\s?]+\s+(get|is)([A-Z]\w*)\s*\(\s*\)/g;
  let m;
  while ((m = re.exec(src)) !== null) {
    props.add(m[2].charAt(0).toLowerCase() + m[2].slice(1));
  }
}

// Implicit / container-provided things EL resolves without a getter
const IMPLICIT = new Set([
  'request', 'contextPath', 'session', 'servletContext', 'pageContext',
  'size', 'length', 'empty', 'key', 'value', 'index', 'count', 'first', 'last',
  'requestScope', 'sessionScope', 'applicationScope', 'param', 'paramValues', 'header',
  // Map keys used in the analytics rows (plain LinkedHashMaps, not beans)
  'service', 'code', 'avgWait', 'avgService', 'counter', 'served', 'date', 'issued',
  'completed', 'status', 'count', 'hour', 'label'
]);

const bad = new Map();
for (const f of walk(VIEWS, '.jsp')) {
  const src = fs.readFileSync(f, 'utf8');
  const rel = path.relative(VIEWS, f).split(path.sep).join('/');
  // every  ${ ... }  expression
  const exprs = src.match(/\$\{[^}]*\}/g) || [];
  for (const e of exprs) {
    // dotted property accesses:  foo.bar.baz
    const chains = e.match(/[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+/g) || [];
    for (const chain of chains) {
      const parts = chain.split('.');
      for (let i = 1; i < parts.length; i++) {
        const p = parts[i];
        if (IMPLICIT.has(p)) continue;
        if (props.has(p)) continue;
        const key = p + '   (in ' + chain + ')';
        if (!bad.has(rel)) bad.set(rel, new Set());
        bad.get(rel).add(key);
      }
    }
  }
}

if (bad.size === 0) {
  console.log('PASS - every EL property maps to a getter (or a known implicit).');
} else {
  for (const [file, set] of bad) {
    console.log('\n' + file);
    [...set].forEach(x => console.log('   ? ' + x));
  }
}
