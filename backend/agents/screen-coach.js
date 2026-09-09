import { screenSystem } from '../core/prompts.js';
import { visionWithFallback } from '../providers/provider-manager.js';

export async function analyzeScreen(imageData, prompt) {
  if (!imageData) return { reply: 'Screen image nahi mili.', provider: 'none' };
  return visionWithFallback(imageData, prompt, screenSystem);
}
