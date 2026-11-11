# Indicadores de Cadastro (local)

Programa local (Java 17 / Spring Boot) que importa as planilhas mensais de SLA de cadastro
— **Fornecedor**, **Cliente** e **Material** —, identifica cada uma pelas colunas, separa os
registros e mostra os indicadores (por classificação, faixa de prazo, grupo) com histórico mensal.

- Roda só em `127.0.0.1:8765` (não aceita conexões da rede). Banco H2 em arquivo, na pasta `./dados`.
- Leitura de `.xlsx` em streaming (planilha de cliente com ~16 mil linhas lê com 200 MB de memória).
- Nenhuma planilha de dados é versionada (`*.xlsx` está no `.gitignore`).

## Gerar o pacote para usar

    mvn package
    # copie target/app.jar + distribuir/iniciar.bat + distribuir/LEIA-ME.txt para uma pasta

## Rodar em desenvolvimento

    mvn spring-boot:run

## Como cada planilha é reconhecida

| Área | Aba | Colunas que identificam | Inclusão/Manutenção | Prazo |
|---|---|---|---|---|
| Fornecedor | RESUMO | `Supplier -> id`, `NewColumn.Classificação`, `Prazo` | `NewColumn.Classificação` | `Prazo` ("Mais de ..." = fora) |
| Cliente | RESUMO | `Cód. do Cliente`, `Processo`, duas colunas `Indicador` | 1ª `Indicador` | 2ª `Indicador` ("Mais de ..." = fora) |
| Material | Lista | `IdSIN`, `Tipo material`, `Indicador`, `Classificação` | `Classificação` | `Indicador` (Dentro/Fora do Prazo) |

Mês de referência: nome do mês no arquivo → coluna `MÊS` → datas de finalização (texto) → datas (célula),
ou o mês escolhido na tela. Datas de célula com dia/mês trocados (comum nessas exportações) são corrigidas.
