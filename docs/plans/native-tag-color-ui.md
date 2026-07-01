# Native tag color UI

**Status:** Implemented in Compose (`TagColorPickerSheet`, tonal chips). Legacy View fragments removed.

**Prerequisite:** Full Compose migration — complete.

**Created:** 2026-06-30

---

## What native Android tag color pickers look like

On Android, **tag/label color selection is not a full spectrum picker**. Apps like Google Keep, Gmail labels, and Tasks use a **curated swatch grid**, not a color wheel or text list.

| Pattern | Native (M3 / Google apps) | QuickNotes today |
|---------|---------------------------|------------------|
| **Picker container** | `ModalBottomSheet` or full-screen bottom panel | `TagColorPickerSheet` (ModalBottomSheet) |
| **Picker content** | Grid of **circular color swatches** (typically 8–12 colors) | 12 curated colors via `TagColorSwatch` grid |
| **Selection affordance** | Checkmark on swatch, or 2dp `outline` ring | Checkmark on selected swatch |
| **Touch target** | 48dp minimum per swatch | 48dp swatch in adaptive grid |
| **Chip appearance** | **Tonal**: neutral chip + colored dot/icon, or muted container tint | `TagFilterChip` / `TagLabelChip` with color dot |
| **Accessibility** | Content description per color ("Red label") | `contentDescription` on swatches |

**What is NOT native for tags:** HSV/RGB sliders, `ColorPickerDialog`, Photoshop-style wheels, or plain text lists.

### Recommended visual spec for QuickNotes

**Color swatch (picker item)**
- 40dp circle, 48dp touch target
- Fill: tag color at **container** tone (muted), not 500-level saturation
- Selected: checkmark centered, contrast-aware icon color
- Unselected: 1dp `outlineVariant` stroke
- Grid: 4 columns on phone, 6+ on tablet (`GridCells.Adaptive(48.dp)`)

**Tag chip (display)**
- Style: **`AssistChip`** or **`SuggestionChip`** (not solid Filter Chip fills)
- Leading: 12dp circle tinted with tag accent
- Background: `surfaceContainer` or low-alpha `secondaryContainer`
- Text: `onSurface` (color indicated by dot, not fill)

**Palette**
- Trim 19 Material 500 colors → **12 curated label colors** with container + accent in light/dark
- Keep existing `@ColorRes` IDs in `TagRepository` for persistence compatibility

---

## Consolidated Compose entry points

Color picking and chip rendering now live in:

- `TagColorPickerSheet` — used from `ManageTagsBottomSheet` and `ManageNoteBottomSheet`
- `TagFilterChip` / `TagLabelChip` — search filters, note editor, manage-tags list

Legacy View fragments (`ManageTagsFragment`, `ManageNoteFragment`, `TagColorSettingsFragment`, `SearchNotesFragment`) have been removed.

---

## Implementation phases (post–Compose migration)

### Phase 1 — Shared Compose components

Under `view/compose/`:

- **`TagColorSwatch`** — circular swatch with selected state
- **`TagColorPickerSheet`** — `ModalBottomSheet` + `LazyVerticalGrid`
- **`TagLabelChip`** — AssistChip + leading color dot

### Phase 2 — Replace pickers

Wire `TagColorPickerSheet` into ManageTags, ManageNote, TagColorSettings.

### Phase 3 — Replace chip display

Search filter row, note editor tags, manage-tags list.

### Phase 4 — Adaptive (optional)

Navigation 3 list-detail for tag + color on tablets.

### Phase 5 — Styles API (optional, experimental)

Only if M3 AssistChip is insufficient; requires Compose 1.12+ alpha.

---

## Success criteria

- Color picker shows **visual swatches**, not text names
- Tag chips use **tonal Assist Chip + dot** (readable in light/dark)
- One shared picker composable used everywhere
- Palette feels like Keep/Tasks, not legacy Material 500 pills
