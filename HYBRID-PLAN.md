# QPlay Hybrid — experimento isolado

## Objetivo

Transformar o QPlay na interface principal do aparelho e aproveitar o Peanuts Native como motor de preparação e execução de jogos. Esta branch é experimental; a branch `main` permanece preservada.

## Base analisada

- Projeto Peanuts Native recebido em ZIP, diretório Android em `android/app`.
- Namespace Peanuts: `dev.nxnative.android`.
- Build de teste padrão: `dev.nxnative.android`; build público: `dev.bananative.android`; builds de teste personalizados podem usar sufixos.
- Ponto de entrada externo: alias `dev.peanuts.launch.LaunchActivity`, direcionado a `FrontendLaunch`.
- Contrato documentado em `android/FRONTENDS.md`: o launcher envia o jogo como URI `ACTION_VIEW` ou como extra de caminho/título. `FrontendLaunch` valida se o jogo está pronto e inicia `GameActivity`; quando o jogo termina, `GameActivity.backToFrontend` retorna ao launcher externo.
- Componentes relevantes: `Game.java`, `GameList.java`, `Frontend.java`, `FrontendLaunch.java`, `GameActivity.java`, `GameSettings.java`, `PackUi.java`, `BuildPipeline` e as telas de configurações.
- O Peanuts não expõe todas as telas internas como API pública. Algumas Activities são privadas; integração profunda exigirá trazer/adaptar código e manter as alterações locais.

## Estado inicial desta branch

- QPlay continua sendo a interface visual e biblioteca.
- A integração existente abre arquivos `.nup` pelo alias do Peanuts.
- O manifesto experimental oferece também a categoria Android HOME. O usuário precisa escolher QPlay como launcher padrão nas configurações do sistema.
- Isto ainda é uma integração entre dois APKs, não uma fusão do código do Peanuts. O objetivo da próxima etapa é trazer os serviços e telas necessárias para dentro do app QPlay Hybrid.

## Arquitetura alvo

1. **Shell QPlay:** HOME, fileira horizontal de capas, perfil, dock, configurações, editor e navegação por controle.
2. **Camada de domínio Peanuts:** biblioteca/identificação de jogos, verificação de build, preparação/empacotamento, configurações por jogo, shaders, drivers e saves.
3. **Adaptadores QPlay:** telas QPlay que chamam os serviços Peanuts, sem duplicar a lógica do motor.
4. **Execução:** iniciar a atividade de jogo Peanuts e voltar à HOME QPlay ao encerrar.
5. **Atualizações:** manter um registro explícito das adaptações QPlay e comparar com cada atualização upstream antes de reaplicar.

## Plano de implementação

### Fase 1 — HOME e ciclo de execução
- Validar QPlay como HOME padrão em aparelho de teste.
- Validar descoberta do pacote Peanuts instalado.
- Validar abertura de um jogo pronto via alias externo.
- Validar retorno à HOME QPlay após encerrar o jogo.
- Validar comportamento do botão HOME físico/controle separadamente; o Android pode reservar esse comando ao sistema.

### Fase 2 — dados reais do Peanuts
- Estudar como importar a biblioteca Peanuts sem copiar manualmente entradas duplicadas.
- Definir uma camada de leitura dos jogos e estado de build.
- Preservar URIs SAF/permissões e o acesso a pastas exigido pelo motor.

### Fase 3 — editor integrado
- Recriar no estilo QPlay as ações reais de preparação, build, validação e atualização.
- Incorporar as configurações por jogo a partir do formato `launcher.json` e/ou dos mecanismos internos suportados.
- Não expor opções que não possam ser persistidas/aplicadas pelo motor.

### Fase 4 — motor no mesmo APK
- Avaliar mover os módulos Android e nativos do Peanuts para o módulo QPlay.
- Ajustar namespace/applicationId, manifest, services, providers, processos e caminhos de armazenamento.
- Resolver dependências, NDK/CMake, assets e condições de licenciamento.
- Só remover o APK externo depois que o fluxo integrado estiver testado.

## Regras de segurança do experimento

- Não alterar `main`.
- Não modificar nem redistribuir o ZIP original.
- Não remover avisos de copyright/licenças.
- Não incluir chaves ou credenciais.
- Cada fase precisa compilar e ser testada no dispositivo antes de avançar.
- O sucesso de compilação não será tratado como prova de funcionamento do jogo ou do controle HOME.
