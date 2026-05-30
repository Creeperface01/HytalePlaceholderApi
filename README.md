# HytalePlaceholderAPI

A flexible and powerful placeholder system for Hytale server developers. It allows for easy registration and retrieval of dynamic values in strings, with support for caching, player-specific data, parameters, and scoped contexts.

## Table of Contents
- [Getting Started](#getting-started)
- [Usage](#usage)
  - [Retrieving Values](#retrieving-values)
  - [Translating Strings](#translating-strings)
- [Registering Placeholders](#registering-placeholders)
  - [Static Placeholders](#static-placeholders)
  - [Visitor-Sensitive Placeholders](#visitor-sensitive-placeholders)
  - [Advanced Builder Options](#advanced-builder-options)
- [Advanced Features](#advanced-features)
  - [Parameters](#parameters)
  - [Scopes & Contexts](#scopes--contexts)
- [Message Formatting](#message-formatting)
  - [How formatting works in messages](#how-formatting-works-in-messages)
  - [Colors](#colors)
  - [Text styles](#text-styles)
  - [Translations](#translations)
  - [Programmatic construction](#programmatic-construction)
- [Kotlin Support](#kotlin-support)
- [Built-in Placeholders](#built-in-placeholders)

---

## Getting Started

To use the API, you first need to get the `PlaceholderAPI` instance.

**Java:**
```java
PlaceholderAPI api = PlaceholderAPI.getInstance();
```

**Kotlin:**
```kotlin
val api = PlaceholderAPI.getInstance()
```

---

## Usage

### Retrieving Values
You can get a single placeholder value using `getValue()`.

```java
// Player can be null if the placeholder isn't visitor-sensitive
String value = api.getValue("placeholder_name", player);
```

### Translating Strings
To replace all placeholders in a string automatically, use the `translateString()` method.

**Java:**
```java
String result = api.translateString("Hello %player_display_name%!", player);
```

**Kotlin:**
HytalePlaceholderAPI provides a convenient extension function for strings.
```kotlin
val result = "Hello %player_display_name%!".translatePlaceholders(player)
```

---

## Registering Placeholders

There are two primary types of placeholders:

1.  **Static:** These have the same value for everyone. They are effectively cached by the API for better performance.
2.  **Visitor-Sensitive:** These depend on the player (visitor) they are being shown to (e.g., player name, balance).

### Static Placeholders
Example of a placeholder that returns the current server tick:

**Java:**
```java
api.builder("tick", Integer.class)
        .loader(entry -> Server.getInstance().getTick())
        .build();
```

**Kotlin DSL:**
```kotlin
api.build<Int>("tick") {
    loader {
        Server.getInstance().tick
    }
}
```

### Visitor-Sensitive Placeholders
Example of a placeholder that returns the player's name:

**Java:**
```java
api.builder("player_name", String.class)
        .visitorLoader(entry -> entry.getPlayer().getName())
        .build();
```

**Kotlin DSL:**
In Kotlin, you can access the `player` instance directly within the lambda.
```kotlin
api.build<String>("player_name") {
    visitorLoader {
        player.name
    }
}
```

### Advanced Builder Options
The builder provides several configuration options:

```java
api.builder("tick", Integer.class)
        .aliases("server_tick", "servertick") // Alternative names
        .autoUpdate(true)                    // Triggers update listeners automatically
        .updateInterval(10)                  // Cache duration or auto-update interval in ticks
        .processParameters(true)             // Enable parameter support for this placeholder
        .build();
```

---

## Advanced Features

### Parameters

Placeholders can accept parameters using the `%name<args>%` syntax. Three flavors are supported and they can be freely
mixed in the same placeholder:

| Form                                | Example                  | Access                                            |
|-------------------------------------|--------------------------|---------------------------------------------------|
| Positional                          | `%player_name<lc>%`      | `parameters.single()` / `parameters.getUnnamed()` |
| Multiple positional (`;`-separated) | `%join<a;b;c>%`          | `parameters.getUnnamed()` returns all in order    |
| Named (`key=value`)                 | `%greet<target=world>%`  | `parameters["target"]` / `parameters.getNamed()`  |
| Nested placeholder as value         | `%upper<%player_name%>%` | `parameters.single().matchedGroup` (see below)    |

`PlaceholderParameters` exposes `single()`, `get(key)`, `getNamed()`, `getUnnamed()`, and `getAll()` so loaders can pick
whichever access pattern fits.

**Implementation:**
Enable parameter processing in the builder and access them via `entry.getParameters()` (Java) or the `parameters`
receiver (Kotlin).

**Java:**
```java
api.builder("player_name", String.class)
        .processParameters(true)
        .visitorLoader(entry -> {
            String name = entry.getPlayer().getDisplayName();
            Parameter param = entry.getParameters().single();

            if (param != null && "lc".equals(param.getValue())) {
                return name.toLowerCase();
            }

            return name;
        }).build();
```

**Kotlin:**
```kotlin
api.build<String>("player_name") {
    processParameters(true)
    visitorLoader {
        val name = player.displayName

        if (parameters.single()?.value == "lc") {
            return@visitorLoader name.toLowerCase()
        }

        name
    }
}
```

#### Named parameters

Named parameters use the `key=value` form and let loaders read arguments by name regardless of position. Combine
multiple parameters with `;`:

```
%greet<target=world>%
%link<text=Click here;url=https://example.com>%
```

**Kotlin:**

```kotlin
api.build<String>("greet") {
  processParameters(true)
  loader {
    val target = parameters["target"]?.value ?: "stranger"
    "hello $target"
  }
}
```

**Java:**

```java
api.builder("greet",String .class)
        .

processParameters(true)
        .

loader(entry ->{
Parameter target = entry.getParameters().get("target");
String value = target != null ? target.getValue() : "stranger";
            return"hello "+value;
        }).

build();
```

#### Nested placeholders as parameters

A parameter value can itself be a placeholder, e.g. `%upper<%player_name%>%`. Nested placeholders are **not**
auto-resolved — the parser exposes them on the parameter as a `MatchedGroup`, and the loader decides what to do with
them (typically resolving via `api.getValue(...)`). When a parameter contains a nested placeholder, `parameter.value` is
empty; the inner placeholder lives in `parameter.matchedGroup`.

**Kotlin:**

```kotlin
api.build<String>("upper") {
  processParameters(true)
  loader {
    val param = parameters.single() ?: return@loader ""
    val raw = param.matchedGroup?.let { api.getValue(it.value) }
      ?: param.value
    raw.uppercase()
  }
}

// "%upper<%player_name%>%"  →  "ALICE"
```

**Java:**

```java
api.builder("upper",String .class)
        .

processParameters(true)
        .

loader(entry ->{
Parameter param = entry.getParameters().single();
            if(param ==null)return"";

String raw = param.getMatchedGroup() != null
        ? api.getValue(param.getMatchedGroup().getValue())
        : param.getValue();
            return raw.

toUpperCase();
        }).

build();
```

### Scopes & Contexts
Scopes allow placeholders to be available only in specific situations or to use data provided at runtime.

*   **Scope:** A singleton defining the category (e.g., `ChatScope`).
*   **Context:** A specific instance of a scope holding data (e.g., the actual `PlayerChatEvent`).

By default, everything is in the `GlobalScope`.

**Example: ChatScope**
In a chat event, you can use `%message%` or `%message_sender%` which are only available within the `ChatScope`.

```kotlin
fun onChat(e: PlayerChatEvent) {
    // Translate placeholders using the ChatScope context from the event
    e.message = messageTemplate.translatePlaceholders(
        e.player,
        e.context // Extension property providing the ChatScope.Context
    )
}
```

---

## Built-in Placeholders
The API comes with several built-in placeholders:

| Placeholder                              | Description                                                 | Scope        |
|------------------------------------------|-------------------------------------------------------------|--------------|
| `%player_display_name%`                  | Player's display name                                       | Global       |
| `%player_gamemode%`                      | Player's game mode                                          | Global       |
| `%player_x%`, `%player_y%`, `%player_z%` | Player coordinates                                          | Global       |
| `%server_online%`                        | Number of players online                                    | Global       |
| `%server_ram_used%`                      | Server RAM usage                                            | Global       |
| `%time%`                                 | Current server time                                         | Global       |
| `%message%`                              | Chat/Message content                                        | Chat/Message |
| `%message_sender%`                       | Sender of the message                                       | Chat/Message |
| `%color%`                                | Tailwind palette color — see [Colors](#colors)              | Global       |
| `%color_rgb%`                            | RGB color — see [Colors](#colors)                           | Global       |
| `%color_hex%`                            | Hex color — see [Colors](#colors)                           | Global       |
| `%color_oklch%`                          | OKLCH color — see [Colors](#colors)                         | Global       |
| `%bold%`                                 | Bold style marker — see [Text styles](#text-styles)         | Global       |
| `%italic%`                               | Italic style marker — see [Text styles](#text-styles)       | Global       |
| `%monospace%`                            | Monospace style marker — see [Text styles](#text-styles)    | Global       |
| `%underlined%`                           | Underline style marker — see [Text styles](#text-styles)    | Global       |
| `%link%`                                 | Clickable link marker — see [Text styles](#text-styles)     | Global       |
| `%trans%`                                | i18n translation marker — see [Translations](#translations) | Global       |

---

## Message Formatting

The API ships with three families of formatting placeholders that produce structured `Message` output rather than plain
text: **colors**, **text styles**, and **i18n translations**. Colors and styles behave like state markers — instead of
being replaced by text, they apply an attribute to every segment that follows them. Translations resolve i18n message
ids with optional parameters.

### How formatting works in messages

Formatting placeholders evaluate to `MessageColor`, `MessageStyle`, or a translation `Message` rather than a string. To
actually render the attributes, translate the input with **`translateMessage()`** — which returns a structured `Message`
with per-segment attributes — instead of `translateString()`:

```kotlin
val message = api.translateMessage(
  "%color<red>%%bold%Error! %color<gray; 400>%%bold<false>%Details here."
)
```

If you call `translateString()` on the same input, attributes are silently dropped because there is no state to
propagate — `translateString` just joins each placeholder's `Message` next to surrounding text. Inside
`translateMessage`, every color/style placeholder applies to **everything after it, up to the next placeholder that
overrides the same attribute**. There is no implicit "reset"; pass `<false>` (or omit the placeholder) to switch a style
off.

### Colors

| Placeholder              | Description                                                                                                                                  |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `%color<name; shade>%`   | Tailwind palette color. `shade` defaults to `500` if omitted (e.g. `%color<red>%`).                                                          |
| `%color_rgb<r; g; b>%`   | RGB triplet, each `0–255`.                                                                                                                   |
| `%color_hex<value>%`     | Hex string. Accepts `#fff`, `fff`, `#ffffff`, or `ffffff`.                                                                                   |
| `%color_oklch<L; C; H>%` | OKLCH: lightness `0–1`, chroma `0–~0.4`, hue in degrees. Perceptually uniform — useful for theming. Out-of-gamut values are clipped to sRGB. |

Invalid input (unknown color name, malformed hex, out-of-range RGB component, etc.) leaves the placeholder text in the
output unchanged.

#### Tailwind palette

The `%color<...>%` placeholder uses the official Tailwind CSS v3 default palette. Available color names:

`slate`, `gray`, `zinc`, `neutral`, `stone`, `red`, `orange`, `amber`, `yellow`, `lime`, `green`, `emerald`, `teal`,
`cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`

Each color has 11 shades: `50`, `100`, `200`, `300`, `400`, `500`, `600`, `700`, `800`, `900`, `950`. Lower numbers are
lighter; `500` is the base.

```
%color<red>%             → red-500    (#ef4444)
%color<sky; 600>%        → sky-600    (#0284c7)
%color<emerald; 300>%    → emerald-300 (#6ee7b7)
```

### Text styles

Style placeholders work the same way as colors: each one flips a single attribute on the active "pen", and every text
segment after it inherits that attribute until the same attribute is overridden. Pass `<false>` (or `0`, `off`, `no`) to
turn a style off; any other value — or no argument at all — turns it on.

| Placeholder    | Description                                                       |
|----------------|-------------------------------------------------------------------|
| `%bold%`       | Bold weight on subsequent segments. `%bold<false>%` turns it off. |
| `%italic%`     | Italic on subsequent segments. `%italic<false>%` turns it off.    |
| `%monospace%`  | Monospace / code font. `%monospace<false>%` turns it off.         |
| `%underlined%` | Underline. `%underlined<false>%` turns it off.                    |
| `%link<url>%`  | Wraps subsequent segments in a clickable link to `url`.           |

Styles compose freely with colors and with each other:

```kotlin
api.translateMessage(
  "%color<red>%%bold%Danger!%bold<false>% %italic%(read carefully)"
)
```

Without arguments `%bold%` and `%bold<true>%` are equivalent. `%link<...>%` requires a URL — used without one it
collapses to the bare text `link`.

### Translations

`%trans<key; ...>%` produces a `Message.translation(key)` so the receiving client can resolve the localized text. The
first unnamed argument is the i18n key. Any additional arguments become message parameters: unnamed arguments are bound
to indexed names (`"0"`, `"1"`, …), and named arguments (`name=value`) keep their original names.

```
%trans<server.welcome>%
%trans<server.welcome; name=Alice>%
%trans<server.welcome; Alice; 5>%
%trans<server.welcome; Alice; count=5>%
```

When used inside `translateMessage`, the translation `Message` is inlined as its own segment and inherits any active
style/color attributes:

```kotlin
// "%bold%%trans<server.welcome>%"
// → Message.translation("server.welcome").bold(true)
```

If the placeholder is invoked with no key, it falls back to the bare text `trans`.

### Programmatic construction

`MessageColor` and `MessageStyle` also expose factories for use directly in code:

```kotlin
// Colors
MessageColor.tailwind("red", 500)     // palette lookup; null if unknown
MessageColor.rgb(255, 128, 0)         // RGB triplet, clamped to 0–255
MessageColor.hex("#3b82f6")           // hex; null if malformed
MessageColor.oklch(0.7, 0.15, 30.0)   // OKLCH → sRGB hex

// Styles — each returns a marker that applyTo(message) writes onto a Message.
MessageStyle.Bold(enabled = true)
MessageStyle.Italic(enabled = true)
MessageStyle.Monospace(enabled = true)
MessageStyle.Underlined(enabled = true)
MessageStyle.Link(url = "https://example.com")
```

The `hex` field on the returned `MessageColor` is always a 6-character lowercase hex string (no `#` prefix). Each
`MessageStyle.toMessage()` returns a fresh empty `Message` carrying only that attribute, which is exactly what the
default placeholder formatters produce.

---

## Kotlin Support
HytalePlaceholderAPI is built with Kotlin in mind, offering:
- **Type-safe DSL** for registering placeholders.
- **Extension functions** like `String.translatePlaceholders()`.
- **Lambda receivers** for easy access to `player`, `parameters`, and `context`.
