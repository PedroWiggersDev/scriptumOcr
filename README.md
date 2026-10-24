# 🔎 Scriptum OCR

Serviço de reconhecimento óptico de caracteres (OCR) desenvolvido com
**Quarkus**, **Tesseract** e **RabbitMQ**.

O serviço pode receber imagens por HTTP ou processá-las de forma assíncrona
por uma fila.

## 📋 Pré-requisitos

- Java 21
- Docker e Docker Compose, somente para executar o ambiente completo
- RabbitMQ, somente para testar a integração real com a fila

O projeto possui Gradle Wrapper, portanto não é necessário instalar o Gradle:

```bash
./gradlew --version
```

## 🚀 Executar em modo desenvolvimento

O modo desenvolvimento habilita recarregamento automático:

```bash
./gradlew quarkusDev
```

Endpoints úteis:

- API OCR: <http://localhost:8080/ocr>
- Swagger UI: <http://localhost:8080/q/swagger-ui>
- Métricas Prometheus: <http://localhost:8080/q/metrics>
- Quarkus Dev UI: <http://localhost:8080/q/dev>

## 📦 Compilar e executar

Gerar o pacote Quarkus:

```bash
./gradlew build
```

Executar o artefato gerado:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

Gerar um JAR único:

```bash
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar
```

## 🌐 API REST

### `POST /ocr`

Processa uma imagem codificada em Base64.

Exemplo de requisição:

```json
{
  "documentId": 123,
  "language": "por",
  "imageBase64": "BASE64_DA_IMAGEM"
}
```

Idiomas disponíveis:

- `por` — português
- `eng` — inglês

Exemplo com `curl`:

```bash
curl -X POST http://localhost:8080/ocr \
  -H 'Content-Type: application/json' \
  -d '{"documentId":123,"language":"por","imageBase64":"BASE64_DA_IMAGEM"}'
```

Resposta esperada:

```json
{
  "documentId": 123,
  "paragraphs": ["Primeiro parágrafo", "Segundo parágrafo"],
  "confidence": 93.4
}
```

## 📨 Filas RabbitMQ

O serviço utiliza os seguintes canais:

| Canal | Finalidade |
|---|---|
| `ocr-processing` | Recebe solicitações de OCR |
| `ocr-reading-return` | Publica as respostas processadas |

As mensagens devem conter o mesmo JSON utilizado pela API REST.

### 🧪 Testar a fila sem RabbitMQ

O teste de integração utiliza o conector em memória:

```bash
./gradlew integrationTest --rerun-tasks
```

Esse teste valida o fluxo completo de entrada, processamento com Tesseract e
publicação da resposta.

### 🐇 Testar com RabbitMQ real

Na raiz do projeto, configure as credenciais locais e inicie o RabbitMQ e o OCR:

```bash
cp .env.example .env
docker compose up --build -d
```

O arquivo `.env` permite configurar `RABBITMQ_URL`, `RABBITMQ_USERNAME` e
`RABBITMQ_PASSWORD`. `RABBITMQ_URL` usa o formato de endereços aceito pelo
conector RabbitMQ (`host:porta`, sem `amqp://`), por exemplo
`rabbitmq:5672` dentro da rede Compose ou `localhost:5672` ao executar o
serviço fora do Compose. Também aceita uma lista separada por vírgulas.

O serviço `rabbitmq-topology` declara exchanges, filas e bindings antes de
iniciar o OCR, usando as credenciais configuradas no `.env`. Falhas de
processamento são rejeitadas pelo consumidor e encaminhadas para
`ocr-processing-dlq`.

Publique uma mensagem na fila de processamento:

```bash
set -a
. ./.env
set +a
curl -u "${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}" \
  -H 'content-type: application/json' \
  -X POST 'http://localhost:15672/api/exchanges/%2F/ocr.processing/publish' \
  --data-binary @- <<'JSON'
{
  "properties": {
    "content_type": "application/json"
  },
  "routing_key": "ocr.processing",
  "payload": "{\"documentId\":2026,\"language\":\"eng\",\"imageBase64\":\"BASE64_DA_IMAGEM\"}",
  "payload_encoding": "string"
}
JSON
```

Consulte a resposta publicada:

```bash
curl -u "${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}" \
  -H 'content-type: application/json' \
  'http://localhost:15672/api/queues/%2F/ocr-reading-return/get' \
  --data-binary @- <<'JSON'
{
  "count": 1,
  "ackmode": "ack_requeue_true",
  "encoding": "auto",
  "truncate": 50000
}
JSON
```

