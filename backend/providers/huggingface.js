import { config } from '../core/config.js';

export async function huggingFaceChat(messages, model = 'openai/gpt-oss-120b:fastest') {
  if (!config.huggingFaceToken) throw new Error('huggingface_not_configured');
  const response = await fetch('https://router.huggingface.co/v1/chat/completions', {
    method: 'POST',
    headers: { Authorization: `Bearer ${config.huggingFaceToken}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model, messages, temperature: 0.2 })
  });
  const raw = await response.text();
  if (!response.ok) throw new Error(`huggingface_${response.status}`);
  const data = JSON.parse(raw);
  return data?.choices?.[0]?.message?.content?.trim() || '';
}
