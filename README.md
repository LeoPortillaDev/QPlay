# QPlay — Android prototype 0.1

Aplicativo Android independente, inspirado na navegação de consoles, com foco inicial em celular e tablet Android em modo paisagem.

## O que esta versão contém
- Interface inicial em Jetpack Compose.
- Biblioteca com jogos demonstrativos.
- Tela de detalhes, configurações e menu rápido demonstrativo.
- Projeto preparado para compilar APK Debug pelo GitHub Actions.

## Ainda não implementado
- Descoberta/importação real de atalhos do Peanuts ou de outros aplicativos.
- Inicialização real dos jogos por atalhos.
- Captura global do botão Home de controles enquanto outro app está em primeiro plano.
- Encerramento de aplicativos de terceiros e confirmação de término de processos.
- Suporte validado a controles Xbox, PlayStation e Nintendo em todos os dispositivos.

O botão de fechar nesta versão é apenas demonstrativo. O Android não permite que um aplicativo comum force universalmente o encerramento de outro aplicativo. A integração real será desenvolvida e testada separadamente.

## Compilação pelo GitHub
1. Envie o conteúdo desta pasta para a raiz do repositório `LeoPortillaDev/QPlay` na branch `main`.
2. Abra a aba **Actions** do repositório.
3. Se necessário, habilite GitHub Actions.
4. O workflow **QPlay Android APK** será executado a cada push na `main`, ou poderá ser iniciado manualmente.
5. Ao terminar, abra a execução e baixe o artefato `QPlay-debug-...`.

## Configuração técnica
- Android Gradle Plugin: 8.7.3
- Gradle: 8.9 (instalado no runner do GitHub Actions)
- Kotlin: 2.0.21
- Java: 17
- compileSdk: 35
- minSdk: 26
- applicationId: `com.qplay.launcher`

## Importante
Esta é uma versão inicial de desenvolvimento. O workflow gera um APK Debug para testes, não um APK/AAB assinado para publicação na Google Play. Antes de publicar, será necessário implementar e validar as integrações reais, testar em dispositivos e configurar assinatura de release.
