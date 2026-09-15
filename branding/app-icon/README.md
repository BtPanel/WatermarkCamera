# Watermark Camera App Icon

## Approved brief

- Mode: new symbol, followed by a neutral app-icon adaptation.
- Product: a camera app that binds time, location, and field information to photos.
- Audience: field, construction, inspection, and attendance users who need clear records.
- Brand essence: Make every field photo a clear record with time and location evidence.
- Register: direct, professional, reliable, and visible on a busy phone home screen.
- Required text: none.
- Minimum tested size: 16 px.
- Approved direction: B2 Coordinate Cut.
- Approved color: R2 Signal Red.
- Colors: signal red `#E43D36`, white `#FFFFFF`, amber `#FFC247` in sRGB.
- Assumption: Android is the immediate use context, inferred from the supplied emulator screenshots and package name. These exports do not claim current Android adaptive-icon compliance.

## Concept decision

The selected mark combines a record frame with one pulled-down corner. The cut corner carries the location idea without using a conventional map-pin silhouette. A circular center reads as both a camera lens and a recorded coordinate.

| Candidate | Decision | Reason |
| --- | --- | --- |
| A Trusted Frame | Eliminated | Clear camera meaning, but scanner and generic camera readings remained likely. |
| B1 Pin Seal | Eliminated | Failed distinctiveness because the dominant silhouette is a standard location pin. |
| B2 Coordinate Cut | Selected | Retains location meaning, stays legible at 16 px, and has a less generic silhouette. |
| B3 Coordinate Stamp | Eliminated | Center detail became dense at 16 px and could read as a target or medical cross. |
| B4 Address Tag | Eliminated | Dominant unintended reading was a chat bubble. |
| C Field Record | Eliminated | Strong record meaning, but weaker camera recognition and a document/scanner risk. |

## Finalist scorecard

Scores use the 1-5 scale in the app-logo-design visual QA profile.

| Criterion | B2 score | Evidence |
| --- | ---: | --- |
| Distinctiveness | 4 | Asymmetric cut corner differentiates it from a conventional camera or location pin. |
| Simplicity | 4 | One dominant frame silhouette and one circular internal structure. |
| 16 px legibility | 4 | Frame, center point, and lower-right cut remain separate in the raster export. |
| Monochrome performance | 5 | Geometry remains identifiable in black, reverse, and gray without hue. |
| Relevance | 4 | Frame, coordinate point, and pulled corner support the approved brand essence. |
| Optical balance | 4 | The lower-right extension is counterweighted by the open center and even outer margins. |
| Adaptability | 4 | Flat geometry works as a transparent mark and inside a square icon field. |
| Originality confidence | not run | No public competitor, visual-similarity, or trademark screen was performed. |

All scored hard gates meet their required floor. Originality confidence remains unscored, so the design is provisional rather than ready for commercial adoption.

## Export inventory

`exports/logo-mark.svg` is the editable, platform-neutral master. It uses `currentColor`, contains no live text, scripts, embedded raster data, external resources, platform mask, shadow, or gradient.

`exports/logo-mark-reversed.svg` is the white reverse mark. `exports/app-icon.svg` is the approved signal-red presentation. PNG exports are supplied at 16, 32, 48, 64, 72, 96, 144, 192, 512, and 1024 px. Exact hashes and roles are recorded in `exports/manifest.json`.

## Provenance and rights risk

- Project ID: `watermark-camera-icon-b2-r2`.
- Manifest version: 1.0.
- Decision owner: user.
- Date: 2026-09-09.
- Intended use: private workspace app-icon design and evaluation.
- Privacy: local workspace; no candidate or supplied image was uploaded to an external search service.
- Inputs reviewed: `camera_check.png`, `final-camera.png`, and `templates-builtin.png` as user-supplied product context. Creator, claimed owner, and publication permission are unknown.
- Construction: original controlled vector geometry authored in the workspace; no font, stock element, third-party logo, or generated bitmap was used.
- Tool context: Codex app-logo-design workflow and a local Node.js SVG/PNG exporter.
- Competitor screen: `not run`; no public search was requested.
- Visual-similarity screen: `not run`; external upload was not authorized.
- Trademark-database screen: `not run`; jurisdiction and goods/services scope were not specified.

This is an initial design and rights-risk record, not legal clearance, registrability advice, exclusivity, or a non-infringement opinion. Before commercial adoption, run a category and jurisdiction-specific search and obtain qualified trademark advice where appropriate.

## Next test

Place the neutral mark into the app project's current Android adaptive-icon template, preview every launcher mask in the supported Android Studio version, and test it on both light and dark wallpapers. A short blinded recognition check with field users should confirm that the first reading is "camera record with location" rather than document, chat, or map pin.
