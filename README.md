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
Placeholders can accept parameters using the `%name<param1,param2>%` syntax. 

**Example usage:** `%player_name<lc>%` (to get the name in lowercase).

**Implementation:**
Enable parameter processing in the builder and access them via `entry.getParameters()`.

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

| Placeholder | Description | Scope |
|-------------|-------------|-------|
| `%player_display_name%` | Player's display name | Global |
| `%player_gamemode%` | Player's game mode | Global |
| `%player_x%`, `%player_y%`, `%player_z%` | Player coordinates | Global |
| `%server_online%` | Number of players online | Global |
| `%server_ram_used%` | Server RAM usage | Global |
| `%time%` | Current server time | Global |
| `%message%` | Chat/Message content | Chat/Message |
| `%message_sender%` | Sender of the message | Chat/Message |

---

## Kotlin Support
HytalePlaceholderAPI is built with Kotlin in mind, offering:
- **Type-safe DSL** for registering placeholders.
- **Extension functions** like `String.translatePlaceholders()`.
- **Lambda receivers** for easy access to `player`, `parameters`, and `context`.
