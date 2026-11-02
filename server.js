const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const APK_PATH = path.join(__dirname, 'EUEE-Mastery.apk');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.apk': 'application/vnd.android.package-archive',
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  let pathname = decodeURIComponent(url.pathname);

  // Serve APK directly
  if (pathname === '/EUEE-Mastery.apk' || pathname === '/apk/EUEE-Mastery.apk' || pathname === '/download' || pathname === '/Mastery.apk' || pathname === '/apk/Mastery.apk') {
    const candidateApk = fs.existsSync(path.join(__dirname, 'Mastery.apk')) ? path.join(__dirname, 'Mastery.apk') : APK_PATH;
    if (fs.existsSync(candidateApk)) {
      const stat = fs.statSync(candidateApk);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="Mastery.apk"',
      });
      return fs.createReadStream(candidateApk).pipe(res);
    }
  }

  // Root path: serve index.html
  if (pathname === '/' || pathname === '/index.html') {
    const indexPath = path.join(PUBLIC_DIR, 'index.html');
    if (fs.existsSync(indexPath)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return fs.createReadStream(indexPath).pipe(res);
    }
  }

  // Try static files in public
  let filePath = path.join(PUBLIC_DIR, pathname);
  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    return fs.createReadStream(filePath).pipe(res);
  }

  // Fallback to web app
  const fallbackApp = path.join(PUBLIC_DIR, 'assets', 'aistudio', 'EUEEastery_Full_Offline_ee717a0c.html');
  if (fs.existsSync(fallbackApp)) {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    return fs.createReadStream(fallbackApp).pipe(res);
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`EUEE Mastery web companion & APK server listening on port ${PORT}`);
});
