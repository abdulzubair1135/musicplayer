const fs = require('fs');
const path = require('path');

const target = path.join(__dirname, 'node_modules', '@distube', 'ytdl-core', 'dist', 'solver', 'worker.js');
if (fs.existsSync(target)) {
  let content = fs.readFileSync(target, 'utf8');
  if (content.includes('const TIMEOUT_MS = 2000;')) {
    content = content.replace('const TIMEOUT_MS = 2000;', 'const TIMEOUT_MS = 15000;');
    fs.writeFileSync(target, content, 'utf8');
    console.log('✅ Successfully patched @distube/ytdl-core solver worker timeout to 15000ms');
  } else {
    console.log('ℹ️ worker.js already patched or pattern not found');
  }
} else {
  console.log('⚠️ worker.js not found at', target);
}
