# ReciboSaldo

Aplicación Android de código abierto para llevar el saldo a partir de tickets de compra. No pide cuenta ni envía los datos a ningún servidor: todo se guarda en el teléfono.

También está en inglés. La app usa el idioma del teléfono y se puede cambiar en Ajustes.

## Qué hace

- Saldo inicial, sin registro.
- Foto del ticket o imagen de la galería. El texto se lee en el dispositivo.
- Detecta empresas habituales (Mercadona, Carrefour, Lidl, Amazon, Repsol y otras). Si no detecta la empresa, deja el campo vacío para editarlo.
- Siempre se puede revisar el importe, la empresa, la categoría y la fecha antes de guardar.
- Apuntes manuales de ingreso y gasto, con empresa.
- En Inicio, tabla desplazable con la columna DÍA y la columna TOTAL (recibido, gastado y total del día).
- Diagrama de categorías con el gasto ordenado de mayor a menor.
- Búsqueda por tienda, importe o nota y filtro por meses en Inicio.
- Presupuesto mensual con indicador de gasto en Inicio.
- Gastos que se repiten cada mes (por ejemplo, alquiler o suscripción).
- Copia de seguridad: guardar y recuperar saldo, movimientos y gastos repetidos. Las fotos de los tickets no se incluyen en la copia.
- Revisión de tickets con opciones de girar, recortar y volver a leer la foto, y elegir entre las fechas detectadas.
- Español e inglés.

## Compilar

El workflow de GitHub (`Build APK`) necesita dos secretos configures en el repositorio:

- `RECIBO_STORE_PASSWORD`
- `RECIBO_KEY_PASSWORD`

Para compilar en local, define esas mismas variables como variables de entorno, o añade estas líneas a `local.properties` (archivo ignorado por Git):

```text
recibo.storePassword=...
recibo.keyPassword=...
```
