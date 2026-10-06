# Satoshi Font Setup

Quacky uses **Satoshi** by Fontshare as its primary and only typeface.

## Download Instructions:
1. Visit https://www.fontshare.com/fonts/satoshi
2. Download the font zip archive.
3. Extract the OTF or TTF font files.
4. Copy the following files into `app/src/main/res/font/` renamed to lowercase:
   - `satoshi_light.otf` (or `.ttf`)
   - `satoshi_regular.otf` (or `.ttf`)
   - `satoshi_medium.otf` (or `.ttf`)
   - `satoshi_bold.otf` (or `.ttf`)
   - `satoshi_black.otf` (or `.ttf`)

Note: If these font resource files are not yet added, the app automatically and gracefully falls back to `FontFamily.SansSerif` so the project compiles and runs seamlessly.
