# VahmurHolograms 0.1.0

Add-on para FancyHolograms que permite tratar varias líneas con escalas y separaciones distintas como **un solo holograma lógico**.

## Idea

FancyHolograms mantiene un holograma TEXT normal como `anchor`. Ese holograma conserva ubicación, yaw/pitch, billboard, visibilidad, distancia, sombra, fondo, etc. Su texto debe ser invisible:

```yaml
    text:
    - '<reset>'
```

VahmurHolograms lee `plugins/VahmurHolograms/config.yml` y crea líneas temporales usando la API de FancyHolograms. Las líneas temporales no son persistentes y no se escriben en `holograms.yml`.

Esto permite que el anchor siga moviéndose/editándose con FancyHolograms mientras cada línea visual puede tener una escala distinta.

## Requisitos

- Paper 1.21.4+
- Java 21+
- FancyHolograms 2.10.0-java21 / 2.10.x o una versión posterior compatible con la misma API
- Nexo es opcional. Los `<glyph:...>` pueden permanecer dentro del texto porque las líneas se renderizan mediante FancyHolograms.

## Instalación

1. Coloca `VahmurHolograms-0.1.0.jar` en `plugins/`.
2. Reinicia el servidor.
3. Crea un holograma TEXT normal en FancyHolograms, por ejemplo `CajaKoth`.
4. Déjalo con:

```yaml
    text:
    - '<reset>'
```

5. Configura el grupo en `plugins/VahmurHolograms/config.yml`.
6. Usa `/vholo reload`.

## Formato

```yaml
holograms:
  caja_koth:
    enabled: true
    anchor: CajaKoth
    start_y: 0.55
    line_height: 0.24
    lines:
      - text: '<gray>◈ — <bold>VAHMUR</bold> — ◈</gray>'
        scale: 0.80
        gap_after: 0.03

      - text: '<#FF80B5><bold>ᴄᴀᴊᴀ</bold>'
        scale: 1.80
        gap_after: -0.06

      - text: '<glyph:essential_icons_dialogue>'
        scale: 1.00
```

### Propiedades por línea

- `text`: MiniMessage/FancyHolograms/Nexo glyphs.
- `scale`: escala uniforme.
- `scale_x`, `scale_y`, `scale_z`: sobrescriben `scale` por eje.
- `offset_x`, `offset_y`, `offset_z`: ajuste fino de posición.
- `gap_before`: espacio adicional antes de la línea.
- `gap_after`: espacio adicional después de la línea; admite negativos.
- `text_shadow`: `true/false`, opcional; si falta hereda del anchor.
- `see_through`: `true/false`, opcional; si falta hereda del anchor.

La altura automática que consume cada línea es `line_height * scale_y`.

## Comandos

- `/vholo reload`
- `/vholo status`
- `/vholo rebuild`
- `/vholo rebuild <id>`

Permiso: `vahmurholograms.admin`

## Comportamiento

- Al mover/modificar el anchor desde FancyHolograms, el grupo se reconstruye automáticamente.
- Al ocultarse/mostrarse el anchor para un jugador, las líneas siguen la misma visibilidad.
- Al recargar FancyHolograms, las líneas se destruyen y reconstruyen.
- El anchor debe seguir siendo TEXT.
