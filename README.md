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
`RABBITMQ_PASSWORD`. Para brokers gerenciados, `RABBITMQ_URL` aceita uma URI
completa, incluindo usuário, senha e virtual host, por exemplo
`amqps://usuario:senha@broker.exemplo.com/meu-virtual-host`. Use `amqp://` para
conexões sem TLS e `amqps://` para conexões TLS; caracteres especiais nas
credenciais devem estar codificados como URL. Nesse formato, não é necessário
definir `RABBITMQ_USERNAME` ou `RABBITMQ_PASSWORD` no serviço OCR. Para manter
instalações existentes, o formato antigo `host:porta` (como `rabbitmq:5672` na
rede Compose ou `localhost:5672` fora dela) continua aceito com usuário e senha
separados.

Ao iniciar, o próprio microsserviço declara as exchanges, filas e bindings que
utiliza caso ainda não existam. A conta RabbitMQ configurada precisa de
permissão para configurar, publicar e consumir no virtual host. Falhas de
processamento são rejeitadas pelo consumidor e encaminhadas para
`ocr-processing-dlq`.

O `.env.example` define `APP_ENV=staging`, que deixa a API OCR e Swagger UI
disponíveis. O endpoint `GET /version` informa a versão em JSON. Sem `APP_ENV`,
o serviço assume produção.

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

O workflow [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) segue este
fluxo:

- Pushes ou merges para `develop` e a abertura/atualização de pull requests
  não executam actions.
- Em cada merge para `main`, executa testes unitários e de integração, agrega
  a cobertura JaCoCo, compila o pacote Quarkus e aguarda o quality gate do
  SonarCloud. Após sucesso, publica a imagem no Docker Hub com a versão de
  `VERSION` e implanta em homologação usando o digest exato da imagem.
- Em cada push para uma branch `release/**`, não executa testes nem rebuild:
  verifica se a imagem versionada já existe no Docker Hub e implanta essa
  versão em produção.

A versão SemVer vem do arquivo [`VERSION`](./VERSION), por exemplo `1.2.3`.
Atualize-o manualmente para cada release. No merge para `main`, o Docker Hub
recebe `v<versão>`, `v<versão>-homologacao` e `sha-<commit>`. O Render de
homologação usa o digest da imagem daquele workflow; produção usa `v<versão>`.
Mantenha a mesma versão no merge da branch `release/**`. Metadados SemVer
(`+build`) são omitidos das tags Docker.

Configure no GitHub, em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `SONAR_TOKEN` | Token de análise do SonarCloud |
| Variable | `SONAR_PROJECT_KEY` | Chave do projeto no SonarCloud |
| Variable | `SONAR_ORGANIZATION` | Identificador da organização SonarCloud |
| Secret | `DOCKERHUB_USERNAME` | Usuário ou organização do Docker Hub |
| Secret | `DOCKERHUB_TOKEN` | Access token do Docker Hub com permissão de escrita |
| Secret | `RENDER_STAGING_DEPLOY_HOOK_URL` | Deploy hook do serviço Render de homologação |
| Secret | `RENDER_PRODUCTION_DEPLOY_HOOK_URL` | Deploy hook do serviço Render de produção |

Configure os dois serviços Render como serviços de imagem Docker usando o
repositório `docker.io/<DOCKERHUB_USERNAME>/scriptum-ocr`; o workflow informa
a tag ou digest a cada deploy. O registry e o repositório configurados no Render
devem corresponder exatamente a esse endereço; somente a tag ou o digest podem
mudar na chamada do deploy hook. Se o repositório Docker Hub for privado,
configure também as credenciais de pull no Render. O fluxo de `main` falha
explicitamente quando faltam credenciais ou o quality gate do SonarCloud não
passa. O deploy de produção falha explicitamente quando a versão ainda não
existe no Docker Hub.

Proteja `main` e `release/**` no GitHub e exija pull request para integrá-las.
O gatilho de push também executa se alguém fizer push direto. A branch
`develop` não está nos gatilhos do workflow.

