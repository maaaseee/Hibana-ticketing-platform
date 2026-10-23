/** @type {import('tailwindcss').Config} */
function withOpacity(varName, fallback) {
  return ({ opacityValue }) => {
    if (opacityValue !== undefined) {
      return `color-mix(in srgb, var(${varName}, ${fallback}) calc(${opacityValue} * 100%), transparent)`
    }
    return `var(${varName}, ${fallback})`
  }
}

export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Surfaces (Abyssal Luminescence)
        "canvas-base": withOpacity("--color-canvas-base", "#0b131e"),
        "surface": withOpacity("--color-surface", "#0c141f"),
        "surface-dim": withOpacity("--color-surface-dim", "#0e1927"),
        "surface-default": withOpacity("--color-surface-default", "#132235"),
        "surface-card": withOpacity("--color-surface-card", "#1c2e48"),
        "surface-crisp": withOpacity("--color-surface-crisp", "#1c2e48"),
        "surface-elevated": withOpacity("--color-surface-elevated", "#223756"),
        "surface-interactive": withOpacity("--color-surface-interactive", "#273f62"),
        "surface-hover": withOpacity("--color-surface-hover", "#2d486f"),
        "surface-module": withOpacity("--color-surface-module", "#162438"),
        "surface-container-deep": withOpacity("--color-surface-container-deep", "#101825"),
        "surface-container-lowest": withOpacity("--color-surface-container-lowest", "#070f19"),
        "surface-container-low": withOpacity("--color-surface-container-low", "#141c27"),
        "surface-container": withOpacity("--color-surface-container", "#18202b"),
        "surface-container-high": withOpacity("--color-surface-container-high", "#232a36"),
        "surface-container-highest": withOpacity("--color-surface-container-highest", "#2d3541"),
        "surface-variant": withOpacity("--color-surface-variant", "#2d3541"),
        "surface-bright": withOpacity("--color-surface-bright", "#323a46"),

        // Typography & Ink
        "high-emphasis": withOpacity("--color-high-emphasis", "#f1f5f9"),
        "high-emphasis-alt": withOpacity("--color-high-emphasis-alt", "#f0f4f8"),
        "on-surface": withOpacity("--color-on-surface", "#dbe3f3"),
        "on-surface-variant": withOpacity("--color-on-surface-variant", "#bfc8cf"),
        "medium-emphasis": withOpacity("--color-medium-emphasis", "#cbd5e1"),
        "muted-light": withOpacity("--color-muted-light", "#94a3b8"),
        "muted": withOpacity("--color-muted", "#64748b"),
        "inverse-surface": withOpacity("--color-inverse-surface", "#dbe3f3"),
        "inverse-on-surface": withOpacity("--color-inverse-on-surface", "#29313d"),

        // Borders & Dividers
        "border-subtle": withOpacity("--color-border-subtle", "#23354d"),
        "border-structural": withOpacity("--color-border-structural", "#1e3a5f"),
        "outline": withOpacity("--color-outline", "#899299"),
        "outline-variant": withOpacity("--color-outline-variant", "#40484e"),
        "border-focus": withOpacity("--color-border-focus", "#86cffb"),

        // Primary & Anchors
        "primary": withOpacity("--color-primary", "#86cffb"),
        "surface-tint": withOpacity("--color-surface-tint", "#86cffb"),
        "on-primary": withOpacity("--color-on-primary", "#00344a"),
        "primary-container": withOpacity("--color-primary-container", "#00688f"),
        "ocean-primary": withOpacity("--color-ocean-primary", "#00688f"),
        "ocean-primary-hover": withOpacity("--color-ocean-primary-hover", "#007da8"),
        "ocean-primary-active": withOpacity("--color-ocean-primary-active", "#005a7d"),
        "on-primary-container": withOpacity("--color-on-primary-container", "#b7e2ff"),
        "ocean-on-primary-container": withOpacity("--color-ocean-on-primary-container", "#b7e2ff"),
        "inverse-primary": withOpacity("--color-inverse-primary", "#00658b"),
        "primary-fixed": withOpacity("--color-primary-fixed", "#c5e7ff"),
        "primary-fixed-dim": withOpacity("--color-primary-fixed-dim", "#86cffb"),
        "on-primary-fixed": withOpacity("--color-on-primary-fixed", "#001e2d"),
        "on-primary-fixed-variant": withOpacity("--color-on-primary-fixed-variant", "#004c6a"),

        // Secondary & Bioluminescence
        "secondary": withOpacity("--color-secondary", "#4fdbc8"),
        "on-secondary": withOpacity("--color-on-secondary", "#003731"),
        "secondary-container": withOpacity("--color-secondary-container", "#04b4a2"),
        "on-secondary-container": withOpacity("--color-on-secondary-container", "#003f38"),
        "secondary-fixed": withOpacity("--color-secondary-fixed", "#71f8e4"),
        "secondary-fixed-dim": withOpacity("--color-secondary-fixed-dim", "#4fdbc8"),
        "on-secondary-fixed": withOpacity("--color-on-secondary-fixed", "#00201c"),
        "on-secondary-fixed-variant": withOpacity("--color-on-secondary-fixed-variant", "#005048"),
        "bioluminescence": withOpacity("--color-bioluminescence", "#14b8a6"),
        "active-bioluminescence": withOpacity("--color-active-bioluminescence", "#14b8a6"),
        "electric-cyan": withOpacity("--color-electric-cyan", "#38bdf8"),
        "topography-cyan": withOpacity("--color-topography-cyan", "#00cff3"),
        "topography-navy": withOpacity("--color-topography-navy", "#1d2f54"),

        // Tertiary
        "tertiary": withOpacity("--color-tertiary", "#7bd0ff"),
        "on-tertiary": withOpacity("--color-on-tertiary", "#00354a"),
        "tertiary-container": withOpacity("--color-tertiary-container", "#00688d"),
        "on-tertiary-container": withOpacity("--color-on-tertiary-container", "#b5e2ff"),
        "tertiary-fixed": withOpacity("--color-tertiary-fixed", "#c4e7ff"),
        "tertiary-fixed-dim": withOpacity("--color-tertiary-fixed-dim", "#7bd0ff"),
        "on-tertiary-fixed": withOpacity("--color-on-tertiary-fixed", "#001e2c"),
        "on-tertiary-fixed-variant": withOpacity("--color-on-tertiary-fixed-variant", "#004c69"),

        // Action Buttons & Vibrant Accents
        "btn-buy": withOpacity("--color-btn-buy", "#0055b3"),
        "btn-buy-hover": withOpacity("--color-btn-buy-hover", "#00428d"),
        "btn-buy-active": withOpacity("--color-btn-buy-active", "#00336e"),
        "accent-sky": withOpacity("--color-accent-sky", "#38bdf8"),
        "accent-sky-hover": withOpacity("--color-accent-sky-hover", "#0284c7"),
        "accent-sky-light": withOpacity("--color-accent-sky-light", "#7dd3fc"),
        "accent-celeste": withOpacity("--color-accent-celeste", "#56cbf9"),
        "accent-celeste-deep": withOpacity("--color-accent-celeste-deep", "#0284c7"),
        "accent-rose": withOpacity("--color-accent-rose", "#f43f5e"),
        "accent-rose-hover": withOpacity("--color-accent-rose-hover", "#e11d48"),
        "navy-dark": withOpacity("--color-navy-dark", "#0a192f"),
        "navy-muted": withOpacity("--color-navy-muted", "#334e68"),

        // Feedback & Canvas Foundation
        "error": withOpacity("--color-error", "#ffb4ab"),
        "on-error": withOpacity("--color-on-error", "#690005"),
        "error-container": withOpacity("--color-error-container", "#93000a"),
        "on-error-container": withOpacity("--color-on-error-container", "#ffdad6"),
        "background": withOpacity("--color-background", "#0c141f"),
        "on-background": withOpacity("--color-on-background", "#dbe3f3"),
      },
      fontFamily: {
        sans: ["Hanken Grotesk", "sans-serif"],
        display: ["Space Grotesk", "sans-serif"],
        headline: ["Space Grotesk", "sans-serif"],
        "headline-sm": ["Space Grotesk", "sans-serif"],
        "headline-md": ["Space Grotesk", "sans-serif"],
        "headline-lg": ["Space Grotesk", "sans-serif"],
        "headline-xl": ["Space Grotesk", "sans-serif"],
        body: ["Hanken Grotesk", "sans-serif"],
        "body-sm": ["Hanken Grotesk", "sans-serif"],
        "body-md": ["Hanken Grotesk", "sans-serif"],
        "body-lg": ["Hanken Grotesk", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
        "label-sm": ["JetBrains Mono", "monospace"],
        "label-md": ["JetBrains Mono", "monospace"],
        "label-lg": ["JetBrains Mono", "monospace"],
        skyvia: ["Skyvia", "Space Grotesk", "sans-serif"],
        ink: ["Skyvia", "Space Grotesk", "sans-serif"],
      },
      fontSize: {
        "body-sm": ["14px", { lineHeight: "20px", letterSpacing: "0.005em", fontWeight: "400" }],
        "body-md": ["16px", { lineHeight: "24px", letterSpacing: "0em", fontWeight: "400" }],
        "body-lg": ["18px", { lineHeight: "28px", letterSpacing: "-0.005em", fontWeight: "400" }],
        "headline-sm": ["20px", { lineHeight: "28px", letterSpacing: "0em", fontWeight: "600" }],
        "headline-md": ["24px", { lineHeight: "32px", letterSpacing: "-0.01em", fontWeight: "600" }],
        "headline-lg": ["32px", { lineHeight: "40px", letterSpacing: "-0.015em", fontWeight: "600" }],
        "headline-xl": ["40px", { lineHeight: "48px", letterSpacing: "-0.02em", fontWeight: "700" }],
        "headline-xl-mobile": ["28px", { lineHeight: "36px", letterSpacing: "-0.01em", fontWeight: "700" }],
        "display-lg": ["56px", { lineHeight: "64px", letterSpacing: "-0.03em", fontWeight: "700" }],
        "display-lg-mobile": ["36px", { lineHeight: "44px", letterSpacing: "-0.02em", fontWeight: "700" }],
        "label-sm": ["10px", { lineHeight: "14px", letterSpacing: "0.06em", fontWeight: "500" }],
        "label-md": ["12px", { lineHeight: "16px", letterSpacing: "0.04em", fontWeight: "500" }],
        "label-lg": ["14px", { lineHeight: "20px", letterSpacing: "0.02em", fontWeight: "500" }]
      },
      spacing: {
        gutter: "1.5rem",
        "gutter-mobile": "1rem",
        margin: "2.5rem",
        "margin-mobile": "1rem",
        "space-xs": "0.25rem",
        "space-sm": "0.5rem",
        "space-md": "1rem",
        "space-lg": "1.5rem",
        "space-xl": "2.5rem",
      },
      borderRadius: {
        DEFAULT: "0.125rem",
        sm: "0.125rem",
        md: "0.25rem",
        lg: "0.375rem",
        xl: "0.5rem",
        "2xl": "0.75rem",
        full: "9999px",
      }
    },
  },
  plugins: [],
}
