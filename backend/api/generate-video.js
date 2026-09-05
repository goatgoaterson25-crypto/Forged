// /api/generate-video.js
// Kicks off a Veo 3.1 video generation job. Returns an operation name
// immediately — video generation takes 1-6 minutes, too long for a single
// serverless request, so the client polls /api/video-status with this ID.

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Use POST' });
  }

  const { prompt } = req.body || {};
  if (!prompt || typeof prompt !== 'string') {
    return res.status(400).json({ error: 'Missing "prompt" string in request body' });
  }

  const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
  if (!GEMINI_API_KEY) {
    return res.status(500).json({ error: 'Server is missing GEMINI_API_KEY env var' });
  }

  const MODEL = process.env.VEO_MODEL || 'veo-3.1-generate-preview';

  try {
    const startRes = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:predictLongRunning`,
      {
        method: 'POST',
        headers: {
          'x-goog-api-key': GEMINI_API_KEY,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          instances: [{ prompt }]
        })
      }
    );

    const data = await startRes.json();

    if (!startRes.ok) {
      return res.status(startRes.status).json({ error: data.error?.message || JSON.stringify(data) });
    }

    // data.name looks like "models/veo-3.1-generate-preview/operations/xxxxx"
    return res.status(200).json({ operationName: data.name });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Unknown server error' });
  }
}
