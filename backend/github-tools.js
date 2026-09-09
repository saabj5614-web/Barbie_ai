const GITHUB_TOKEN = process.env.GITHUB_TOKEN || '';

function headers() {
  return {
    Authorization: `Bearer ${GITHUB_TOKEN}`,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2026-03-10',
    'Content-Type': 'application/json'
  };
}

export async function githubWriteFile({ owner, repo, path, content, message, branch = 'main', sha }) {
  if (!GITHUB_TOKEN) throw new Error('github_not_configured');
  if (!owner || !repo || !path || !message) throw new Error('github_fields_required');
  const payload = { message, content: Buffer.from(String(content), 'utf8').toString('base64'), branch };
  if (sha) payload.sha = sha;
  const r = await fetch(`https://api.github.com/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contents/${path.split('/').map(encodeURIComponent).join('/')}`, {
    method: 'PUT', headers: headers(), body: JSON.stringify(payload)
  });
  const data = await r.json();
  if (!r.ok) throw new Error(`github_${r.status}`);
  return { path, commit: data?.commit?.sha || null, url: data?.content?.html_url || null };
}
