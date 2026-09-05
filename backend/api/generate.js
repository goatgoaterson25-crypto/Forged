// /api/generate.js
// Vercel serverless function — the only place your API key lives.
// The Android app calls this endpoint; this function calls Hugging Face.

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Use POST' });
  }

  const { prompt, negative_prompt, width, height, steps } = req.body || {};

  if (!prompt || typeof prompt !== 'string') {
    return res.status(400).json({ error: 'Missing "prompt" string in request body' });
  }

  const HF_API_KEY = process.env.HF_API_KEY;
  if (!HF_API_KEY) {
    return res.status(500).json({ error: 'Server is missing HF_API_KEY env var' });
  }

  // Free-tier friendly model. Swap for a different HF model id if you prefer.
  const MODEL = process.env.HF_MODEL || 'stabilityai/stable-diffusion-xl-base-1.0';

  try {
    const hfRes = await fetch(`https://api-inference.huggingface.co/models/${MODEL}`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${HF_API_KEY}`,
        'Content-Type': 'application/json',
        Accept: 'image/png'
      },
      body: JSON.stringify({
        inputs: prompt,
        parameters: {
          negative_prompt: negative_prompt || undefined,
          width: width || 1024,
          height: height || 1024,
          num_inference_steps: steps || 25
        }
      })
    });

    if (!hfRes.ok) {
      const text = await hfRes.text();
      // HF returns JSON errors (e.g. model loading, rate limit) rather than an image on failure.
      let message = text;
      try {
        const asJson = JSON.parse(text);
        message = asJson.error || text;
      } catch (_) {}
      return res.status(hfRes.status).json({ error: message });
    }

    const arrayBuffer = await hfRes.arrayBuffer();
    const base64 = Buffer.from(arrayBuffer).toString('base64');

    return res.status(200).json({
      image: `data:image/png;base64,${base64}`
    });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Unknown server error' });
  }
}
