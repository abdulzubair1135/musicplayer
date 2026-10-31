require('dotenv').config();
const express = require('express');
const cors = require('cors');
const compression = require('compression');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const ytSearch = require('yt-search');
const { Ytdl, chooseFormat, Client } = require('@distube/ytdl-core');
const ytdl = new Ytdl({
  clients: [Client.VisionOS, Client.Tv, Client.WebEmbedded]
});

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

// Filter out non-music and lengthy compilation/mashup videos
function filterMusicResults(videos, options = {}) {
  const excludeKeywords = [
    'podcast', 'interview', 'lecture', 'news', 'debate',
    'documentary', 'gameplay', 'tutorial', 'unboxing',
    'mashup', 'jukebox', '100 songs', 'top 50', 'top 100', 'top 20', 'top 30', 'top 40',
    'full album', 'compilation', 'non stop', 'non-stop', 'dj mix', 'megamix', 'hour loop',
    'playlist', 'all songs', 'audio jukebox', 'greatest hits', 'most streamed', 'songs that own'
  ];
  const maxDuration = options.maxDuration || (options.allowLong ? 10800 : 300); // 5 mins (300s) default!
  const minDuration = options.minDuration || 35;  // min 35 secs

  return videos.filter(v => {
    const title = (v.title || '').toLowerCase();
    const hasBadKeyword = excludeKeywords.some(kw => title.includes(kw));
    const duration = v.duration?.seconds || 0;
    if (options.allowLong) {
      return !['podcast', 'interview', 'lecture', 'news', 'gameplay'].some(kw => title.includes(kw));
    }
    return !hasBadKeyword && duration >= minDuration && duration <= maxDuration;
  });
}

// ─── Routes ───────────────────────────────────────────────────────────────────

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    version: '1.0.0',
    nodeVersion: process.version,
    platform: process.platform
  });
});

// Diagnostic route
app.get('/api/test-ytdl', async (req, res) => {
  const id = req.query.id || 'mZQH8CPQ-wo';
  const steps = [];
  const start = Date.now();
  try {
    steps.push({ step: 'start', elapsed: Date.now() - start });
    const info = await ytdl.getInfo(id);
    steps.push({ step: 'got_info', elapsed: Date.now() - start, formats: info?.formats?.length || 0 });
    const audioFormats = (info?.formats || []).filter(f => f.hasAudio && !f.hasVideo && f.url);
    steps.push({ step: 'audio_formats', count: audioFormats.length, sampleUrl: audioFormats[0]?.url?.substring(0, 50) });
    res.json({ ok: true, elapsed: Date.now() - start, steps });
  } catch (err) {
    res.status(500).json({ ok: false, elapsed: Date.now() - start, error: err.message, stack: err.stack, steps });
  }
});

// In-memory cache for ultra-fast searches and zero CPU waste on Render
const searchCache = new Map();
const SEARCH_CACHE_TTL = 15 * 60 * 1000; // 15 minutes

function getCachedSearch(key) {
  const item = searchCache.get(key);
  if (item && (Date.now() - item.time < SEARCH_CACHE_TTL)) {
    return item.data;
  }
  return null;
}

function setCachedSearch(key, data) {
  if (searchCache.size > 300) {
    const oldest = searchCache.keys().next().value;
    searchCache.delete(oldest);
  }
  searchCache.set(key, { data, time: Date.now() });
}

