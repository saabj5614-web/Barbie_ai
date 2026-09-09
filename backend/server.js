import http from 'node:http';
import { config } from './core/config.js';
import { plannerSystem } from './core/prompts.js';
import { planAction } from './agents/action-planner.js';
import { analyzeScreen } from './agents/screen-coach.js';
import { chatWithFallback } from './providers/provider-manager.js';
import { githubWriteFile } from './github-tools.js';

function json(res, code, data) {
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, X-Barbie-Secret',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS'
  });
  res.end(JSON.stringify(data));
}
function authorized(req) { return !config.backendSecret || req.headers['x-barbie-secret'] === config.backendSecret; }
function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => { body += chunk; if (body.length > 5000000) req.destroy(); });
    req.on('end', () => { try { resolve(JSON.parse(body || '{}')); } catch { reject(new Error('invalid_json')); } });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true, name: 'Barbie AI', version: '4.0', architecture: 'modular-multi-provider' });
  if (!authorized(req)) return json(res, 401, { error: 'unauthorized' });
  if (req.method !== 'POST') return json(res, 404, { error: 'not_found' });
  try {
    const data = await readBody(req);
    if (req.url === '/api/chat' || req.url === '/api/action') {
      const message = String(data.message || '').trim();
      if (!message) return json(res, 400, { error: 'message_required' });
      return json(res, 200, await planAction(message));
    }
    if (req.url === '/api/ask') {
      const message = String(data.message || '').trim();
      if (!message) return json(res, 400, { error: 'message_required' });
      return json(res, 200, await chatWithFallback(message, plannerSystem));
    }
    if (req.url === '/api/screen') {
      const image = String(data.imageBase64 || '').replace(/^data:image\/(?:jpeg|jpg|png);base64,/, '');
      if (!image) return json(res, 400, { error: 'image_required' });
      if (image.length > 4500000) return json(res, 413, { error: 'image_too_large' });
      return json(res, 200, await analyzeScreen(image, String(data.prompt || '').trim()));
    }
    if (req.url === '/api/github/file') {
      if (!config.githubOwner || !config.githubRepo) return json(res, 503, { error: 'github_repo_not_configured' });
      const result = await githubWriteFile({ ...data, owner: config.githubOwner, repo: config.githubRepo });
      return json(res, 200, { ok: true, ...result });
    }
    return json(res, 404, { error: 'not_found' });
  } catch (error) {
    console.error(error);
    return json(res, error?.message === 'invalid_json' ? 400 : 500, { error: error?.message || 'server_error' });
  }
});
server.listen(config.port, () => console.log(`Barbie AI backend listening on ${config.port}`));
