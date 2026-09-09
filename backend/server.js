import http from 'node:http';
import { githubWriteFile } from './github-tools.js';

const PORT = Number(process.env.PORT || 8080);
const OPENROUTER_KEY = process.env.OPENROUTER_API_KEY || '';
const HF_TOKEN = process.env.HUGGINGFACE_TOKEN || '';
const BACKEND_SECRET = process.env.BACKEND_SECRET || '';
const GITHUB_OWNER = process.env.GITHUB_OWNER || '';
const GITHUB_REPO = process.env.GITHUB_REPO || '';

const system = `You are Barbie AI, a personal Android assistant. Reply in Roman Urdu by default. Do not use Hindi or Devanagari. If the user clearly speaks English, Urdu, Punjabi, Sindhi or another language, reply in that language, but never Hindi. Be concise. You can understand natural requests; users do not need fixed commands. Never claim a phone action happened unless the Android app confirms it.`;
const plannerSystem = `${system}
Return ONLY one JSON object, with no markdown, using this shape: {"reply":"short natural reply","action":{"type":"none"}}.
Possible action types: none, youtube_search, web_search, open_whatsapp, call_number, open_files, screen_share, back, home, click_text, github_request.
For youtube_search use {"type":"youtube_search","query":"..."}; web_search uses query; open_whatsapp uses optional number and message; call_number uses number; click_text uses text; others need no extra fields.
Choose an action only when the user's request clearly asks for it. For call/WhatsApp, extract a phone number only if one is actually present; never invent a number. For requests such as "dial this person's number" without a number, explain that Barbie needs the contact/number and can open the relevant app. Keep reply concise.`;
const screenSystem = `${system} You are also Barbie Screen Coach. When given a phone screenshot, describe only what is visibly present. If asked what to do next, give short, safe, concrete steps. Never claim you clicked, typed, sent, purchased, deleted, or changed anything unless the Android app confirms that action.`;

function json(res, code, data) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type, X-Barbie-Secret', 'Access-Control-Allow-Methods': 'GET,POST,OPTIONS' });
  res.end(JSON.stringify(data));
}
function authorized(req) { return !BACKEND_SECRET || req.headers['x-barbie-secret'] === BACKEND_SECRET; }

async function provider(url, key, model, message, promptSystem = system) {
  const r = await fetch(url, { method: 'POST', headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ model, messages: [{ role: 'system', content: promptSystem }, { role: 'user', content: message }], temperature: 0.2 }) });
  const raw = await r.text();
  if (!r.ok) throw new Error(`provider_${r.status}`);
  const data = JSON.parse(raw);
  return data?.choices?.[0]?.message?.content?.trim() || '';
}

function parsePlan(raw) {
  try { return JSON.parse(raw); } catch {}
  const match = raw.match(/\{[\s\S]*\}/);
  if (!match) return null;
  try { return JSON.parse(match[0]); } catch { return null; }
}

async function plan(message) {
  const providers = [];
  if (OPENROUTER_KEY) providers.push(['https://openrouter.ai/api/v1/chat/completions', OPENROUTER_KEY, 'openrouter/free', 'openrouter']);
  if (HF_TOKEN) providers.push(['https://router.huggingface.co/v1/chat/completions', HF_TOKEN, 'openai/gpt-oss-120b:fastest', 'huggingface']);
  for (const [url, key, model, name] of providers) {
    try {
      const raw = await provider(url, key, model, message, plannerSystem);
      const parsed = parsePlan(raw);
      if (parsed?.reply) return { reply: String(parsed.reply), action: parsed.action && typeof parsed.action === 'object' ? parsed.action : { type: 'none' }, provider: name };
    } catch {}
  }
  return { reply: 'AI provider abhi configured nahi hai.', action: { type: 'none' }, provider: 'none' };
}

async function visionProvider(url, key, model, imageData, prompt, promptSystem = screenSystem) {
  const content = [{ type: 'text', text: prompt || 'Is screenshot mein kya ho raha hai aur mujhe next kya karna chahiye?' }, { type: 'image_url', image_url: { url: `data:image/jpeg;base64,${imageData}` } }];
  const r = await fetch(url, { method: 'POST', headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ model, messages: [{ role: 'system', content: promptSystem }, { role: 'user', content }], temperature: 0.2 }) });
  const raw = await r.text();
  if (!r.ok) throw new Error(`vision_provider_${r.status}`);
  const data = JSON.parse(raw);
  return data?.choices?.[0]?.message?.content?.trim() || '';
}

async function analyzeScreen(imageData, prompt) {
  if (!OPENROUTER_KEY && !HF_TOKEN) return { reply: 'Screen analysis ke liye AI provider abhi configured nahi hai.', provider: 'none' };
  if (OPENROUTER_KEY) { try { const reply = await visionProvider('https://openrouter.ai/api/v1/chat/completions', OPENROUTER_KEY, 'openrouter/free', imageData, prompt); if (reply) return { reply, provider: 'openrouter' }; } catch {} }
  if (HF_TOKEN) { try { const reply = await visionProvider('https://router.huggingface.co/v1/chat/completions', HF_TOKEN, 'openai/gpt-oss-120b:fastest', imageData, prompt); if (reply) return { reply, provider: 'huggingface' }; } catch {} }
  return { reply: 'Is waqt screen image ko analyze karne wala provider available nahi hai.', provider: 'none' };
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true, name: 'Barbie AI', version: '3.0' });
  if (!authorized(req)) return json(res, 401, { error: 'unauthorized' });
  if (req.method !== 'POST') return json(res, 404, { error: 'not_found' });
  let body = '';
  req.on('data', chunk => { body += chunk; if (body.length > 5000000) req.destroy(); });
  req.on('end', async () => {
    try {
      const data = JSON.parse(body || '{}');
      if (req.url === '/api/action') {
        const message = String(data.message || '').trim();
        if (!message) return json(res, 400, { error: 'message_required' });
        return json(res, 200, await plan(message));
      }
      if (req.url === '/api/screen') {
        const image = String(data.imageBase64 || '').replace(/^data:image\/jpeg;base64,/, '');
        const prompt = String(data.prompt || '').trim();
        if (!image) return json(res, 400, { error: 'image_required' });
        if (image.length > 4500000) return json(res, 413, { error: 'image_too_large' });
        return json(res, 200, await analyzeScreen(image, prompt));
      }
      if (req.url === '/api/github/file') {
        if (!GITHUB_OWNER || !GITHUB_REPO) return json(res, 503, { error: 'github_repo_not_configured' });
        const result = await githubWriteFile({ ...data, owner: GITHUB_OWNER, repo: GITHUB_REPO });
        return json(res, 200, { ok: true, ...result });
      }
      if (req.url !== '/api/chat') return json(res, 404, { error: 'not_found' });
      const message = String(data.message || '').trim();
      if (!message) return json(res, 400, { error: 'message_required' });
      return json(res, 200, await plan(message));
    } catch (e) { return json(res, 500, { error: e?.message === 'github_not_configured' ? 'github_not_configured' : 'server_error' }); }
  });
});
server.listen(PORT, () => console.log(`Barbie AI backend listening on ${PORT}`));
