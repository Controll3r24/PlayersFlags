# Players Flag  (by Controll3r24)

Plugin per Paper 26.2 che capisce il paese di ogni player dal suo IP e fornisce
il placeholder `%player_flag%` con la bandiera (tag MiniMessage `<head:...>`).

## Compilare
Servono JDK 25 e Gradle 9.1+ (oppure IntelliJ):

    gradle build

Il jar si trova in `build/libs/PlayersFlag-1.0.0.jar` -> mettilo in `plugins/`.
Installa anche PlaceholderAPI (e l'espansione Player: `/papi ecloud download Player`).

## Placeholder
- `%player_flag%`  /  `%playersflag_flag%`  -> bandiera (`<head:...>` o emoji)
- `..._flag_code`, `..._flag_country`, `..._flag_emoji`

Il valore di `%player_flag%` e' una stringa MiniMessage: il plugin che lo usa
(chat, tab, scoreboard...) deve interpretare MiniMessage per mostrare la testa.

## Teste delle bandiere
In `config.yml` -> `flags:` metti `CODICE: valore` (nome, UUID, URL texture o base64
accettati dal tag `<head:...>`). Senza valore viene usata la bandiera emoji.

## Comandi
/pf help | list [pagina] | info [player] | reload | set <player> <codice> | reset <player> | refresh <player> | welcome

Permessi: `playersflag.use` (default tutti), `playersflag.admin` (default op).

## Privacy
Per cercare il paese l'IP viene inviato al servizio configurato in `geolocation.provider-url`
(default ip-api.com, HTTP, gratuito per uso non commerciale). Il plugin salva solo il codice paese
per UUID in `data.yml`, mai l'IP. Informa i tuoi giocatori nella privacy policy del server.
