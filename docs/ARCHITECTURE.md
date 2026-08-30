# Slimes Architecture For AI

Этот файл нужен для будущей разработки с ИИ. Он фиксирует структуру проекта и правила работы.

Важно: стек брать не из Notion. Стек и API брать из локальной документации Cristalix:

- `C:\cristalixdocument\Основа.md`
- `C:\cristalixdocument\DiamondPaper`
- `C:\cristalixdocument\CristalixBukkitCoreApi`
- `C:\cristalixdocument\WorkAndDotApi (WADA)`
- `C:\cristalixdocument\Enginex`
- текущие `build.gradle` и `slimehunt-client/build.gradle`

## Текущий Рабочий Стек

Серверный плагин:

- Minecraft server: DiamondPaper / MC 1.12.2.
- Bukkit/Core API: `ru.cristalix.core:bukkit-api:1.15.5`.
- DiamondPaper dependency: `gg.cristalix:diamondpaper:1.4.11`.
- WADA для серверных красивых меню/HUD/объектов: `gg.cristalix:wada:4.11.0`.
- Java toolchain проекта: Java 25.

Клиентский мод для Enginex:

- Enginex dependency в текущем проекте: `gg.cristalix:enginex:5.0.1`.
- Исходники лежат в `slimehunt-client`.
- Готовый клиентский bundle должен лежать на сервере в папке плагина:
  `plugins/Slimes/SlimeHunt-bundle.jar`.
- Не класть `SlimeHunt-bundle.jar` прямо в `plugins`, иначе Bukkit попробует загрузить его как серверный плагин.

Репозиторий зависимостей:

- `https://repo.c7x.dev/repository/maven-public/`
- креды брать из `~/.gradle/gradle.properties` или env, не хардкодить.

## Что Можно Брать Из Notion

Из Notion можно брать правила организации работы:

- не писать весь режим одним большим классом;
- разделять `service/listener/repository/ui/config/model`;
- делать config-first;
- не додумывать неописанные механики как финальные;
- проектировать так, чтобы потом можно было подключить MongoDB и мультисерверность.

Из Notion не брать устаревшие версии библиотек, Gradle-настройки и стек, если они конфликтуют с документацией Cristalix или текущим проектом.

## Главные Правила

- `Main` только собирает сервисы, запускает lifecycle и регистрирует команды.
- Команды только разбирают ввод и вызывают сервисы.
- Listener-ы только принимают Bukkit-события и передают работу сервисам.
- Сервисы содержат бизнес-логику.
- Repository/DAO отвечает за сохранение.
- Config/provider отвечает за загрузку и валидацию конфигов.
- UI-код держать отдельно от бизнес-логики.
- NMS использовать только при крайней необходимости и изолировать в `dev.lhoopy.adapter`.

## Текущие Пакеты

- `dev.lhoopy`
  - bootstrap плагина.

- `dev.lhoopy.lifecycle`
  - общий lifecycle-контракт `PluginService`.

- `dev.lhoopy.command`
  - Bukkit-команды.

- `dev.lhoopy.config`
  - общие config/provider контракты и ошибки валидации.

- `dev.lhoopy.profile.model`
  - данные игрока: валюта, прогресс, открытые зоны, хранилище.

- `dev.lhoopy.profile.repository`
  - интерфейсы сохранения профиля.
  - `InMemoryPlayerProfileRepository` только временная заглушка.
  - будущая MongoDB-реализация должна реализовать `PlayerProfileRepository`.

- `dev.lhoopy.profile.service`
  - загрузка, выгрузка и кэш профилей.

- `dev.lhoopy.profile.listener`
  - события входа/выхода игрока.

- `dev.lhoopy.slime.model`
  - чистые модели слаймов.

- `dev.lhoopy.slime.config`
  - загрузка и валидация slime-конфигов.

- `dev.lhoopy.slime.registry`
  - реестр валидированных типов слаймов.
  - реестр не читает YAML напрямую.

- `dev.lhoopy.slime.world`
  - живые слаймы в мире, взаимодействие с сущностями, предметы поимки.

- `dev.lhoopy.hunt`
  - тестовая ловля, WADA/Enginex bridge.

- `dev.lhoopy.farm.model/config/repository/service`
  - ферма, растения, семена, грядки, рост, сбор.

- `dev.lhoopy.pen.model/config/repository/service`
  - загоны, слаймы внутри загонов, кормление, производство ресурсов.

- `dev.lhoopy.sell.service`
  - терминал ручной продажи и заказы.

- `dev.lhoopy.craft.service`
  - крафты семян и гибридных ресурсов.

- `dev.lhoopy.adapter`
  - изоляция NMS и внешних API.

## Config-First

- Балансные значения, цены, лимиты, тайминги, шансы и награды должны быть в конфигах.
- Сервисы не читают YAML напрямую, а зависят от provider/registry.
- Конфиги валидируются при старте.
- При ошибке конфига писать понятную ошибку в лог и не запускать сломанную систему молча.

## Cristalix UI Правила

- Серверные готовые меню/HUD/объекты делать через WADA, если это подходит задаче.
- Полноценные кастомные экраны, osu-like ловлю и сложный клиентский UI делать через Enginex client mod.
- Перед добавлением UI читать соответствующий раздел в `C:\cristalixdocument\WorkAndDotApi (WADA)` или `C:\cristalixdocument\Enginex`.
- Не использовать обычные Bukkit Inventory GUI как основной красивый UI, если по документации Cristalix есть подходящий WADA/Enginex вариант.

## Совместимость

- Серверная часть должна быть совместима с DiamondPaper / MC 1.12.2.
- Материалы, звуки, частицы и entity-типы проверять под 1.12.2.
- Не использовать новые Paper API.
- Клиентский Enginex-код живет отдельно в `slimehunt-client`.

## Будущие Системы

Рекомендуемый порядок:

1. ID/registry/config слой.
2. `PlayerProfile` и сохранения.
3. Ферма.
4. Загоны.
5. Слаймы в загонах, кормление, производство ресурсов.
6. Терминал продажи.
7. Крафты.
8. Скрещивание.
9. Ночной цикл, боевой пропуск и сложный прогресс.

## Мини-Чеклист Перед Кодом

- Прочитана ли нужная локальная документация Cristalix?
- Понятно ли, в какой пакет класть код?
- Есть ли config/provider, если добавляется баланс?
- Есть ли model/service/listener/repository/ui разделение?
- Не используется ли устаревший стек из Notion вместо текущего Cristalix-стека?
