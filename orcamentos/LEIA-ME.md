# Orçamentos e Recibos (Java / Spring Boot)

## Rodar no seu computador
    mvn spring-boot:run
Abra http://localhost:8080 . Crie a conta em "Criar conta" (código de convite padrão: troque-este-convite).
Usa banco H2 em ./data.

## Colocar na internet (acesso de fora)
Suba esta pasta para um GitHub e publique em Render, Railway, Fly.io ou um VPS (há Dockerfile).
No Render, configure "Root Directory" como `orcamentos`. Defina estas variáveis de ambiente:
- CODIGO_CONVITE -> código que quem for criar conta precisa digitar (TROQUE o padrão!)
- DB_URL, DB_USER, DB_PASSWORD -> PostgreSQL, ex.: jdbc:postgresql://host:5432/banco
  (sem isso os dados ficam em arquivo local e podem ser perdidos em hospedagens gratuitas)

## Como os dados ficam separados
Cada conta (usuário) tem a própria empresa (menu "Minha empresa") e os próprios documentos.
Toda consulta filtra pelo usuário logado, então uma conta nunca enxerga os dados de outra.
