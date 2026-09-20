# VHolograms 0.2.0

Add-on para FancyHolograms que añade propiedades independientes por línea sin reemplazar el flujo normal de FancyHolograms.

## Filosofía

**FancyHolograms sigue siendo la fuente de verdad.** El texto, ubicación, background, text shadow general, visibilidad, billboard, escala general, translation y demás propiedades continúan editándose normalmente con `/hologram`, `/holo` o `/fholo`.

VHolograms solo guarda metadata adicional para las líneas que necesitan comportarse de forma diferente.

Por ejemplo:

```text
/fholo edit caja setline 1 <#FF80B5><bold>ᴄᴀᴊᴀ</bold> --scale 1.8
/fholo edit caja setline 2 <gray>Calidad ★★★★★</gray> --gap-after 0.12
/fholo edit caja setline 3 <white>ᴄʟɪᴄᴋ ᴘᴀʀᴀ ᴀʙʀɪʀ</white> --offset-y -0.03
```

El texto anterior queda guardado por FancyHolograms. `data.yml` solo conserva `scale`, `gap`, `offset`, etc.

## Opciones por línea

Se pueden añadir al final de `setline`, `addline`, `insertbefore` e `insertafter`:

```text
--scale
--scale-x
--scale-y
--scale-z
--offset-x
--offset-y
--offset-z
--gap-before
--gap-after
--text-shadow
--see-through
```

Ejemplo:

```text
/fholo edit ejemplo setline 1 <gold>Texto --scale 1.5 --offset-y 0.10 --text-shadow false
```

Para volver a heredar una propiedad desde FancyHolograms se puede utilizar `inherit`:

```text
/fholo edit ejemplo setline 1 <gold>Texto --text-shadow inherit
```

Los valores generales siguen viniendo de Fancy. Si Fancy tiene `text_shadow: true`, todas las líneas lo heredan excepto aquellas que tengan un override de VHolograms.

## Autocompletado

VHolograms complementa el autocompletado de FancyHolograms. Después del texto aparecen las opciones `--scale`, `--offset-*`, `--gap-*`, etc., además de sugerencias para sus valores.

## Cómo funciona

Cuando ninguna línea tiene metadata de VHolograms, el holograma funciona exactamente como un holograma normal de Fancy.

Cuando al menos una línea utiliza una propiedad avanzada:

1. Fancy conserva intacto el holograma original y su texto.
2. VHolograms evita que el TextDisplay original se renderice al jugador.
3. Crea un TextDisplay temporal no persistente por cada línea.
4. Cada línea hereda las propiedades generales de Fancy y aplica únicamente sus overrides.
5. Las líneas temporales se registran como `MANUAL` para conservar la visibilidad decidida por el holograma original.

Las líneas temporales no se guardan en `holograms.yml`.

## Rendimiento

VHolograms no añade un loop propio por tick. Aprovecha los eventos y tareas de visibilidad/actualización que ya mantiene FancyHolograms. El coste adicional depende principalmente de cuántas líneas avanzadas están visibles, ya que cada línea necesita su propio TextDisplay para poder tener una escala o transformación independiente.

## Edición normal de Fancy

Todo esto sigue funcionando normalmente:

```text
/fholo create text miholograma
/fholo edit miholograma setline 1 <gold>Mi texto
/fholo edit miholograma background transparent
/fholo edit miholograma textshadow true
/fholo edit miholograma visibilitydistance 24
/fholo edit miholograma translate 0 -0.15 0
```

Solo se activa VHolograms cuando una línea recibe alguna opción `--...`.

## Inserción y borrado de líneas

Si un holograma ya usa VHolograms, los comandos nativos `insertbefore`, `insertafter` y `removeline` reajustan automáticamente los números de la metadata para que los estilos continúen unidos a la línea correcta.

## Comandos propios

```text
/vholo status
/vholo reload
/vholo rebuild [holograma]
/vholo clear <holograma> [línea]
```

`/vholo clear` elimina únicamente metadata de VHolograms. El texto original de Fancy vuelve a mostrarse.

Permiso: `vholograms.admin`

## Compatibilidad

- Java 21+
- Paper 1.21.4+
- FancyHolograms 2.10.x compatible
- Nexo opcional; los glyphs permanecen dentro del texto de Fancy y se renderizan de la misma forma.

## Migración desde 0.1.0

La versión 0.1.0 almacenaba el texto dentro de la configuración de VHolograms y requería dejar el anchor con `<reset>`. La 0.2.0 detecta ese formato, intenta restaurar el texto original dentro de FancyHolograms y migra únicamente los estilos a `data.yml`.
