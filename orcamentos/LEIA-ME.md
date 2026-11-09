# Orçamentos e Recibos (Java / Spring Boot)

## Rodar no seu computador
    mvn spring-boot:run
Abra http://localhost:8080 . Crie a conta em "Criar conta" (código de convite padrão: troque-este-convite).
Usa banco H2 em ./data.

## Rodar na sua VPS (sempre ligado, sem "dormir")
Hospedagens gratuitas (Render etc.) desligam o app depois de ~15 min sem acesso. Na VPS ele fica ligado direto.

### Opção 1 — Docker (recomendado)
Na VPS (Ubuntu), uma única vez:

    curl -fsSL https://get.docker.com | sh
    git clone https://github.com/davidinfohelp/orcamentos-recibos.git
    cd orcamentos-recibos/orcamentos
    cp .env.example .env
    nano .env            # troque DB_PASSWORD e CODIGO_CONVITE
    docker compose up -d --build

Pronto: abra http://IP-DA-VPS:8080 . O app e o banco PostgreSQL sobem com `restart: unless-stopped`,
então voltam sozinhos se travarem ou se a VPS reiniciar. Os dados ficam no volume `dbdata`.

Atualizar depois de mudar o código:

    cd orcamentos-recibos && git pull && cd orcamentos && docker compose up -d --build

Ver logs: `docker compose logs -f app` · Backup do banco:
`docker compose exec db pg_dump -U orcamentos orcamentos > backup-$(date +%F).sql`

### Opção 2 — sem Docker (systemd)
Instale Java 17 (`sudo apt install openjdk-17-jre`), gere o `app.jar` com `mvn package`, copie para `/opt/orcamentos/`,
crie `/opt/orcamentos/.env` com as variáveis (DB_URL, DB_USER, DB_PASSWORD, CODIGO_CONVITE) e use `deploy/orcamentos.service`
(instruções dentro do arquivo).

### Domínio e HTTPS (opcional)
Use `deploy/nginx.conf` + `certbot` (instruções dentro do arquivo). Nesse caso coloque `PORTA=127.0.0.1:8080` no `.env`
para a porta 8080 não ficar aberta direto na internet.

## Base de clientes
Menu **Clientes** no painel. Em "Novo cliente", digite o CNPJ e clique em **Buscar CNPJ**: razão social, nome fantasia,
telefone, e-mail, endereço e situação são preenchidos com os dados públicos da Receita (via BrasilAPI, com a CNPJ.ws
como reserva). CPF é cadastrado manualmente.
No orçamento/recibo há o campo **Buscar cliente salvo** e um botão **Buscar** ao lado do CPF/CNPJ. A caixa
"Salvar este cliente na minha base" (marcada por padrão) cadastra automaticamente quem ainda não estiver na base.
Cada conta tem a própria base de clientes. A VPS precisa conseguir acessar `brasilapi.com.br` e `publica.cnpj.ws`.

## Colocar na internet em outras hospedagens
Suba esta pasta para um GitHub e publique em Render, Railway, Fly.io ou um VPS (há Dockerfile).
No Render, configure "Root Directory" como `orcamentos`. Defina estas variáveis de ambiente:
- CODIGO_CONVITE -> código que quem for criar conta precisa digitar (TROQUE o padrão!)
- DB_URL, DB_USER, DB_PASSWORD -> PostgreSQL, ex.: jdbc:postgresql://host:5432/banco
  (sem isso os dados ficam em arquivo local e podem ser perdidos em hospedagens gratuitas)

## Como os dados ficam separados
Cada conta (usuário) tem a própria empresa (menu "Minha empresa") e os próprios documentos.
Toda consulta filtra pelo usuário logado, então uma conta nunca enxerga os dados de outra.
