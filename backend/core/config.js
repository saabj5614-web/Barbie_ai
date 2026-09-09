export const config = {
  port: Number(process.env.PORT || 8080),
  openRouterKey: process.env.OPENROUTER_API_KEY || '',
  huggingFaceToken: process.env.HUGGINGFACE_TOKEN || '',
  backendSecret: process.env.BACKEND_SECRET || '',
  githubOwner: process.env.GITHUB_OWNER || '',
  githubRepo: process.env.GITHUB_REPO || ''
};