// ── Search with Pagination & Duration Filter ──
// GET /api/search?q=shape+of+you&page=1&limit=30&allowLong=false
app.get('/api/search', async (req, res) => {
  const query = req.query.q;
  const page = Math.max(parseInt(req.query.page) || 1, 1);
  const limit = Math.min(parseInt(req.query.limit) || 30, 50);
  const allowLong = req.query.allowLong === 'true';
  const maxDuration = allowLong ? 10800 : 360; // 6 mins default!

  if (!query || query.trim().length < 2) {
    return res.status(400).json({ error: 'Query too short. Minimum 2 characters.' });
  }

  const cleanQ = query.trim();
  const cacheKey = `${cleanQ.toLowerCase()}_p${page}_l${allowLong}`;
  const cached = getCachedSearch(cacheKey);
  if (cached) {
    return res.json({ results: cached, total: cached.length, query, page, cached: true });
  }

  try {
    const wantsKaraoke = cleanQ.toLowerCase().includes('karaoke');
    const wantsInstrumental = cleanQ.toLowerCase().includes('instrumental');

    // Exclude compilation/mashup keywords
    const excludeKeywords = [
      'podcast', 'interview', 'lecture', 'news', 'gameplay',
      'mashup', 'jukebox', '100 songs', 'top 50', 'top 100', 'top 20', 'top 30', 'top 40',
      'full album', 'compilation', 'non stop', 'non-stop', 'dj mix', 'megamix', 'hour loop',
      'playlist', 'all songs', 'audio jukebox', 'greatest hits', 'most streamed', 'songs that own'
    ];

    const cleanSubject = cleanQ.replace(/hit songs|hits|best songs|all songs|top songs|jukebox|mix|songs|song|audio/gi, '').trim() || cleanQ;
    const lowerQ = cleanQ.toLowerCase();

    // Determine the optimal single primary search query
    let primaryQuery = cleanQ;
    if (lowerQ.includes('phonk') || lowerQ.includes('funk')) {
      primaryQuery = page === 1 ? 'brazilian phonk viral audio' : 'montagem phonk ultra slowed';
    } else if (lowerQ.includes('hit songs') || lowerQ.includes('top hits') || lowerQ.includes('best songs')) {
      primaryQuery = `${cleanSubject} official song`;
    } else if (page > 1) {
      primaryQuery = `${cleanSubject} song official audio`;
    }

    const firstResult = await ytSearch({ query: primaryQuery }).catch(() => ({ videos: [] }));
    let candidateVideos = [...(firstResult.videos || [])];

    // If first search returned fewer than 10 candidates, fetch one targeted fallback
    if (candidateVideos.length < 10) {
      const fallbackQuery = (lowerQ.includes('phonk') || lowerQ.includes('funk'))
        ? 'phonk slow reverb audio'
        : `${cleanSubject} lyrical video song`;
      const secondResult = await ytSearch({ query: fallbackQuery }).catch(() => ({ videos: [] }));
      if (secondResult && secondResult.videos) {
        candidateVideos.push(...secondResult.videos);
      }
    }

    // Deduplicate and filter out non-music and compilation junk
    const seenIds = new Set();
    const chosenVideos = [];
    for (const v of candidateVideos) {
      if (!v || !v.videoId || seenIds.has(v.videoId)) continue;
      seenIds.add(v.videoId);

      const titleLower = (v.title || '').toLowerCase();
      if (!allowLong && excludeKeywords.some(kw => titleLower.includes(kw))) continue;
      if (!wantsKaraoke && (titleLower.includes('karaoke') || titleLower.includes('scrolling lyrics'))) continue;
      if (!wantsInstrumental && (titleLower.includes('instrumental') || titleLower.includes('without music') || titleLower.includes('backing track'))) continue;
      if (titleLower.includes('whatsapp status') || titleLower.includes('8k status') || titleLower.includes('status video')) continue;

      const dur = v.duration?.seconds || 0;
      if (dur < 30 || dur > maxDuration) continue;

      chosenVideos.push(v);
      if (chosenVideos.length >= limit) break;
    }

    const songs = chosenVideos.map(mapVideoToSong);
    setCachedSearch(cacheKey, songs);
    res.json({ results: songs, total: songs.length, query, page });
  } catch (err) {
    console.error('Search error:', err.message);
    res.status(500).json({ error: 'Search failed. Try again.' });
  }
});

