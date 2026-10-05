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
- Español e inglés.

## Compilar el APK

1. Instala [Android Studio](https://developer.android.com/studio).
2. Abre esta carpeta.
3. Espera a que Gradle sincronice.
4. Menú Build → Build Bundle(s) / APK(s) → Build APK(s).
5. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

También puedes generar el APK en GitHub: sube este proyecto y abre la pestaña Actions. El flujo `Build APK` deja el archivo para descargar.

## Subir a GitHub

```bash
git init
git add .
git commit -m "Primera versión de ReciboSaldo"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/recibo-saldo.git
git push -u origin main
```

Licencia MIT.
