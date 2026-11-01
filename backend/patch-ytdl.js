const fs = require('fs');
const path = require('path');

// 1. Patch solver/worker.js TIMEOUT_MS
const workerTarget = path.join(__dirname, 'node_modules', '@distube', 'ytdl-core', 'dist', 'solver', 'worker.js');
if (fs.existsSync(workerTarget)) {
  let content = fs.readFileSync(workerTarget, 'utf8');
  content = content.replace(/const TIMEOUT_MS = \d+;/, 'const TIMEOUT_MS = 60000;');
  fs.writeFileSync(workerTarget, content, 'utf8');
  console.log('✅ Patched worker.js TIMEOUT_MS to 60000ms');
} else {
  console.log('⚠️ workerTarget not found:', workerTarget);
}

// 2. Patch solver/index.js CALL_DEADLINE_MS
const indexTarget = path.join(__dirname, 'node_modules', '@distube', 'ytdl-core', 'dist', 'solver', 'index.js');
if (fs.existsSync(indexTarget)) {
  let content = fs.readFileSync(indexTarget, 'utf8');
  content = content.replace(/const CALL_DEADLINE_MS = [0-9_]+;/, 'const CALL_DEADLINE_MS = 60_000;');
  fs.writeFileSync(indexTarget, content, 'utf8');
  console.log('✅ Patched index.js CALL_DEADLINE_MS to 60_000ms');
} else {
  console.log('⚠️ indexTarget not found:', indexTarget);
}