Em cada serviço Render, configure `RABBITMQ_URL` como URI completa
(`amqps://usuario:senha@host/virtual-host`) com as credenciais do broker
correspondente ao ambiente. O endereço `amqps://` ativa TLS; para conexão sem
TLS, use `amqp://`. O Render injeta `PORT` (por padrão, `10000`), que o serviço
usa para receber tráfego HTTP. O Dockerfile mantém `8080` como padrão para
execução local.

No serviço OCR de homologação, configure `APP_ENV=staging`. Isso mantém os
endpoints OCR e Swagger UI disponíveis. No serviço OCR de produção, não
configure `APP_ENV`: o padrão `production` bloqueia os endpoints REST de OCR
e as rotas Swagger UI/OpenAPI, mantendo `GET /version`, as filas RabbitMQ e os
endpoints operacionais de health e métricas disponíveis. Assim, o consumo e a
publicação de mensagens continuam ativos sem expor o OCR via HTTP.

### RabbitMQ no Render

Como homologação e produção usam os mesmos nomes de filas, crie **um Web
Service RabbitMQ para cada ambiente**; compartilhar um broker faria
consumidores dos dois ambientes competirem pelas mesmas mensagens. O diretório
[`rabbitmq/render`](./rabbitmq/render) contém uma imagem baseada no RabbitMQ
com Management, configurado para usar a porta HTTP do Web Service.

No Dashboard do Render, crie um **Web Service** conectado a este repositório
para homologação e repita o processo para produção. Use a branch desejada,
deixe **Root Directory** vazio e configure em cada serviço:

- **Language:** Docker
- **Dockerfile Path:** `rabbitmq/render/Dockerfile`
- **Health Check Path:** `/`
- **Instance type:** plano pago, para permitir disco persistente
- **Disk:** mount path `/var/lib/rabbitmq`; escolha o tamanho adequado ao volume
  esperado de mensagens e dados
- **Environment Variables:** `RABBITMQ_DEFAULT_USER` e
  `RABBITMQ_DEFAULT_PASS`, com credenciais fortes e exclusivas

O painel Management escuta na porta HTTP `PORT` fornecida pelo Render e fica
acessível publicamente pela URL do Web Service. A autenticação do RabbitMQ
protege o painel; use uma senha longa e exclusiva e, se possível, restrinja
acesso por IP nas configurações do Render. O broker recebe conexões AMQP pela
porta `5672` na rede privada do Render; não configure um TCP proxy público
para AMQP. Após o primeiro deploy, copie o **Service Address** exibido em
**Connect**. No serviço OCR do mesmo workspace e região, configure
`RABBITMQ_URL` com esse endereço e as credenciais em uma URI `amqp://`, por
exemplo `amqp://usuario:senha@host/virtual-host`.

Ao iniciar, o OCR cria automaticamente as exchanges `ocr.processing`,
`ocr.reading` e `ocr.processing.dlx`, as filas `ocr-processing`,
`ocr-processing-dlq` e `ocr-reading-return`, e seus bindings. A fila
`ocr-processing` é criada com dead-letter exchange `ocr.processing.dlx` e
routing key `ocr.processing.failed`.

O disco persistente é necessário: sem ele os dados do broker podem ser perdidos
em reinicializações ou novos deploys. Essa configuração é de instância única e
não oferece alta disponibilidade gerenciada; para dados críticos de produção,
considere um provedor RabbitMQ gerenciado. Como se trata de um Web Service,
o painel Management fica acessível pela internet; use credenciais fortes e
restrição de IP, se disponível.

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
| `RABBITMQ_URL` | URI completa (`amqp://` ou `amqps://`) ou endereço legado `host:porta` |
| `RABBITMQ_USERNAME` | Usuário do formato legado; exigido pelo Compose para criar o broker local |
| `RABBITMQ_PASSWORD` | Senha do formato legado; exigida pelo Compose para criar o broker local |
| `PORT` | `8080` local; definido pelo Render no serviço hospedado |
| `APP_ENV` | `staging` na homologação; omitido em produção (padrão `production`) |
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
teste