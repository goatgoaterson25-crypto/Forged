// /api/generate.js
// Vercel serverless function — proxies image prompts to Pollinations.ai,
// a genuinely free, no-key, no-signup image generation API.

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(204).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Use POST' });
  }

  const { prompt, width, height } = req.body || {};

  if (!prompt || typeof prompt !== 'string') {
    return res.status(400).json({ error: 'Missing "prompt" string in request body' });
  }

  try {
    const encodedPrompt = encodeURIComponent(prompt);
    const w = width || 1024;
    const h = height || 1024;
    const seed = Math.floor(Math.random() * 1e9);

    const url = `https://image.pollinations.ai/prompt/${encodedPrompt}?width=${w}&height=${h}&seed=${seed}&nologo=true`;

    const imgRes = await fetch(url);

    if (!imgRes.ok) {
      return res.status(imgRes.status).json({ error: `Pollinations returned ${imgRes.status}` });
    }

    const arrayBuffer = await imgRes.arrayBuffer();
    const base64 = Buffer.from(arrayBuffer).toString('base64');
    const contentType = imgRes.headers.get('content-type') || 'image/jpeg';

    return res.status(200).json({
      image: `data:${contentType};base64,${base64}`
    });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Unknown server error' });
  }
}
