# Loterías y Apuestas del Estado — App para TV y Tabletas

Aplicación web optimizada para televisores y tabletas que muestra en tiempo real los **botes** de los próximos sorteos y los **últimos resultados** de los juegos gestionados por SELAE.

## Características

- 🎰 **Botes** — tarjeta por cada juego con importe, fecha del próximo sorteo y complementos (Joker, El Millón…)
- 🎱 **Resultados** — números en bolas codificadas por color con el premio del 1.º
- 🌓 Modo oscuro automático
- 📺 Layout responsive para TV (1080p / 4K) y tabletas
- 🔄 Auto-refresh configurable (por defecto cada 5 minutos)
- 🛡️ Fallback a datos demo si la API no responde

## Juegos soportados

| id_game | Juego |
|---------|-------|
| 1 | Euromillones |
| 2 | La Primitiva |
| 4 | Bonoloto |
| 5 | El Gordo de la Primitiva |
| 21 | LotoTurf |
| 25 | La Quiniela |
| 26 | QuiniGol |
| 43 | EuroDreams |

## Configuración de la API

Edita las dos primeras constantes en el bloque `<script>` de `index.html`:

```js
const API_BOTES   = "https://TU-API.COM/botes";
const API_RESULTS = "https://TU-API.COM/resultados";
const REFRESH_MS  = 5 * 60 * 1000; // auto-refresh en ms
```

### Formato JSON esperado — Botes (`API_BOTES`)

```json
{
  "data": [
    {
      "id_game": 1,
      "name": "Euromillones",
      "date": "2026-04-25",
      "jackpot": 63000000,
      "addons": [{ "name": "El Millón", "jackpot": 1000000 }]
    }
  ]
}
```

### Formato JSON esperado — Resultados (`API_RESULTS`)

```json
{
  "data": [
    {
      "id_game": 1,
      "name": "Euromillones",
      "date": "2026-04-22",
      "numbers": [3, 15, 23, 41, 48],
      "stars": [2, 9],
      "prize": 45000000,
      "winners": 0
    },
    {
      "id_game": 2,
      "name": "La Primitiva",
      "date": "2026-04-21",
      "numbers": [7, 14, 22, 33, 40, 42],
      "complement": 5,
      "reintegro": 3,
      "prize": 5200000,
      "winners": 1
    }
  ]
}
```

## Despliegue

Es un único fichero HTML estático, sin dependencias externas. Se puede servir con cualquier servidor web:

```bash
# Python
python3 -m http.server 8080

# Node (npx)
npx serve .

# Nginx / Apache — copia index.html a la raíz del sitio
```

## Licencia

MIT
