import http from 'node:http';

const PORT = Number(process.env.PORT || 8080);
const OPENROUTER_KEY = process.env.OPENROUTER_API_KEY || '';
const HF_TOKEN = process.env.HUGGINGFACE_TOKEN || '';

const system = `You are Barbie AI. Reply in Roman Urdu by default. Do not use Hindi/Devanagari. If the user clearly speaks English, Urdu, Punjabi, Sindhi or another language, you may reply in that language, but never Hindi. Be concise, helpful and safe.`;

function json(res, code, data) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type', 'Access-Control-Allow-Methods': 'POST,OPTIONS' });
  res.end(JSON.stringify(data));
}

async function provider(url, key, model, message) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model, messages: [{ role: 'system', content: system }, { role: 'user', content: message }], temperature: 0.4 })
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
      const reply = await provider('https://router.huggingface.co/v1/chat/completions', HF_TOKEN, 'meta-llama/Llama-3.1-8B-Instruct:novita', message);
      if (reply) return { reply, provider: 'huggingface' };
    } catch {}
  }
  return { reply: 'AI provider abhi configured nahi hai. Backend mein free provider ki key add karni hogi.', provider: 'none' };
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true, name: 'Barbie AI' });
  if (req.method !== 'POST' || req.url !== '/api/chat') return json(res, 404, { error: 'not_found' });
  let body = '';
  req.on('data', chunk => { body += chunk; if (body.length > 1000000) req.destroy(); });
  req.on('end', async () => {
    try {
      const data = JSON.parse(body || '{}');
      const message = String(data.message || '').trim();
      if (!message) return json(res, 400, { error: 'message_required' });
      const result = await chat(message);
      return json(res, 200, result);
    } catch (e) { return json(res, 500, { error: 'server_error' }); }
  });
});

server.listen(PORT, () => console.log(`Barbie AI backend listening on ${PORT}`));
