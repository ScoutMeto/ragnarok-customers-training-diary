-- Phase 17c (vzorek): bohaté české popisy + svalové partie pro 10 reprezentativních
-- cviků. Formát ke schválení ScoutMetem: technika + coaching cues + časté chyby,
-- plus secondary_muscles (zapojené svaly). Po schválení dotáhneme zbylých ~80.

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hamstringy, střed těla, vzpřimovače páteře, ramena',
    description = 'Základní balistický hinge — pohon jde z hýždí a hamstringů, ne z rukou. Kettlebell se houpe mezi nohama a švihem boků letí do výše hrudníku až očí. Záda neutrální, hrudník otevřený. Vrchol = prudké zaťaté hýždí a břicha (stoj jako „plank"), ruce jsou jen lano. Nádech při spuštění mezi nohy, ostrý výdech ve vrcholu. Časté chyby: dřep místo hinge, kulatá záda, zaklánění a tlačení boků za vrchol.'
WHERE name = 'Kettlebell Swing' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'ramena, hýždě, kvadricepsy, obliky, lopatkové stabilizátory',
    description = 'Komplexní pomalý přechod z lehu do stoje se zátěží zafixovanou nad hlavou. Trénuje stabilitu ramene, sílu středu těla a koordinaci. Sled: roll-to-elbow → opora o dlaň → most (zvednout boky) → protáhnout nohu pod sebe → klek → výpadový stoj, a stejnou cestou zpět. Pohled na zátěž (později vpřed), pohyb pomalu a v každé pozici stabilní. Časté chyby: spěch, propadlé/odpojené rameno, ohnutý loket, ztráta vertikály paže.'
WHERE name = 'KB Turkish Get-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'triceps, horní část zad, střed těla',
    description = 'Striktní tlak z rackové pozice nad hlavu bez pomoci nohou. Lokty pod zápěstím, předloktí svisle. Pevný „plank ve stoje": zaťaté hýždě a břicho, žebra dolů (žádný záklon). Tlak po dráze mírně dozadu, hlava „projde" pod zátěž do plného zámku (biceps u ucha). Nádech a zpevnění před tlakem, výdech v horní fázi. Časté chyby: záklon v bedrech, vystrčená brada, neúplný zámek nahoře.'
WHERE name = 'KB Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Dřep se zátěží drženou oběma rukama u hrudníku (goblet). Skvělý pro nácvik vzpřímeného trupu a hloubky. Lokty směřují dolů mezi kolena, paty na zemi, kolena ven v ose špiček. Dřep do plné hloubky s neutrální páteří. Nádech dolů, výdech nahoru. Časté chyby: předklon trupu, zvedání pat, kolena padají dovnitř.'
WHERE name = 'KB Goblet Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hýždě, hamstringy, vzpřimovače páteře, střed těla',
    description = 'Dřep s osou na zádech (high-bar/low-bar). Osa pevně na trapézech, hrudník nahoru, břicho zpevněné (bracing). Boky a kolena se ohýbají současně, kolena v ose špiček, hloubka pod paralelu. Tlak přes celé chodidlo, kolena „ven". Nádech a zadržení dechu nahoře (Valsalva), výdech po průchodu nejtěžším bodem. Časté chyby: kolaps kolen dovnitř, předklon, mělká hloubka, zvedání pat.'
WHERE name = 'Back Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hamstringy, vzpřimovače páteře, lats, trapézy, předloktí',
    description = 'Mrtvý tah ze země (konvenční). Osa nad středem chodidla, holeně blízko osy, neutrální záda, hrudník nahoru, lats zapojené (chránit osu u těla). Tah přes paty, osa kopíruje nohy, boky a ramena stoupají současně. Zámek = zaťaté hýždě, NE záklon. Nádech a zpevnění (brace) před odtržením. Časté chyby: kulatá bedra, osa daleko od těla, trhání rukama, hyperextenze v zámku.'
WHERE name = 'Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'triceps, přední ramena',
    description = 'Tlak s velkou osou vleže na lavici. Lopatky stažené a zatažené (retrakce + deprese), mírný oblouk v bedrech, chodidla pevně na zemi. Osa klesá na spodní část hrudníku, lokty cca 45° od těla (ne do stran). Tlak po mírné diagonále zpět nad ramena. Nádech při spouštění, výdech v tlaku. Časté chyby: rozjeté lokty, odlepené hýždě, odraz osy od hrudníku.'
WHERE name = 'Bench Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'triceps, horní část zad, střed těla',
    description = 'Striktní tlak osy nad hlavu ve stoji. Úzký nadhmat, lokty pod osou, zápěstí pevná. Pevný trup (hýždě + břicho), žebra dolů — žádný záklon v bedrech. Brada uhne dozadu, osa jde svisle, hlava „projde" pod osu do zámku nad středem těla. Nádech a zpevnění před tlakem. Časté chyby: záklon v bedrech, tlak osy dopředu, neúplný zámek.'
WHERE name = 'Overhead Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hamstringy, střed těla, předloktí, biceps',
    description = 'Přechod kettlebellu ze švihu do rackové pozice. Pohon z hýždí (hinge), zátěž vedená blízko těla, loket se zasune k žebrům a ruka „obejme" zvon — žádný náraz na předloktí. Měkké dosednutí do racku: zvon spočívá na předloktí, zápěstí neutrální, loket dole. Časté chyby: bouchnutí zvonu o předloktí, tahání bicepsem, příliš velký oblouk od těla.'
WHERE name = 'KB Clean' AND is_system = TRUE;

UPDATE exercise_catalog_item SET
    secondary_muscles = 'hamstringy, ramena, střed těla, předloktí',
    description = 'Jednorázový pohyb kettlebellu ze švihu rovnou nad hlavu do zámku. Silný hinge pohon, zátěž zrychluje vzhůru blízko těla; v závěru se ruka „protočí" kolem zvonu (punch through) pro měkké dosednutí bez nárazu na předloktí. Zámek se stabilním ramenem a svislou paží. Nádech při spuštění, výdech v zámku. Časté chyby: náraz zvonu na předloktí, ohnutý loket, ztráta dráhy blízko těla.'
WHERE name = 'KB Snatch' AND is_system = TRUE;