// ── Synced Lyrics ──
// GET /api/lyrics?title=shape+of+you&artist=ed+sheeran
app.get('/api/lyrics', async (req, res) => {
  const { title, artist, duration } = req.query;
  if (!title) {
    return res.status(400).json({ error: 'Title required' });
  }

  // Clean title: remove "Official Video", "(Lyrics)", etc.
  const cleanTitle = title
    .replace(/\(.*?\)|\[.*?\]/g, '')
    .replace(/official|music|video|audio|lyrics|feat\..*|ft\..*/gi, '')
    .trim();

  const cleanArtist = (artist || '')
    .replace(/\(.*?\)|\[.*?\]/g, '')
    .replace(/vevo|topic|records/gi, '')
    .trim();

  try {
    const params = new URLSearchParams({ track_name: cleanTitle });
    if (cleanArtist && cleanArtist !== 'Unknown') params.append('artist_name', cleanArtist);
    if (duration) params.append('duration', Math.round(duration / 1000));

    const response = await fetch(`https://lrclib.net/api/get?${params.toString()}`, {
      headers: { 'User-Agent': 'Z-Music/1.0 (https://github.com/abdulzubair1135/musicplayer)' }
    });

    if (response.ok) {
      const data = await response.json();
      return res.json({
        syncedLyrics: data.syncedLyrics || null,
        plainLyrics: data.plainLyrics || null,
        instrumental: data.instrumental || false
      });
    }

    // Fallback search
    const searchParams = new URLSearchParams({ q: `${cleanTitle} ${cleanArtist}`.trim() });
    const searchRes = await fetch(`https://lrclib.net/api/search?${searchParams.toString()}`, {
      headers: { 'User-Agent': 'Z-Music/1.0 (https://github.com/abdulzubair1135/musicplayer)' }
    });

    if (searchRes.ok) {
      const results = await searchRes.json();
      const match = results.find(r => r.syncedLyrics) || results[0];
      if (match) {
        return res.json({
          syncedLyrics: match.syncedLyrics || null,
          plainLyrics: match.plainLyrics || null,
          instrumental: match.instrumental || false
        });
      }
    }

    res.status(404).json({ error: 'No lyrics found' });
  } catch (err) {
    console.error('Lyrics fetch error:', err.message);
    res.status(500).json({ error: 'Failed to fetch lyrics' });
  }
});

const https = require('https');
const http = require('http');

// In-memory cache for audio streaming URLs (15 mins TTL)
const streamUrlCache = new Map();

// ── Stream Audio ──
// GET /api/stream/:videoId
app.get('/api/stream/:videoId', async (req, res) => {
  const { videoId } = req.params;

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }

  // High-speed audio stream endpoint (itag 140 = 128kbps AAC M4A audio)
  const streamEndpoint = `https://yt.chocolatemoo53.com/latest_version?id=${videoId}&itag=140`;

  if (req.query.proxy !== 'true') {
    return res.redirect(302, streamEndpoint);
  }

  // Fallback proxy if explicitly requested
  const proxyHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
  };
  if (req.headers.range) {
    proxyHeaders['Range'] = req.headers.range;
  }

  const followRedirect = (url, depth = 0) => {
    if (depth > 5) {
      if (!res.headersSent) res.status(500).json({ error: 'Too many redirects' });
      return;
    }
    const client = url.startsWith('https') ? https : http;
    const proxyReq = client.get(url, { headers: proxyHeaders }, (proxyRes) => {
      if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
        return followRedirect(proxyRes.headers.location, depth + 1);
      }
      res.writeHead(proxyRes.statusCode, {
        'Content-Type': proxyRes.headers['content-type'] || 'audio/mp4',
        'Accept-Ranges': 'bytes',
        'Access-Control-Allow-Origin': '*',
        'Cache-Control': 'public, max-age=3600',
      });
      proxyRes.pipe(res);
    });
    proxyReq.on('error', (err) => {
      if (!res.headersSent) res.status(500).json({ error: err.message });
    });
    req.on('close', () => proxyReq.destroy());
  };

  followRedirect(streamEndpoint);
});

