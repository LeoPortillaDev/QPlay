# QPlay — Roadmap técnico

## Objetivo
Aplicativo Android independente para organizar e iniciar jogos e atalhos instalados. O foco inicial é celular e tablet em modo paisagem; TV Box fica para uma etapa futura.

## Princípios
- Não é emulador e não utiliza pastas de ROMs.
- Não substitui a tela inicial do Android.
- A biblioteca guarda referências a aplicativos/atalhos; remover da biblioteca não desinstala o jogo.
- Nunca iniciar outro jogo enquanto a sessão anterior não tiver sido liberada de forma segura.
- Não prometer encerramento forçado de aplicativos de terceiros: usar mecanismos suportados pelo Android e pelo próprio jogo.

## Etapas
1. Biblioteca real: descobrir aplicativos iniciáveis, adicionar/remover entradas e persistir a seleção localmente.
2. Atalhos: investigar e integrar atalhos dinâmicos/fixados compatíveis, incluindo o fluxo criado pelo Peanuts.
3. Personalização: nome de exibição, ícone/capa, favoritos e coleções.
4. Controles: navegação por gamepad via KeyEvent e MotionEvent; mapear Xbox, PlayStation, Switch Pro e controles genéricos.
5. Sessões: registrar aplicativo iniciado, detectar retorno ao QPlay e bloquear uma nova inicialização até liberar a sessão.
6. Menu rápido: implementar apenas ações que o Android permite de forma confiável, documentando limitações de sobreposição e botão Home.

## Primeira entrega funcional
Substituir os jogos fictícios por aplicativos instalados que o Android exponha como iniciáveis. Persistir a biblioteca localmente. Não implementar ainda encerramento forçado nem captura global do botão Home.

## Build
GitHub Actions compila APK Debug. O ambiente de desenvolvimento é GitHub; Android Studio não é requisito para o fluxo do usuário.
