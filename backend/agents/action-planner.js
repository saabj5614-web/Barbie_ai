import { plannerSystem } from '../core/prompts.js';
import { chatWithFallback } from '../providers/provider-manager.js';

function parseJson(text) {
  try { return JSON.parse(text); } catch {}
  const match = String(text).match(/\{[\s\S]*\}/);
  if (!match) return null;
  try { return JSON.parse(match[0]); } catch { return null; }
}

const allowed = new Set(['none','youtube_search','web_search','open_whatsapp','call_number','open_files','screen_share','back','home','click_text','github_request']);

export async function planAction(message) {
  const result = await chatWithFallback(message, plannerSystem);
  const parsed = parseJson(result.reply);
  if (!parsed?.reply) return { reply: result.reply, action: { type: 'none' }, provider: result.provider };
  const action = parsed.action && typeof parsed.action === 'object' ? parsed.action : { type: 'none' };
  if (!allowed.has(String(action.type))) action.type = 'none';
  return { reply: String(parsed.reply), action, provider: result.provider };
}