// ── Direct Download Endpoint ──
// GET /api/download/:videoId
app.get('/api/download/:videoId', async (req, res) => {
  const { videoId } = req.params;
  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }
  const streamEndpoint = `https://yt.chocolatemoo53.com/latest_version?id=${videoId}&itag=140`;
  res.redirect(302, streamEndpoint);
});

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return res.status(400).json({ error: 'Invalid video ID.' });
  }

  try {
    const info = await ytdl.getInfo(videoId);
    const audioFormats = info.formats.filter(f => f.hasAudio && !f.hasVideo && f.url);
    const format = chooseFormat(audioFormats, { quality: 'highestaudio' }) || audioFormats[0];

    if (!format || !format.url) {
      return res.status(404).json({ error: 'No audio format available.' });
    }

    const title = (info.videoDetails?.title || videoId).replace(/[^a-zA-Z0-9_-]/g, '_');
    const ext = format.mimeType?.includes('mp4') ? 'm4a' : 'webm';
    const client = format.url.startsWith('https') ? https : http;

    res.setHeader('Content-Disposition', `attachment; filename="${title}.${ext}"`);
    res.setHeader('Content-Type', format.mimeType?.split(';')[0] || 'audio/mp4');

    if (format.contentLength) {
      res.setHeader('Content-Length', format.contentLength);
    }

    const proxyReq = client.get(format.url, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' },
    }, (proxyRes) => {
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
      console.error('Download stream error:', err.message);
      if (!res.headersSent) {
        res.status(500).end();
      }
    });

    req.on('close', () => {
      proxyReq.destroy();
    });

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
// GET /api/trending?genre=pop&allowLong=false
app.get('/api/trending', async (req, res) => {
  const genre = req.query.genre;
  const allowLong = req.query.allowLong === 'true';
  const query = genre
    ? `${genre} songs official video`
    : 'trending songs 2024 official music video';
  try {
    const result = await ytSearch({ query });
    const videos = filterMusicResults(result.videos || [], { allowLong }).slice(0, 15);
    res.json({ results: videos.map(mapVideoToSong), genre: genre || 'trending' });
  } catch (err) {
    console.error('Trending error:', err.message);
    res.status(500).json({ error: 'Could not fetch trending.' });
  }
});

// ── Discover by genre ──
// GET /api/discover?genre=lofi&allowLong=false
const DISCOVER_GENRES = [
  'arijit singh official song',
  'diljit dosanjh official music video',
  'shreya ghoshal official song',
  'ap dhillon official song',
  'the weeknd official music video',
  'ed sheeran official audio',
  'atif aslam official song',
  'taylor swift official music video',
  'sidhu moose wala official song',
  'alan walker official music video',
];

app.get('/api/discover', async (req, res) => {
  const allowLong = req.query.allowLong === 'true';
  const query = req.query.genre
    ? `${req.query.genre} official song`
    : DISCOVER_GENRES[Math.floor(Math.random() * DISCOVER_GENRES.length)];

  try {
    const result = await ytSearch({ query });
    let videos = filterMusicResults(result.videos || [], { allowLong });
    // If strict filter yielded less than 4, take any individual track <= maxDuration
    if (videos.length < 4) {
      const maxDuration = allowLong ? 10800 : 300;
      videos = (result.videos || []).filter(v => (v.duration?.seconds || 0) <= maxDuration && (v.duration?.seconds || 0) >= 30);
    }
    res.json({ results: videos.slice(0, 15).map(mapVideoToSong) });
  } catch (err) {
    console.error('Discover error:', err.message);
    res.status(500).json({ error: 'Could not fetch discover.' });
  }
});

