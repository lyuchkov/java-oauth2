#  Oauth Server

### Примечание

Для тестового запуска необходимо запустить postgres на локальной машине на порту 5432. (либо исправить application.properties)
Запуск контейнера:

```bash
docker run --name oauth-postgre -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres
```