# Guia para qualquer IA que for editar o QPlay

Launcher Android (Kotlin + Jetpack Compose) com a interface IGUAL à tela inicial do Nintendo Switch.
Leia ISTO antes de mexer. Não reescreva o projeto: altere só o que foi pedido.

## Regras que não podem ser quebradas
1. **Nada de `Dialog` / `AlertDialog` / `Popup` para telas.** Dialog nunca fica em tela cheia (o Android o encolhe com margens).
   Toda tela é uma *página* dentro da MainActivity, usando `SwitchPage(...)` (ui/SwitchComponents.kt).
   Navegação = `enum Screen` + `when (screen)` em `QPlayApp()` (MainActivity.kt). Voltar = `BackHandler` (B do controle também).
2. **Tema claro do Switch.** Fundo `SwitchColors.Bg` (#EBEBEB), texto `SwitchColors.Ink`. Não voltar ao tema escuro.
   Cores ciano/teal vêm de `SwitchColors.Cyan/Teal` (seguem a cor de destaque do usuário). Não use cores fixas para seleção.
3. **Medidas = fração da altura da tela** (`u = maxHeight` dentro de `BoxWithConstraints`). Constantes no topo do MainActivity.kt
   (`TILE_SIZE`, `DOCK_TOP`...). Não use dp fixos para o layout principal.
4. **Foco / controle:** todo item clicável precisa funcionar com direcional + botão A. Use os helpers `gamepadA`, `SwitchButton`,
   `SwitchListItem`, `SwitchTileFrame`. Seleção = borda ciano pulsante. Nunca desenhe `border(0.dp)` (vira um fio fino).
5. **Chaves do SharedPreferences "qplay_library" não mudam** (usuário já tem dados salvos):
   `rom_folder_uri`, `selected_apps`, `display_name`, `profile_avatar_uri`, `favorites`, `accent_color`,
   `edit_name_<key>`, `group_<key>`, `cover_<key>`. `<key>` = uri do .nup ou nome do pacote.
6. Não remover recursos existentes: avatar/nome, favoritos, grupos, filtros, capas, busca de capas, Peanuts, sons.

## Mapa dos arquivos (app/src/main/java/com/qplay/launcher/)
- `MainActivity.kt` — estado, navegação, tela inicial (HomeScreen), GameTile, imersivo (tela cheia), integração Peanuts.
- `Pages.kt` — páginas: SettingsPage (Configurações), EditPage (Personalizar jogo), CoversPage (Buscar capa), AppsPage.
- `CoverCatalog.kt` — catálogo público de ícones do Switch: busca por nome (todas as palavras, sem duplicados), cache em memória/disco.
- `ui/SwitchComponents.kt` — componentes: SwitchPage, SwitchButton, SwitchListItem, SwitchTileFrame, SwitchCircleButton,
  SwitchStatusBar, SwitchAvatar, SwitchBottomBar, campos de texto.
- `ui/Theme.kt` — `SwitchColors` e `QPlayTheme`.

## Como pedir mudanças (exemplos)
- "Adicione uma categoria X em Configurações": editar `SettingsCategories` e o `when (category)` em `SettingsPage` (Pages.kt).
- "Nova página": criar composable com `SwitchPage`, adicionar valor em `enum Screen` e um ramo no `when (screen)`.
- Antes de entregar, conferir: imports, `@OptIn(ExperimentalFoundationApi::class)` onde usar `combinedClickable`,
  lambdas `() -> Unit` (não `() -> Int`), e que o build do GitHub Actions continua passando.
