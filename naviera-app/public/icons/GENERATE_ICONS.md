## How to generate PWA icons

The PWA requires three icon files in this directory:

- `icon-192.png` — 192x192 px
- `icon-512.png` — 512x512 px
- `icon-512-maskable.png` — 512x512 px with extra padding (safe zone = inner 80%)

### Option A: From the SVG logo

1. Open `icon-source.svg` (in this folder) in any image editor (Figma, Inkscape, etc.)
2. Export at 192x192 and 512x512
3. For the maskable version, add extra padding so the logo fits in the inner 80% circle

### Option B: Quick CLI generation (requires Inkscape)

```bash
inkscape icon-source.svg -w 192 -h 192 -o icon-192.png
inkscape icon-source.svg -w 512 -h 512 -o icon-512.png
# For maskable, use icon-source-maskable.svg with extra padding
inkscape icon-source-maskable.svg -w 512 -h 512 -o icon-512-maskable.png
```

### Option C: Online tool

Upload the SVG to https://maskable.app/editor or https://realfavicongenerator.net
