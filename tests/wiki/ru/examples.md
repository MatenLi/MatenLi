---
title: Примеры
order: 10
summary: Полные файлы модулей, которые можно копировать, с пояснениями к каждой части.
---

Каждый пример — полный `module.json`. Скопируйте его в файл с таким же именем, положите в папку,
заархивируйте папку и импортируйте ZIP.

## Пример 1: только настройка

Добавляет переключатель в MatenLi без протокола. Полезно, чтобы изучить формат.

```json
{
  "apiVersion": 1,
  "name": "Дополнительный журнал",
  "version": "1.0.0",
  "description": "Пишет в журнал больше подробностей для отладки.",
  "customSettings": [
    {
      "key": "verbose",
      "title": "Подробный журнал",
      "type": "switch",
      "default": false,
      "help": "Показывает дополнительные строки в журнале. Выключите после отладки."
    }
  ]
}
```

## Пример 2: протокол с выбором

Добавляет новый тип ссылки, который работает на sing-box и позволяет выбрать режим.

```json
{
  "apiVersion": 1,
  "name": "Пример туннеля",
  "version": "1.0.0",
  "description": "Демонстрационный протокол, который показывает, как ссылки превращаются в профили.",
  "updateUrl": "https://github.com/your-name/example-tunnel",
  "schemes": ["extunnel"],
  "protocols": [
    {
      "id": "extunnel",
      "name": "Пример туннеля",
      "core": "sing-box",
      "schemes": ["extunnel"],
      "outbound": {
        "type": "socks",
        "server": "${host}",
        "server_port": "${port}",
        "version": "5"
      }
    }
  ],
  "customSettings": [
    {
      "key": "mode",
      "title": "Режим",
      "type": "choice",
      "default": "auto",
      "options": ["auto", "fast", "safe"],
      "help": "Auto выбирает за вас."
    }
  ]
}
```

Ссылка `extunnel://example.org:1080` создаст профиль с хостом `example.org` и портом `1080`.

## Пример 3: помощник-библиотека

Добавляет протокол, которому нужна нативная библиотека. Библиотека лежит в ZIP рядом с `module.json`.

```json
{
  "apiVersion": 1,
  "name": "Протокол с помощником",
  "version": "1.0.0",
  "description": "Запускает библиотеку-помощник, которая нужна протоколу.",
  "helperBinary": { "android": "libhelper.so" },
  "pingTimeoutSec": 30,
  "protocols": [
    {
      "id": "helper-proto",
      "name": "Протокол с помощником",
      "core": "sing-box",
      "schemes": ["helper"],
      "outbound": {
        "type": "socks",
        "server": "127.0.0.1",
        "server_port": "${localPort}"
      }
    }
  ],
  "customSettings": [
    {
      "key": "localPort",
      "title": "Локальный порт",
      "type": "number",
      "default": 1080,
      "min": 1024,
      "max": 65535
    }
  ]
}
```

ZIP тогда содержит `module.json` и `libhelper.so`. Используйте только библиотеку, собранную под процессор телефона
(для почти всех современных Android-телефонов это arm64).

## Пример 4: обновление с GitHub

Укажите в `updateUrl` репозиторий. Каждый новый релиз с большим `version` предлагается как обновление:

```json
{
  "apiVersion": 1,
  "name": "Плагин с обновлениями",
  "version": "2.3.1",
  "description": "Проверяет собственный репозиторий на обновления.",
  "updateUrl": "https://github.com/your-name/updatable-plugin"
}
```

## Чек-лист перед публикацией

- Файл — корректный JSON.
- `apiVersion` равен `1`.
- Каждый `key` в `customSettings` уникален.
- Каждый `${name}` в outbound имеет соответствующий `key` или поле, которое даёт профиль.
- ZIP содержит `module.json` в корне, а не внутри другой папки.
