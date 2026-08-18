# CineReserve

Система за резервация на кинобилети — програма, избор на място, каса, бар и админ панел.

Работи на твоя компютър с **Java 17**, **Spring Boot** и **MySQL**.

## Какво ти трябва

1. **Java 17** (не Java 8). Eclipse Temurin / Adoptium JDK 17 е ок.
2. **MySQL** на `localhost:3306`. Базата `cinema_db` се създава сама, ако я няма.
3. **Maven wrapper** — вече е в проекта (`mvnw.cmd`). Не трябва да инсталираш Maven.

## Настройки за базата

Отвори `src/main/resources/application.properties` и сложи **своята** MySQL парола:

```
spring.datasource.username=root
spring.datasource.password=твоята-парола
```

## Как се пуска

В PowerShell, в папката на проекта:

```powershell
$env:JAVA_HOME = "ПЪТЯТ_КЪМ_JDK_17"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\mvnw.cmd spring-boot:run
```

Чакаш `Started TicketSystemApplication`, после отваряш [http://localhost:8080](http://localhost:8080).

Първият път Maven сваля зависимости и иска интернет.

## С какво да влезеш

При първо пускане се създават:

| Роля   | Потребител | Парола     |
|--------|------------|------------|
| Админ  | `admin`    | `admin123` |
| Касиер | `cashier`  | `cashier123` |
| Клиент | `client`   | `client123` |

Админът прави кина, зали, филми, прожекции и отчети.  
Касиерът продава билети и артикули.  
Клиентът си купува билети от баланса.

Повече подробности: `КАК-ДА-ПУСНА.txt`.
