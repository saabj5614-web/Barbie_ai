import { config, barbieSystemPrompt } from './config.js';
import { openRouterChat } from '../providers/openrouter.js';
import { huggingFaceChat } from '../providers/huggingface.js';

export async function chatWithFallback(messages) {
  const attempts = [];
  if (config.openRouterKey) attempts.push(() => openRouterChat(messages));
  if (config.huggingFaceToken) attempts.push(() => huggingFaceChat(messages));
  let lastError;
  for (const attempt of attempts) {
    try {
      const result = await attempt();
      if (result) return result;
    } catch (error) { lastError = error; }
  }
  throw lastError || new Error('no_provider_configured');
}

export function userMessage(text) {
  return [{ role: 'system', content: barbieSystemPrompt }, { role: 'user', content: text }];
}
