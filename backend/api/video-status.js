// /api/video-status.js
// Poll this with ?op=<operationName> (URL-encoded) until it returns done: true.

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Use GET' });
  }

  const { op } = req.query;
  if (!op) {
    return res.status(400).json({ error: 'Missing "op" query param' });
  }

  const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
  if (!GEMINI_API_KEY) {
    return res.status(500).json({ error: 'Server is missing GEMINI_API_KEY env var' });
  }

  try {
    const statusRes = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/${op}`,
      { headers: { 'x-goog-api-key': GEMINI_API_KEY } }
    );

    const data = await statusRes.json();

    if (!statusRes.ok) {
      return res.status(statusRes.status).json({ error: data.error?.message || JSON.stringify(data) });
    }

    if (!data.done) {
      return res.status(200).json({ done: false });
    }

    if (data.error) {
      return res.status(500).json({ done: true, error: data.error.message || 'Generation failed' });
    }

    const videoUri = data.response?.generateVideoResponse?.generatedSamples?.[0]?.video?.uri;
    if (!videoUri) {
      return res.status(500).json({ done: true, error: 'No video found in completed response' });
    }

    // Fetch the actual video bytes (this download URI also requires the API key)
    const videoRes = await fetch(videoUri, { headers: { 'x-goog-api-key': GEMINI_API_KEY } });
    if (!videoRes.ok) {
      return res.status(videoRes.status).json({ done: true, error: 'Failed to download finished video' });
    }
    const buffer = await videoRes.arrayBuffer();
    const base64 = Buffer.from(buffer).toString('base64');

    return res.status(200).json({
      done: true,
      video: `data:video/mp4;base64,${base64}`
    });
  } catch (err) {
    return res.status(500).json({ error: err.message || 'Unknown server error' });
  }
}
