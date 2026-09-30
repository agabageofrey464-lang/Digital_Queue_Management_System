const fs = require('fs'), path = require('path');
const ROOT = path.resolve(__dirname, '..', 'src', 'main', 'java');

function norm(p) { return p.split(path.sep).join('/'); }

function walk(d, out = []) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}
const files = walk(ROOT);

// Every project class -> its package
const projClass = new Map();
for (const f of files) {
  const rel = norm(path.relative(ROOT, f));
  const pkg = path.dirname(rel).split('/').join('.');
  projClass.set(path.basename(f, '.java'), pkg);
}

const JAVA_LANG = new Set(['String', 'Integer', 'Long', 'Double', 'Boolean', 'Object', 'Exception',
  'RuntimeException', 'IllegalArgumentException', 'IllegalStateException', 'Math', 'System', 'Override',
  'Character', 'Byte', 'Short', 'Float', 'Class', 'Thread', 'Throwable', 'Number', 'StringBuilder',
  'CharSequence', 'Deprecated', 'SuppressWarnings', 'FunctionalInterface', 'Void', 'Iterable',
  'Comparable', 'Runnable', 'NumberFormatException']);

let problems = 0;
for (const f of files) {
  const src = fs.readFileSync(f, 'utf8');
  const rel = norm(path.relative(ROOT, f));
  const myPkg = path.dirname(rel).split('/').join('.');

  const imports = new Set();
  for (const l of src.match(/^import\s+(?:static\s+)?[\w.]+;/gm) || []) {
    const m = l.match(/^import\s+(?:static\s+)?([\w.]+);/);
    if (m) imports.add(m[1].split('.').pop());
  }

  // Strip comments, string/char literals, import and package lines
  const code = src
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .replace(/\/\/[^\n]*/g, ' ')
    .replace(/"(?:[^"\\]|\\.)*"/g, '""')
    .replace(/'(?:[^'\\]|\\.)*'/g, "''")
    .replace(/^\s*import[^\n]*$/gm, ' ')
    .replace(/^\s*package[^\n]*$/gm, ' ');

  // Capitalised identifiers not preceded by a dot => candidate type references
  const refs = new Set();
  const re = /(^|[^\w.$])([A-Z][A-Za-z0-9_]*)/g;
  let m;
  while ((m = re.exec(code)) !== null) refs.add(m[2]);

  const missing = [];
  for (const r of refs) {
    if (/^[A-Z0-9_]+$/.test(r)) continue;                 // CONSTANT_CASE / enum constants
    if (JAVA_LANG.has(r)) continue;
    if (imports.has(r)) continue;
    if (projClass.has(r)) {
      if (projClass.get(r) !== myPkg) {
        missing.push(r + '  <- project class in ' + projClass.get(r) + ', NOT imported');
      }
      continue;                                            // same package: fine
    }
    missing.push(r + '  <- unknown / not imported');
  }

  if (missing.length) {
    problems++;
    console.log('\n' + rel);
    for (const x of missing) console.log('   ! ' + x);
  }
}
console.log(problems === 0
  ? '\nPASS - every referenced type is imported or in the same package.'
  : '\n' + problems + ' file(s) flagged.');
