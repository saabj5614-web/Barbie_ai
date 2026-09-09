import { config } from '../core/config.js';

export async function openRouterChat(messages, model = 'openrouter/free') {
  if (!config.openRouterKey) throw new Error('openrouter_not_configured');
  const response = await fetch('https://openrouter.ai/api/v1/chat/completions', {
    method: 'POST',
    headers: { Authorization: `Bearer ${config.openRouterKey}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model, messages, temperature: 0.2 })
  });
  const raw = await response.text();
  if (!response.ok) throw new Error(`openrouter_${response.status}`);
  const data = JSON.parse(raw);
  return data?.choices?.[0]?.message?.content?.trim() || '';
}
