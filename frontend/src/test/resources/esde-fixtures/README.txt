Synthetic ES-DE themes for import tests (#575). Every file here was written for Jarn: no XML,
text, names or assets are copied from a real theme, since real themes carry their own licenses.
Each folder copies only the STRUCTURE of a common theme style. Paths may point at images or
videos that don't exist; the parser and translator must cope with that anyway.

  horizon-carousel   system view: horizontal logo carousel over per-system art via ${system.theme}
  wheel-video        vertical wheel carousel, gamelist video, variants with and without video
  cover-grid         gamelist grid of covers, font sizes
  list-detail        textlist plus a detail pane (rating, date, description), split into includes
  per-system         per-system folders (nes, snes) beside a root fallback theme
  color-schemes      dark and light schemes plus a non-selectable one, colors via variables
  legacy-format      pre ES-DE theme with <formatVersion>
  hostile            include cycle, path escapes, DOCTYPE, oversize file (written by the test),
                     missing include, unknown variable, unnamed element

Goldens live in ../esde-goldens/<fixture>/: parser.txt now; translator.json and report.txt are
picked up automatically once the translator (#574) adds them.
