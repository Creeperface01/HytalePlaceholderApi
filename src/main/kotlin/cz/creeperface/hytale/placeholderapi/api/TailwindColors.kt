package cz.creeperface.hytale.placeholderapi.api

// Tailwind CSS v3 default palette. Source: tailwindlabs/tailwindcss colors.js
internal object TailwindColors {

    fun get(name: String, shade: Int): String? = palette[name.lowercase()]?.get(shade)

    private fun shades(
        s50: String, s100: String, s200: String, s300: String, s400: String,
        s500: String, s600: String, s700: String, s800: String, s900: String, s950: String
    ): Map<Int, String> = mapOf(
        50 to s50, 100 to s100, 200 to s200, 300 to s300, 400 to s400,
        500 to s500, 600 to s600, 700 to s700, 800 to s800, 900 to s900, 950 to s950
    )

    private val palette: Map<String, Map<Int, String>> = mapOf(
        "slate" to shades(
            "f8fafc",
            "f1f5f9",
            "e2e8f0",
            "cbd5e1",
            "94a3b8",
            "64748b",
            "475569",
            "334155",
            "1e293b",
            "0f172a",
            "020617"
        ),
        "gray" to shades(
            "f9fafb",
            "f3f4f6",
            "e5e7eb",
            "d1d5db",
            "9ca3af",
            "6b7280",
            "4b5563",
            "374151",
            "1f2937",
            "111827",
            "030712"
        ),
        "zinc" to shades(
            "fafafa",
            "f4f4f5",
            "e4e4e7",
            "d4d4d8",
            "a1a1aa",
            "71717a",
            "52525b",
            "3f3f46",
            "27272a",
            "18181b",
            "09090b"
        ),
        "neutral" to shades(
            "fafafa",
            "f5f5f5",
            "e5e5e5",
            "d4d4d4",
            "a3a3a3",
            "737373",
            "525252",
            "404040",
            "262626",
            "171717",
            "0a0a0a"
        ),
        "stone" to shades(
            "fafaf9",
            "f5f5f4",
            "e7e5e4",
            "d6d3d1",
            "a8a29e",
            "78716c",
            "57534e",
            "44403c",
            "292524",
            "1c1917",
            "0c0a09"
        ),
        "red" to shades(
            "fef2f2",
            "fee2e2",
            "fecaca",
            "fca5a5",
            "f87171",
            "ef4444",
            "dc2626",
            "b91c1c",
            "991b1b",
            "7f1d1d",
            "450a0a"
        ),
        "orange" to shades(
            "fff7ed",
            "ffedd5",
            "fed7aa",
            "fdba74",
            "fb923c",
            "f97316",
            "ea580c",
            "c2410c",
            "9a3412",
            "7c2d12",
            "431407"
        ),
        "amber" to shades(
            "fffbeb",
            "fef3c7",
            "fde68a",
            "fcd34d",
            "fbbf24",
            "f59e0b",
            "d97706",
            "b45309",
            "92400e",
            "78350f",
            "451a03"
        ),
        "yellow" to shades(
            "fefce8",
            "fef9c3",
            "fef08a",
            "fde047",
            "facc15",
            "eab308",
            "ca8a04",
            "a16207",
            "854d0e",
            "713f12",
            "422006"
        ),
        "lime" to shades(
            "f7fee7",
            "ecfccb",
            "d9f99d",
            "bef264",
            "a3e635",
            "84cc16",
            "65a30d",
            "4d7c0f",
            "3f6212",
            "365314",
            "1a2e05"
        ),
        "green" to shades(
            "f0fdf4",
            "dcfce7",
            "bbf7d0",
            "86efac",
            "4ade80",
            "22c55e",
            "16a34a",
            "15803d",
            "166534",
            "14532d",
            "052e16"
        ),
        "emerald" to shades(
            "ecfdf5",
            "d1fae5",
            "a7f3d0",
            "6ee7b7",
            "34d399",
            "10b981",
            "059669",
            "047857",
            "065f46",
            "064e3b",
            "022c22"
        ),
        "teal" to shades(
            "f0fdfa",
            "ccfbf1",
            "99f6e4",
            "5eead4",
            "2dd4bf",
            "14b8a6",
            "0d9488",
            "0f766e",
            "115e59",
            "134e4a",
            "042f2e"
        ),
        "cyan" to shades(
            "ecfeff",
            "cffafe",
            "a5f3fc",
            "67e8f9",
            "22d3ee",
            "06b6d4",
            "0891b2",
            "0e7490",
            "155e75",
            "164e63",
            "083344"
        ),
        "sky" to shades(
            "f0f9ff",
            "e0f2fe",
            "bae6fd",
            "7dd3fc",
            "38bdf8",
            "0ea5e9",
            "0284c7",
            "0369a1",
            "075985",
            "0c4a6e",
            "082f49"
        ),
        "blue" to shades(
            "eff6ff",
            "dbeafe",
            "bfdbfe",
            "93c5fd",
            "60a5fa",
            "3b82f6",
            "2563eb",
            "1d4ed8",
            "1e40af",
            "1e3a8a",
            "172554"
        ),
        "indigo" to shades(
            "eef2ff",
            "e0e7ff",
            "c7d2fe",
            "a5b4fc",
            "818cf8",
            "6366f1",
            "4f46e5",
            "4338ca",
            "3730a3",
            "312e81",
            "1e1b4b"
        ),
        "violet" to shades(
            "f5f3ff",
            "ede9fe",
            "ddd6fe",
            "c4b5fd",
            "a78bfa",
            "8b5cf6",
            "7c3aed",
            "6d28d9",
            "5b21b6",
            "4c1d95",
            "2e1065"
        ),
        "purple" to shades(
            "faf5ff",
            "f3e8ff",
            "e9d5ff",
            "d8b4fe",
            "c084fc",
            "a855f7",
            "9333ea",
            "7e22ce",
            "6b21a8",
            "581c87",
            "3b0764"
        ),
        "fuchsia" to shades(
            "fdf4ff",
            "fae8ff",
            "f5d0fe",
            "f0abfc",
            "e879f9",
            "d946ef",
            "c026d3",
            "a21caf",
            "86198f",
            "701a75",
            "4a044e"
        ),
        "pink" to shades(
            "fdf2f8",
            "fce7f3",
            "fbcfe8",
            "f9a8d4",
            "f472b6",
            "ec4899",
            "db2777",
            "be185d",
            "9d174d",
            "831843",
            "500724"
        ),
        "rose" to shades(
            "fff1f2",
            "ffe4e6",
            "fecdd3",
            "fda4af",
            "fb7185",
            "f43f5e",
            "e11d48",
            "be123c",
            "9f1239",
            "881337",
            "4c0519"
        ),
    )
}
