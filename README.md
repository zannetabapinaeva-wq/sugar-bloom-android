# Sugar Bloom Android 💙

Полностью локальный дневник глюкозы, питания и самочувствия. React + TypeScript + Vite; Android WebViewAssetLoader. Без девушки: озеро, горы и голубые цветы.

- Android 10+ (API 29), target API 35.
- Встроенный интерфейс без загрузки удалённого сайта.
- IndexedDB, отдельный деморежим, календарь, дневник, статистика и личные цели.
- Импорт фото и резервных копий через системный выбор файлов.
- Экспорт PDF/CSV/JSON в Download/SugarBloom.
- Открытые исходники не содержат пользовательских записей или ключей.

## Сборка и скачивание
GitHub Actions собирает и проверяет подпись APK на каждом push в main и публикует файл в Releases. Ссылка после первого успешного релиза: `https://github.com/OWNER/sugar-bloom-android/releases/latest/download/sugar-bloom.apk`.

Сейчас используется тестовая подпись Android. Для стабильных обновлений без переустановки и выпуска в магазин нужно отдельно настроить постоянный приватный ключ подписи через GitHub Secrets.

## Локальная сборка
Node 22+, Java 17, Gradle 8.11.1, Android SDK 35.

```sh
npm ci
npm run build
mkdir -p android/app/src/main/assets
cp -R dist/. android/app/src/main/assets/
gradle -p android assembleDebug
```

При очистке данных или удалении приложения локальный дневник удаляется. Регулярно сохраняйте резервную копию. Приложение не ставит диагнозы и не рассчитывает дозировки лекарств.
