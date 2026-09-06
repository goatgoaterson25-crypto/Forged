// /api/generate-reference.js
// Image-to-image editing: takes a prompt + a reference photo, uploads the
// photo to a free anonymous host (0x0.st) to get a public URL, then asks
// Pollinations' Kontext model to transform it accordingly. Still no API key,
// no billing — the "hosting" step is the only extra piece vs. plain generate.

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

  const { prompt, imageBase64, mimeType } = req.body || {};

  if (!prompt || typeof prompt !== 'string') {
    return res.status(400).json({ error: 'Missing "prompt" string in request body' });
  }
  if (!imageBase64 || typeof imageBase64 !== 'string') {
    return res.status(400).json({ error: 'Missing "imageBase64" string in request body' });
  }

  try {
    const bytes = Buffer.from(imageBase64, 'base64');
    const blob = new Blob([bytes], { type: mimeType || 'image/jpeg' });
    const form = new FormData();
    form.append('file', blob, 'reference.jpg');

    const uploadRes = await fetch('https://0x0.st', {
      method: 'POST',
      body: form
    });

    if (!uploadRes.ok) {
      return res.status(uploadRes.status).json({ error: 'Failed to host the reference image' });
    }

    const imageUrl = (await uploadRes.text()).trim();

    const encodedPrompt = encodeURIComponent(prompt);
    const genUrl = `https://image.pollinations.ai/prompt/${encodedPrompt}?model=kontext&image=${encodeURIComponent(imageUrl)}&nologo=true`;

    const imgRes = await fetch(genUrl);
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
