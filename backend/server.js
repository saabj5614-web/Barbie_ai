import http from 'node:http';

const PORT = Number(process.env.PORT || 8080);
const OPENROUTER_KEY = process.env.OPENROUTER_API_KEY || '';
const HF_TOKEN = process.env.HUGGINGFACE_TOKEN || '';
const BACKEND_SECRET = process.env.BACKEND_SECRET || '';

const system = `You are Barbie AI, a personal Android assistant. Reply in Roman Urdu by default. Do not use Hindi or Devanagari. If the user clearly speaks English, Urdu, Punjabi, Sindhi or another language, reply in that language, but never Hindi. Be concise. When the user asks for a phone action, explain what Android can do and do not claim an action happened unless the app confirms it.`;

function json(res, code, data) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type, X-Barbie-Secret', 'Access-Control-Allow-Methods': 'GET,POST,OPTIONS' });
  res.end(JSON.stringify(data));
}

function authorized(req) { return !BACKEND_SECRET || req.headers['x-barbie-secret'] === BACKEND_SECRET; }

async function provider(url, key, model, message) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model, messages: [{ role: 'system', content: system }, { role: 'user', content: message }], temperature: 0.3 })
  });
  const raw = await r.text();
  if (!r.ok) throw new Error(`provider_${r.status}`);
  const data = JSON.parse(raw);
  return data?.choices?.[0]?.message?.content?.trim() || '';
}

async function chat(message) {
  if (OPENROUTER_KEY) {
    try {
      const reply = await provider('https://openrouter.ai/api/v1/chat/completions', OPENROUTER_KEY, 'openrouter/free', message);
      if (reply) return { reply, provider: 'openrouter' };
    } catch {}
  }
  if (HF_TOKEN) {
    try {
      const reply = await provider('https://router.huggingface.co/v1/chat/completions', HF_TOKEN, 'openai/gpt-oss-120b:fastest', message);
      if (reply) return { reply, provider: 'huggingface' };
    } catch {}
  }
  return { reply: 'AI provider abhi configured nahi hai. API key baad mein backend secret mein add ki ja sakti hai.', provider: 'none' };
}

function detectAction(message) {
  const x = message.toLowerCase();
  if (x.includes('youtube')) return { type: 'youtube_search', query: message.replace(/youtube/ig, '').trim() };
  if (x.includes('whatsapp')) return { type: 'whatsapp' };
  if (x.includes('call') || x.includes('phone')) return { type: 'call' };
  if (x.includes('file') || x.includes('document')) return { type: 'file_picker' };
  return { type: 'chat' };
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true, name: 'Barbie AI', version: '2.0' });
  if (!authorized(req)) return json(res, 401, { error: 'unauthorized' });
  if (req.method !== 'POST') return json(res, 404, { error: 'not_found' });

  let body = '';
  req.on('data', chunk => { body += chunk; if (body.length > 1000000) req.destroy(); });
  req.on('end', async () => {
    try {
      const data = JSON.parse(body || '{}');
      const message = String(data.message || '').trim();
      if (!message) return json(res, 400, { error: 'message_required' });

      if (req.url === '/api/action') return json(res, 200, { action: detectAction(message) });
      if (req.url !== '/api/chat') return json(res, 404, { error: 'not_found' });

      const result = await chat(message);
      return json(res, 200, { ...result, action: detectAction(message) });
    } catch (e) { return json(res, 500, { error: 'server_error' }); }
  });
});

server.listen(PORT, () => console.log(`Barbie AI backend listening on ${PORT}`));
