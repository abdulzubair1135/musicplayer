require('dotenv').config();
const express = require('express');
const cors = require('cors');
const compression = require('compression');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const ytSearch = require('yt-search');
const ytdl = require('@distube/ytdl-core');

const app = express();
const PORT = process.env.PORT || 3000;

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(cors());
app.use(compression());
app.use(morgan('combined'));
app.use(express.json());

// Rate limiting – 200 requests per 15 minutes per IP
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/', limiter);

// ─── Helpers ──────────────────────────────────────────────────────────────────
function formatDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function mapVideoToSong(v) {
  return {
    videoId: v.videoId,
    title: v.title,
    artist: v.author?.name || v.author || 'Unknown',
    album: 'YouTube',
    duration: (v.duration?.seconds || 0) * 1000, // ms
    durationFormatted: v.duration?.timestamp || '0:00',
    thumbnail: v.thumbnail ||
      `https://i.ytimg.com/vi/${v.videoId}/hqdefault.jpg`,
    genre: 'Online',
    sourceType: 'YOUTUBE',
  };
}

// Filter out likely non-music results
function filterMusicResults(videos) {
  const excludeKeywords = ['podcast', 'interview', 'lecture', 'news', 'debate',
    'documentary', 'gameplay', 'tutorial', 'review', 'unboxing'];
  return videos.filter(v => {
    const title = (v.title || '').toLowerCase();
    const hasBadKeyword = excludeKeywords.some(kw => title.includes(kw));
    const duration = v.duration?.seconds || 0;
    // Keep between 1 min and 12 min
    return !hasBadKeyword && duration >= 60 && duration <= 720;
  });
}

// ─── Routes ───────────────────────────────────────────────────────────────────

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString(), version: '1.0.0' });
});

// ── Search ──
// GET /api/search?q=shape+of+you&limit=20
app.get('/api/search', async (req, res) => {
  const query = req.query.q;
  const limit = Math.min(parseInt(req.query.limit) || 20, 50);

  if (!query || query.trim().length < 2) {
    return res.status(400).json({ error: 'Query too short. Minimum 2 characters.' });
  }

  try {
    const result = await ytSearch({ query: query.trim() + ' music', pageStart: 1 });
    const videos = result.videos || [];
    const filtered = filterMusicResults(videos).slice(0, limit);

    const songs = filtered.map(mapVideoToSong);
    res.json({ results: songs, total: songs.length, query });
  } catch (err) {
    console.error('Search error:', err.message);
    res.status(500).json({ error: 'Search failed. Try again.' });
  }
});

// ── Stream Audio ──
// GET /api/stream/:videoId
app.get('/api/stream/:videoId', async (req, res) => {
  const { videoId } = req.params;

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }

  try {
    const url = `https://www.youtube.com/watch?v=${videoId}`;

    // Fetch info first to get content length for range support
    const info = await ytdl.getInfo(url);
    const format = ytdl.chooseFormat(info.formats, {
      quality: 'highestaudio',
      filter: 'audioonly',
    });

    if (!format) {
      return res.status(404).json({ error: 'No audio format available.' });
    }

    const contentLength = format.contentLength;
    const rangeHeader = req.headers.range;

    res.setHeader('Content-Type', 'audio/mpeg');
    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('X-Content-Type-Options', 'nosniff');

    if (rangeHeader && contentLength) {
      const [startStr, endStr] = rangeHeader.replace('bytes=', '').split('-');
      const start = parseInt(startStr, 10);
      const end = endStr ? parseInt(endStr, 10) : parseInt(contentLength) - 1;
      const chunkSize = end - start + 1;

      res.setHeader('Content-Range', `bytes ${start}-${end}/${contentLength}`);
      res.setHeader('Content-Length', chunkSize);
      res.status(206);

      ytdl(url, {
        format,
        range: { start, end },
      }).pipe(res);
    } else {
      if (contentLength) res.setHeader('Content-Length', contentLength);
      res.status(200);
      ytdl(url, { format }).pipe(res);
    }

  } catch (err) {
    console.error('Stream error:', err.message);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Streaming failed. Video may be unavailable.' });
    }
  }
});

