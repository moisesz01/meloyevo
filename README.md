# Meloyevo

App Android personal para el inventario, la caja y las ventas de la tienda de ropa infantil Meloyevo.
Funciona 100 % sin internet y es compatible desde Android 7.0 (toda la línea Tecno Spark).

## Qué incluye

- **Inventario** con foto, búsqueda por nombre y talla ("body 6"), filtros de talla y de estado.
- **Venta rápida** en 2–3 toques: deslizar una tarjeta o tocar "Vender", varias prendas por venta,
  rebajas puntuales, cobro en pesos o dólares, cálculo del vuelto y "Deshacer".
- **Modo Mostrador / Modo Gestión**: la app siempre abre mostrando solo precios de venta. Costos,
  márgenes, caja y resumen requieren tocar el ojo (y el PIN, si lo creas). Se bloquea sola tras
  1–5 minutos sin uso o al salir de la app.
- **Tasa global**: los precios en pesos se recalculan solos al cambiar la tasa; las prendas con
  precio fijado a mano no cambian.
- **Caja** en USD y COP: las ventas suman solas; entradas, salidas, cambio de divisa y cuadre del día.
- **Resumen** de hoy, la semana y el mes, historial de ventas y devoluciones.
- **Respaldo** en un solo archivo .zip (prendas, fotos, ventas, caja y tasa).

## Cómo obtener el APK

### Opción A · Sin instalar nada (GitHub)

1. Crea una cuenta en github.com y un repositorio **privado** nuevo.
2. Sube todo el contenido de esta carpeta (botón *Add file → Upload files*; arrastra la carpeta completa,
   incluida `.github`).
3. Ve a la pestaña **Actions**. La compilación "Compilar APK" arranca sola y tarda unos 5–8 minutos.
4. Al terminar, entra a la ejecución y descarga **meloyevo-apk** (un .zip con el .apk dentro).
5. Pasa el .apk al celular (WhatsApp, Drive o cable) y ábrelo. Android pedirá permitir
   "instalar apps de origen desconocido" para esa app: acéptalo.

> Si GitHub no muestra la carpeta `.github` al subir, créala a mano: *Add file → Create new file*,
> nombre `.github/workflows/apk.yml`, y pega el contenido del archivo.

### Opción B · Android Studio

1. Instala Android Studio (versión 2024.3 "Meerkat" o más reciente).
2. *File → Open* y elige esta carpeta. Espera a que termine de sincronizar.
3. *Build → Generate App Bundles or APKs → Generate APKs* (variante **release**).
4. El APK queda en `app/build/outputs/apk/release/app-release.apk`.

Usa siempre el APK **release**: el de depuración es mucho más lento en celulares modestos.

## Firma de la app

La llave de firma y sus contraseñas no se guardan en Git. Para generar el APK de release en GitHub
Actions, configura estos secretos en **Settings → Secrets and variables → Actions**:

- `MELOYEVO_KEYSTORE_BASE64`: contenido de `app/meloyevo.jks` codificado en Base64.
- `MELOYEVO_KEYSTORE_PASSWORD`: contraseña del keystore.
- `MELOYEVO_KEY_ALIAS`: alias de la llave.
- `MELOYEVO_KEY_PASSWORD`: contraseña de la llave.

Guarda una copia segura del keystore: todas las versiones futuras deben usar la misma llave para
actualizar la app sin desinstalarla y perder sus datos. No publiques el archivo ni sus contraseñas.

## Primer uso

1. Abre **Caja** (toca el ojo si te lo pide) y ajusta la **tasa** de cambio.
2. Registra una **Entrada** con el efectivo con el que abres la caja ("Saldo inicial"), en pesos y en dólares.
3. En **Resumen → ⚙ Ajustes** crea un **PIN** si otras personas usan el celular, y elige el redondeo.
4. Agrega prendas con **+**: la cámara se abre sola. Usa "Guardar y agregar otra talla" para cargar
   rápido la misma prenda en varias tallas.
5. Crea un **respaldo** cada semana y guárdalo fuera del celular.

## Estructura del código

```
app/src/main/java/com/meloyevo/app/
├── MainActivity.kt        bloqueo al salir y al tocar
├── AppViewModel.kt        estado de toda la app
├── data/                  Room (prendas, ventas, caja), ajustes, fotos y respaldo
├── util/Dinero.kt         formato de moneda, precios, búsqueda, tallas y fechas
└── ui/                    pantallas Compose (Material 3)
```

Tecnología: Kotlin, Jetpack Compose + Material 3, Room, Coil. Sin conexión a internet ni permisos especiales.
