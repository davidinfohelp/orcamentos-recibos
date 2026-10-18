# Orçamentos e Recibos (Java / Spring Boot)

## Rodar no seu computador
    mvn spring-boot:run
Abra http://localhost:8080 (login padrão: admin / troque-esta-senha). Usa banco H2 em ./data.

## Colocar na internet (acesso de fora)
Suba esta pasta para um GitHub e publique em Render, Railway, Fly.io ou um VPS (há Dockerfile).
Defina estas variáveis de ambiente:
- APP_USER e APP_PASSWORD  -> seu login (TROQUE a senha padrão!)
- DB_URL, DB_USER, DB_PASSWORD -> PostgreSQL, ex.: jdbc:postgresql://host:5432/banco
  (sem isso os dados ficam em arquivo local e podem ser perdidos em hospedagens gratuitas)