// ── Direct Download Endpoint ──
// GET /api/download/:videoId
app.get('/api/download/:videoId', async (req, res) => {
  const { videoId } = req.params;

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }

  try {
    const url = `https://www.youtube.com/watch?v=${videoId}`;
    const info = await ytdl.getInfo(url);
    const format = ytdl.chooseFormat(info.formats, {
      quality: 'highestaudio',
      filter: 'audioonly',
    });

    if (!format) {
      return res.status(404).json({ error: 'No audio format available.' });
    }

    const title = (info.videoDetails?.title || videoId).replace(/[^a-zA-Z0-9_-]/g, '_');
    res.setHeader('Content-Disposition', `attachment; filename="${title}.m4a"`);
    res.setHeader('Content-Type', 'audio/mp4');

    if (format.contentLength) {
      res.setHeader('Content-Length', format.contentLength);
    }

    ytdl(url, { format }).pipe(res);
  } catch (err) {
    console.error('Download error:', err.message);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Download failed.' });
    }
  }
});

// ── Get Song Info (for download URL) ──
// GET /api/info/:videoId
app.get('/api/info/:videoId', async (req, res) => {
  const { videoId } = req.params;

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }

  try {
    const url = `https://www.youtube.com/watch?v=${videoId}`;
    const info = await ytdl.getInfo(url);
    const format = ytdl.chooseFormat(info.formats, {
      quality: 'highestaudio',
      filter: 'audioonly',
    });

    const details = info.videoDetails;
    res.json({
      videoId,
      title: details.title,
      artist: details.author?.name || 'Unknown',
      duration: parseInt(details.lengthSeconds) * 1000,
      thumbnail: details.thumbnails?.slice(-1)[0]?.url || '',
      streamUrl: `/api/stream/${videoId}`,
      bitrate: format?.bitrate || 0,
    });
  } catch (err) {
    console.error('Info error:', err.message);
    res.status(500).json({ error: 'Could not fetch song info.' });
  }
});

// ── Trending ──
// GET /api/trending?genre=pop
app.get('/api/trending', async (req, res) => {
  const genre = req.query.genre || 'top hits 2024';
  try {
    const result = await ytSearch({ query: `${genre} best songs 2024 music` });
    const videos = filterMusicResults(result.videos || []).slice(0, 15);
    res.json({ results: videos.map(mapVideoToSong), genre });
  } catch (err) {
    console.error('Trending error:', err.message);
    res.status(500).json({ error: 'Could not fetch trending.' });
  }
});

// ── Discover by genre ──
// GET /api/discover?genre=lofi
const DISCOVER_GENRES = [
  'lofi hip hop chill beats',
  'top bollywood songs 2024',
  'trending pop hits 2024',
  'best indie music 2024',
  'arabic pop music 2024',
];

app.get('/api/discover', async (req, res) => {
  const query = req.query.genre
    ? `${req.query.genre} music popular 2024`
    : DISCOVER_GENRES[Math.floor(Math.random() * DISCOVER_GENRES.length)];

  try {
    const result = await ytSearch({ query });
    const videos = filterMusicResults(result.videos || []).slice(0, 10);
    res.json({ results: videos.map(mapVideoToSong) });
  } catch (err) {
    console.error('Discover error:', err.message);
    res.status(500).json({ error: 'Could not fetch discover.' });
  }
});

// ── Radio (related songs) ──
// GET /api/radio?videoId=xxx&title=song+name&artist=artist+name
app.get('/api/radio', async (req, res) => {
  const { title, artist } = req.query;
  const query = title ? `${title} ${artist || ''} similar songs` : 'top music mix 2024';

  try {
    const result = await ytSearch({ query: query.trim() + ' music' });
    const videos = filterMusicResults(result.videos || []).slice(0, 20);
    res.json({ results: videos.map(mapVideoToSong) });
  } catch (err) {
    console.error('Radio error:', err.message);
    res.status(500).json({ error: 'Could not generate radio.' });
  }
});

// ─── 404 Handler ─────────────────────────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found.' });
});

// ─── Global error handler ─────────────────────────────────────────────────────
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error.' });
});

// ─── Start ────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log(`🎵 Z-Music Backend running on port ${PORT}`);
  console.log(`   Health: http://localhost:${PORT}/api/health`);
  console.log(`   Search: http://localhost:${PORT}/api/search?q=shape+of+you`);
  console.log(`   Stream: http://localhost:${PORT}/api/stream/:videoId`);
});