// ── Radio (smart related songs matching same vibe and genre) ──
// GET /api/radio?videoId=xxx&title=song+name&artist=artist+name&allowLong=false
app.get('/api/radio', async (req, res) => {
  const { title, artist } = req.query;
  const allowLong = req.query.allowLong === 'true';
  const maxDuration = allowLong ? 10800 : 300; // 5 mins default!

  const cleanTitle = (title || '').toLowerCase().replace(/\(.*?\)|\[.*?\]/g, '').trim();
  const cleanArtist = (artist || '').toLowerCase().replace(/\(.*?\)|\[.*?\]/g, '').trim();

  // Smart multi-query mood & genre classification
  let radioQueries = [];
  if (cleanTitle.includes('phonk') || cleanArtist.includes('phonk') || cleanTitle.includes('funk')) {
    radioQueries = [
      'brazilian phonk slowed',
      'montagem phonk',
      'phonk viral audio',
      'kordhell murder in my mind',
      'dvrst close eyes'
    ];
  } else if (cleanTitle.includes('lofi') || cleanArtist.includes('lofi') || cleanTitle.includes('chill')) {
    radioQueries = [
      `${cleanArtist || cleanTitle} lofi chill beats`,
      'hindi lofi chill beats official',
      'lofi hip hop beats'
    ];
  } else if (
    cleanArtist.includes('arijit') || cleanTitle.includes('khairiyat') ||
    cleanTitle.includes('sad') || cleanTitle.includes('dard') ||
    cleanTitle.includes('channa') || cleanTitle.includes('judai') ||
    cleanTitle.includes('tujhe kitna') || cleanTitle.includes('agar tum')
  ) {
    radioQueries = [
      'arijit singh official song',
      'bollywood sad romantic hit song official video',
      'arijit singh love songs official',
      'mohit chauhan romantic hits official',
      'pritam emotional hit songs'
    ];
  } else if (
    cleanArtist.includes('punjabi') || cleanArtist.includes('aujla') ||
    cleanArtist.includes('diljit') || cleanArtist.includes('sidhu') ||
    cleanArtist.includes('dhillon') || cleanArtist.includes('shubh')
  ) {
    radioQueries = [
      'punjabi hit songs official audio',
      'diljit dosanjh hit songs official',
      'ap dhillon official songs',
      'karan aujla official audio',
      'sidhu moose wala hit songs'
    ];
  } else if (cleanArtist && cleanArtist !== 'unknown') {
    radioQueries = [
      `${cleanArtist} hit songs official audio`,
      `${cleanArtist} popular music video`,
      `${cleanTitle} similar hit songs official`
    ];
  } else {
    radioQueries = [
      `${cleanTitle} similar hit songs official`,
      'trending bollywood hit songs official'
    ];
  }

  try {
    const results = await Promise.all(radioQueries.map(q => ytSearch({ query: q }).catch(() => ({ videos: [] }))));
    const allVideos = [];
    for (const r of results) {
      if (r && r.videos) allVideos.push(...r.videos);
    }

    // Extract seed keywords to strictly exclude the seed song itself and its duplicate remixes
    const seedWords = cleanTitle.split(/\s+/).filter(w => w.length >= 3 && !['song', 'official', 'video', 'audio', 'full'].includes(w));
    const seenIds = new Set();
    const candidateSongs = [];

    for (const v of allVideos) {
      if (seenIds.has(v.videoId)) continue;
      seenIds.add(v.videoId);

      const t = (v.title || '').toLowerCase();
      // Strictly exclude seed song itself (e.g. if seed is Khairiyat, do not include any Khairiyat versions)
      if (seedWords.length > 0 && seedWords.some(w => t.includes(w))) continue;
      // Exclude karaoke, instrumental, whatsapp status
      if (t.includes('karaoke') || t.includes('instrumental') || t.includes('whatsapp status') || t.includes('8k status')) continue;

      const dur = v.duration?.seconds || 0;
      if (dur < 30 || dur > maxDuration) continue;

      candidateSongs.push(v);
      if (candidateSongs.length >= 25) break;
    }

    const songs = candidateSongs.map(mapVideoToSong);
    res.json({ results: songs });
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

  // Render 24/7 Keep-Alive: Ping health endpoint every 8 minutes
  const pingUrl = process.env.RENDER_EXTERNAL_URL || 'https://musicplayer-blyl.onrender.com';
  setInterval(() => {
    https.get(`${pingUrl}/api/health`, (res) => {
      console.log(`[KeepAlive] Pinged ${pingUrl}/api/health - Status: ${res.statusCode}`);
    }).on('error', (err) => {
      console.warn('[KeepAlive] Warning:', err.message);
    });
  }, 8 * 60 * 1000);
});