Painel do RabbitMQ: <http://localhost:15672>

- Usuário e senha: os valores definidos no `.env`

## 🧪 Testes

Executar todos os testes:

```bash
./gradlew test --rerun-tasks
```

Executar somente os testes unitários:

```bash
./gradlew unitTest --rerun-tasks
```

Executar somente os testes de integração:

```bash
./gradlew integrationTest --rerun-tasks
```

Os testes exibem:

- `[UNITÁRIO]` ou `[INTEGRAÇÃO]` ao iniciar
- `STARTED`, `PASSED`, `FAILED` ou `SKIPPED`
- Total de testes aprovados, falhos e ignorados

Relatórios HTML:

```text
build/reports/tests/test/index.html
build/reports/tests/unitTest/index.html
build/reports/tests/integrationTest/index.html
```

## 🔄 Integração contínua

O workflow [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) executa em
pull requests e em cada push para `develop` e `main`. A versão do projeto vem
do arquivo [`VERSION`](./VERSION), que deve conter uma versão SemVer, por
exemplo `1.2.3`. Incremente manualmente a versão ao preparar uma mudança:
patch para correções compatíveis, minor para novas funcionalidades compatíveis
e major para alterações incompatíveis.

O workflow roda os testes unitários, gera relatórios JaCoCo, compila o pacote
Quarkus com a versão de `VERSION` e envia métricas ao SonarCloud (incluindo
cobertura, bugs, vulnerabilidades, code smells e duplicação). Em pushes bem
sucedidos para `develop`, publica `v<versão>-develop` e `sha-<commit>` no
Docker Hub. Em pushes para `main`, publica `v<versão>`, `latest` e
`sha-<commit>`. Caso `VERSION` use metadados SemVer (`+build`), estes são
omitidos das tags Docker, que não aceitam o caractere `+`; a versão completa
permanece no artefato Gradle.

Configure no GitHub, em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `SONAR_TOKEN` | Token de análise do SonarCloud |
| Variable | `SONAR_PROJECT_KEY` | Chave do projeto no SonarCloud |
| Variable | `SONAR_ORGANIZATION` | Identificador da organização SonarCloud |
| Secret | `DOCKERHUB_USERNAME` | Usuário ou organização do Docker Hub |
| Secret | `DOCKERHUB_TOKEN` | Access token do Docker Hub com permissão de escrita |

Os passos de análise e publicação são ignorados se seus respectivos
parâmetros não estiverem configurados; testes e build continuam obrigatórios.
Depois da publicação, a imagem fica disponível em
`docker.io/<DOCKERHUB_USERNAME>/scriptum-ocr`.

## 🐳 Executar com Docker Compose

Para iniciar o serviço OCR e sua dependência RabbitMQ:

```bash
cp .env.example .env
docker compose up --build
```

API: <http://localhost:8080/ocr>. O RabbitMQ Management fica em
<http://localhost:15672>. Para encerrar os containers, sem apagar os dados:

```bash
docker compose down
```

O volume `rabbitmq_data` mantém os dados do RabbitMQ entre reinicializações.
Para apagar também as filas e mensagens locais, use `docker compose down -v`.

## 📊 Observabilidade

Os logs do serviço exibem horário e nível com cores no console, sem mostrar a
thread de execução. Mensagens de erro e validação são registradas em
português, sem expor o conteúdo Base64 das imagens.

## ⚙️ Configuração

Configure no `.env` ou no ambiente de execução:

| Variável | Padrão |
|---|---|
| `RABBITMQ_URL` | `rabbitmq:5672` no Compose; `localhost:5672` fora dele |
| `RABBITMQ_USERNAME` | Obrigatório no Compose |
| `RABBITMQ_PASSWORD` | Obrigatório no Compose |
| `RABBITMQ_AMQP_PORT` | `5672` (porta publicada no host) |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` |
| `OCR_HTTP_PORT` | `8080` |

Ao executar o serviço diretamente, `RABBITMQ_HOST` e `RABBITMQ_PORT`
continuam disponíveis como alternativa a `RABBITMQ_URL`. Use credenciais
fortes fora do ambiente local. O endpoint de prontidão do serviço fica em
`/q/health/ready`.

## 🧬 Build nativo

Com GraalVM configurado:

```bash
./gradlew build -Dquarkus.native.enabled=true
```

Usando build nativo em container:

```bash
./gradlew build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true
```
