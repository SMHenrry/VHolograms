# VHolograms

**VHolograms** es un add-on para [FancyHolograms](https://github.com/FancyInnovations/FancyPlugins) que permite dar propiedades independientes a cada línea de un holograma de texto sin abandonar el flujo normal de FancyHolograms.

La idea principal es simple:

- **FancyHolograms sigue siendo la fuente de verdad.**
- El texto y las propiedades generales continúan guardándose en FancyHolograms.
- VHolograms solo guarda la metadata adicional que Fancy no puede aplicar de forma independiente a cada línea.
- No necesitas editar `holograms.yml` ni reemplazar el texto por `<reset>`.

## Requisitos

- Java 21+
- Paper 1.21.4+
- FancyHolograms 2.10.x compatible
- Nexo es opcional

Los glyphs de Nexo pueden seguir utilizándose normalmente dentro del texto de FancyHolograms.

## Instalación

1. Instala FancyHolograms.
2. Descarga el JAR de la release más reciente de VHolograms.
3. Colócalo dentro de `plugins/`.
4. Reinicia el servidor.
5. Crea y edita tus hologramas con FancyHolograms como siempre.

VHolograms genera:

```text
plugins/VHolograms/
├── config.yml
└── data.yml
```

No necesitas editar `data.yml` manualmente durante el uso normal.

## Uso rápido

Crea un holograma de texto de Fancy:

```text
/fholo create text ejemplo
```

FancyHolograms crea su línea inicial habitual. Puedes sustituirla normalmente:

```text
/fholo edit ejemplo setline 1 &6Mi texto
```

Hasta este punto el holograma sigue siendo 100 % FancyHolograms.

Para activar una propiedad independiente en esa línea, añade una opción de VHolograms al final:

```text
/fholo edit ejemplo setline 1 &6Mi texto --scale 1.5
```

Fancy conserva el texto:

```yaml
text:
- '&6Mi texto'
```

Mientras VHolograms conserva únicamente la metadata adicional:

```yaml
holograms:
  ejemplo:
    lines:
      1:
        scale: 1.5
```

## Opciones por línea

| Opción | Uso |
| --- | --- |
| `--scale` | Escala uniforme de la línea |
| `--scale-x` | Escala horizontal |
| `--scale-y` | Escala vertical |
| `--scale-z` | Escala en profundidad |
| `--offset-x` | Desplaza únicamente esa línea en X |
| `--offset-y` | Desplaza únicamente esa línea en Y |
| `--offset-z` | Desplaza únicamente esa línea en Z |
| `--gap-before` | Añade espacio antes de la línea y desplaza las siguientes |
| `--gap-after` | Añade espacio después de la línea y desplaza las siguientes |
| `--text-shadow` | Sobrescribe el text shadow para esa línea |
| `--see-through` | Sobrescribe see-through para esa línea |

Las opciones pueden combinarse:

```text
/fholo edit ejemplo setline 1 &6Mi texto --scale 1.5 --offset-y 0.10 --gap-after 0.05
```

VHolograms añade sugerencias al autocompletado para estas opciones y sus valores.

## Herencia desde FancyHolograms

Las propiedades generales siguen perteneciendo a FancyHolograms.

Por ejemplo:

```text
/fholo edit ejemplo textshadow true
```

hará que todas las líneas hereden `text_shadow: true`.

Puedes crear una excepción:

```text
/fholo edit ejemplo setline 2 &7Segunda línea --text-shadow false
```

Ahora la línea 2 usa `false`, mientras las demás continúan heredando el valor de Fancy.

Para quitar el override:

```text
/fholo edit ejemplo setline 2 &7Segunda línea --text-shadow inherit
```

También se aceptan `default` y `reset` como equivalentes de `inherit`.

## Diferencia entre offset y gap

`offset-y` mueve **solamente la línea seleccionada**:

```text
Línea 1
        Línea 2  <- offset
Línea 3
```

`gap-before` y `gap-after` modifican el flujo vertical, por lo que también desplazan las líneas posteriores:

```text
Línea 1

        <- gap extra

Línea 2
Línea 3
```

Esto hace que `gap` sea útil para separar secciones y que `offset-y` sea mejor para ajustes visuales finos.

Los gaps pueden ser negativos.

## Añadir líneas

FancyHolograms continúa administrando las líneas:

```text
/fholo edit ejemplo addline &7Segunda línea
```

También puedes crear la línea con propiedades avanzadas directamente:

```text
/fholo edit ejemplo addline &7Segunda línea --scale 0.9 --offset-y -0.03
```

## Insertar y eliminar líneas

VHolograms sigue los comandos nativos de FancyHolograms:

```text
/fholo edit ejemplo insertbefore 2 &aNueva línea
/fholo edit ejemplo insertafter 2 &bOtra línea
/fholo edit ejemplo removeline 2
```

Si el holograma ya tiene metadata avanzada, VHolograms reajusta automáticamente los números de línea para mantener cada estilo unido al contenido correcto.

Las líneas insertadas también pueden recibir opciones inmediatamente:

```text
/fholo edit ejemplo insertbefore 2 &aNueva línea --scale 1.25 --gap-after 0.10
```

## Propiedades generales de Fancy

Puedes seguir utilizando normalmente las opciones de FancyHolograms:

```text
/fholo edit ejemplo background transparent
/fholo edit ejemplo textshadow true
/fholo edit ejemplo seethrough false
/fholo edit ejemplo visibilitydistance 24
/fholo edit ejemplo translate 0 -0.15 0
/fholo edit ejemplo scale 1 1 1
```

VHolograms hereda estas propiedades y solo las sobrescribe donde exista una excepción por línea.

## Nexo / glyphs

El texto permanece almacenado en FancyHolograms, así que los glyphs pueden utilizarse igual que antes:

```text
/fholo edit ejemplo setline 1 <glyph:essential_icons_dialogue> --scale 0.85
```

VHolograms no reemplaza ni interpreta el glyph: conserva el texto y deja que el sistema utilizado por FancyHolograms lo renderice.

## Comandos de VHolograms

```text
/vholo status
/vholo reload
/vholo rebuild
/vholo rebuild <holograma>
/vholo clear <holograma>
/vholo clear <holograma> <línea>
```

Permiso:

```text
vholograms.admin
```

### `/vholo clear`

Elimina metadata avanzada sin borrar el texto de FancyHolograms.

Para una sola línea:

```text
/vholo clear ejemplo 2
```

Para todo el holograma:

```text
/vholo clear ejemplo
```

Si ya no queda ninguna propiedad avanzada, VHolograms deja de componer el holograma y FancyHolograms vuelve a renderizar su TextDisplay original.

## Cómo funciona internamente

Un holograma normal de FancyHolograms utiliza un único TextDisplay para todas sus líneas. Minecraft aplica transformaciones como la escala al display completo, no a cada línea individual.

Cuando un holograma recibe metadata de VHolograms:

1. FancyHolograms mantiene intactos el texto y los datos originales.
2. VHolograms evita que el TextDisplay original se muestre al jugador.
3. Crea un TextDisplay temporal por cada línea.
4. Cada línea hereda las propiedades generales del holograma original.
5. VHolograms aplica sus overrides individuales.
6. Los displays temporales siguen la visibilidad del holograma original.

Estos displays son **temporales y no persistentes**, por lo que no se escriben en `holograms.yml`.

## Rendimiento

VHolograms no añade un loop propio por tick.

El trabajo adicional ocurre principalmente cuando:

- se activa o desactiva un holograma avanzado;
- FancyHolograms actualiza el holograma;
- se cambia el texto;
- se muestra u oculta para un jugador;
- se ejecuta un rebuild/reload.

El coste visual aumenta con la cantidad de líneas avanzadas visibles porque cada línea necesita su propio TextDisplay. Para hologramas estáticos y distancias de visibilidad razonables, el comportamiento está pensado para mantenerse ligero.

## Compatibilidad con TEXT, ITEM y BLOCK

FancyHolograms maneja `TEXT`, `ITEM` y `BLOCK` como tipos de holograma distintos.

VHolograms actualmente extiende únicamente hologramas `TEXT`. No mezcla `ITEM` o `BLOCK` dentro del mismo holograma lógico.

## Ejemplo completo

```text
/fholo create text caja

/fholo edit caja setline 1 <#FF80B5><bold>ᴄᴀᴊᴀ</bold> --scale 1.8 --gap-after 0.04
/fholo edit caja addline <#FF80B5><bold>ᴋᴏᴛʜ</bold> --scale 1.8 --gap-after 0.08
/fholo edit caja addline <gray>Calidad <#FF80B5>★★★★★</#FF80B5></gray> --gap-after 0.12
/fholo edit caja addline <white>ᴄʟɪᴄᴋ ᴘᴀʀᴀ ᴀʙʀɪʀ</white>

/fholo edit caja background transparent
/fholo edit caja textshadow true
/fholo edit caja visibilitydistance 24
```

El resultado sigue siendo administrable desde FancyHolograms, pero cada línea puede tener su propia escala y separación gracias a VHolograms.
