import { openRouterChat } from './openrouter.js';
import { huggingFaceChat } from './huggingface.js';
import { baseSystem } from '../core/prompts.js';

export async function chatWithFallback(userMessage, system = baseSystem) {
  const messages = [{ role: 'system', content: system }, { role: 'user', content: userMessage }];
  const attempts = [
    ['openrouter', () => openRouterChat(messages)],
    ['huggingface', () => huggingFaceChat(messages)]
  ];
  for (const [name, run] of attempts) {
    try { const reply = await run(); if (reply) return { reply, provider: name }; } catch {}
  }
  return { reply: 'AI provider abhi available nahi hai.', provider: 'none' };
}

export async function visionWithFallback(imageData, prompt, system) {
  const content = [{ type: 'text', text: prompt || 'Is screenshot mein kya nazar aa raha hai?' }, { type: 'image_url', image_url: { url: `data:image/jpeg;base64,${imageData}` } }];
  const messages = [{ role: 'system', content: system }, { role: 'user', content }];
  const attempts = [
    ['openrouter', () => openRouterChat(messages)],
    ['huggingface', () => huggingFaceChat(messages)]
  ];
  for (const [name, run] of attempts) {
    try { const reply = await run(); if (reply) return { reply, provider: name }; } catch {}
  }
  return { reply: 'Screen analysis ke liye provider available nahi hai.', provider: 'none' };
}
