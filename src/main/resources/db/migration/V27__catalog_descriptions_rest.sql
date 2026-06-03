-- Phase 17c: bohaté české popisy + zapojené svaly pro zbývajících 72 systémových cviků.
-- Formát dle schváleného vzorku (V19): technika + coaching cues + časté chyby.
-- Pokud se ScoutMetovi něco nelíbí, doupraví si to přes /catalog (klient edituje vlastní katalog).

-- ============================ LOWER BODY ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Dřep s vlastní vahou. Chodidla na šíři ramen, špičky mírně ven, váha přes celé chodidlo. Boky dozadu a dolů, kolena v ose špiček, hrudník nahoru, neutrální páteř. Hloubka ideálně pod paralelu. Časté chyby: kolaps kolen dovnitř, zvedání pat, předklon trupu.'
WHERE name = 'Air Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla, lýtka',
    description = 'Výpad vpřed s osou na zádech. Krok do výpadu, zadní koleno klesá k zemi, přední holeň svislá, trup vzpřímený. Odraz přední patou zpět do stoje. Časté chyby: krátký krok (koleno přepadá přes špičku), náklon trupu, nestabilní osa.'
WHERE name = 'Barbell Lunge' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, lýtka, střed těla',
    description = 'Výskok na bednu z polodřepu. Aktivní švih pažemi, odraz oběma nohama, měkké doskočení do mírného dřepu (tlumit koleny a boky). Z bedny SCHÁZET, neseskakovat. Časté chyby: tvrdé doskočení s propnutými koleny, dohýbání kyčle až nahoře místo plné extenze.'
WHERE name = 'Box Jump' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, lýtka, střed těla',
    description = 'Skok do dálky z místa. Hluboký nápřah s švihem pažemi, explozivní extenze boků, kolen a kotníků vpřed. Měkký doskok do dřepu s váhou na celých chodidlech. Časté chyby: doskok na paty, ztráta rovnováhy vzad, slabý zášvih paží.'
WHERE name = 'Broad Jump' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Bulharský dřep s vlastní vahou — zadní noha na vyvýšení. Většina váhy na přední noze, trup mírně vpřed, přední holeň svislá, koleno v ose. Klesat svisle dolů. Skvělý na sílu jedné nohy a stabilitu. Časté chyby: tlačení ze zadní nohy, koleno padá dovnitř, krátký rozkrok.'
WHERE name = 'Bulgarian Split Squat (BW)' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, kvadricepsy, lýtka',
    description = 'Výskok ze dřepu s vlastní vahou. Klesnout do polodřepu a explozivně vyskočit, měkké doskočení a plynulé navázání dalšího opakování. Časté chyby: mělký dřep, tvrdý doskok, kolena dovnitř, zadržování dechu.'
WHERE name = 'Jump Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Výpad vpřed s vlastní vahou. Krok vpřed, oba kolena cca 90°, zadní koleno k zemi, trup vzpřímený, přední holeň svislá. Odraz přední patou zpět. Časté chyby: koleno přes špičku, předklon, nestabilní kotník.'
WHERE name = 'Forward Lunge' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Zpětný výpad — krok vzad. Šetrnější ke kolenu než vpřed, lepší kontrola. Klesnout zadním kolenem k zemi, přední holeň svislá, trup vzpřímený, vrátit se odrazem přední nohy. Časté chyby: dlouhý/krátký krok, náklon trupu, ztráta rovnováhy.'
WHERE name = 'Reverse Lunge' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, vzpřimovače páteře, střed těla',
    description = 'Čelní dřep — osa v rackové pozici na přední části ramen, lokty vysoko vpřed. Vynucuje vzpřímený trup, šetří bedra. Hrudník nahoru, lokty drží, dřep do hloubky, kolena ven. Časté chyby: padající lokty (osa se kutálí), předklon, mělká hloubka.'
WHERE name = 'Front Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, vzpřimovače páteře, střed těla',
    description = 'Most na hýždě vleže na zádech. Chodidla u hýždí, tlak přes paty, zvednout boky do přímky rameno–koleno, ve vrcholu maximálně zaťaté hýždě. Žebra dolů, nepřehýbat se v bedrech. Časté chyby: extenze z beder místo z hýždí, neúplný zámek, nohy daleko.'
WHERE name = 'Glute Bridge' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, vzpřimovače páteře, střed těla, adduktory',
    description = 'Hip thrust s osou — lopatky opřené o lavici. Osa přes kyčle (s polstrem), brada zastrčená, žebra dolů. Tlak přes paty, plná extenze kyčle do vodorovného trupu, vrchol = zaťaté hýždě. Časté chyby: hyperextenze beder, zvedání pat, neúplný rozsah.'
WHERE name = 'Hip Thrust' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla, lýtka',
    description = 'Pistol — dřep na jedné noze do plné hloubky, druhá noha natažená vpřed. Vyžaduje sílu, mobilitu kotníku a rovnováhu. Trup mírně vpřed jako protiváha, pata na zemi, kontrolovaně dolů i nahoru. Časté chyby: padání vzad, koleno dovnitř, zvedání paty.'
WHERE name = 'Pistol Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, lýtka, střed těla',
    description = 'Skok s přitažením kolen k hrudníku. Výskok z polodřepu, ve vzduchu kolena nahoru k hrudníku, měkké doskočení a okamžité navázání. Vysoká intenzita. Časté chyby: záklon trupu, tvrdý doskok, ztráta rytmu.'
WHERE name = 'Tuck Jump' AND is_system = TRUE;

-- ============================ HINGE / DEADLIFTY ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, vzpřimovače páteře, předloktí, střed těla',
    description = 'Mrtvý tah s kettlebellem mezi nohama. Hinge — boky dozadu, neutrální záda, kettlebell pod kyčlemi. Tah přes paty, plná extenze kyčle, zámek = zaťaté hýždě. Skvělý nácvik hinge před swingem. Časté chyby: dřep místo hinge, kulatá bedra, tah rukama.'
WHERE name = 'KB Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, hýždě, vzpřimovače páteře, předloktí',
    description = 'Rumunský mrtvý tah s činkami — důraz na hamstringy a hýždě. Z mírného pokrčení kolen posouvat boky dozadu, činky kloužou podél stehen, záda neutrální, klesat do tahu v hamstringách. Návrat extenzí kyčle. Časté chyby: kulatá záda, dřep, činky daleko od těla.'
WHERE name = 'DB Romanian Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, vzpřimovače páteře, předloktí, lats',
    description = 'Rumunský mrtvý tah s osou — excentrický důraz na hamstringy. Mírně pokrčená kolena (fixní úhel), boky dozadu, osa podél stehen, neutrální páteř, klesat do napětí v hamstringách (cca pod kolena). Návrat zatnutím hýždí. Časté chyby: kulatá bedra, posun osy od těla, ohýbání kolen místo hinge.'
WHERE name = 'Romanian Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, vzpřimovače páteře, adduktory, předloktí, trapézy',
    description = 'Sumo mrtvý tah — široký postoj, špičky ven, ruce uvnitř kolen. Kratší dráha, víc zapojení hýždí a adduktorů, vzpřímenější trup. Hrudník nahoru, kolena ven, tah přes paty. Časté chyby: zaoblená záda, kolena dovnitř, boky vyletí dřív než hrudník.'
WHERE name = 'Sumo Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, střed těla, hýžďový střední sval, kotník',
    description = 'Jednonohý mrtvý tah s kettlebellem — síla a stabilita jedné nohy. Hinge na stojné noze, zadní noha se zvedá v prodloužení trupu (jako „houpačka"), boky vodorovné (neotvírat), kettlebell klesá podél holeně. Časté chyby: rotace pánve, kulatá záda, ztráta rovnováhy.'
WHERE name = 'KB Single-Leg Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, vzpřimovače páteře, ramena, předloktí, střed těla',
    description = 'Jednoruční swing — balistický hinge s jedním kettlebellem. Stejná mechanika jako oboustranný (pohon z boků), navíc anti-rotace středu těla. Volná ruka pracuje souhlasně, ramena v rovině. Vrchol = pevný plank ve stoje. Časté chyby: rotace trupu, dřep místo hinge, tahání rukou.'
WHERE name = 'KB Single-Hand Swing' AND is_system = TRUE;

-- ============================ KB OLYMPIC / GRINDS ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, trapézy, předloktí, střed těla',
    description = 'Double clean — dva kettlebelly ze země/swingu do rackové pozice. Pohon z boků, lokty se zlomí pozdě a zvony „obtočí" předloktí (žádný náraz na zápěstí). Příjem do měkké racky u hrudníku. Časté chyby: tahání bicepsem, náraz zvonů na předloktí, ztráta hinge.'
WHERE name = 'KB Double Clean' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla, horní část zad',
    description = 'Čelní dřep se dvěma kettlebelly v rackové pozici. Lokty u těla, zvony na předloktích, hrudník nahoru, dřep do hloubky s vzpřímeným trupem. Vysoký nárok na střed těla. Časté chyby: padání vpřed, rozjeté lokty, mělká hloubka.'
WHERE name = 'KB Double Front Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, horní část zad, střed těla',
    description = 'Striktní tlak dvou kettlebellů z racky nad hlavu. Pevný plank ve stoje (zaťaté hýždě a břicho), předloktí svisle, tlak po dráze mírně dozadu, plný zámek nad rameny. Časté chyby: záklon v bedrech, asymetrie stran, neúplný zámek.'
WHERE name = 'KB Double Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, ramena, hýždě, kvadricepsy, střed těla',
    description = 'Jerk — výraz kettlebellu (kettlebellů) nad hlavu pomocí nohou. Krátký pokrč kolen (dip), explozivní extenze a podsed pod zátěž do zámku, pak dorovnání nohou. Šetří rameno, umožní víc opakování. Časté chyby: tlačení jen rukama, měkký dip, nestabilní zámek.'
WHERE name = 'KB Jerk' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, ramena, hýždě, kvadricepsy, hamstringy, střed těla',
    description = 'Long cycle — opakovaný clean & jerk se dvěma kettlebelly (girevoj sport). Každý cyklus: clean do racky → jerk nad hlavu → spuštění do swingu. Klíč je dýchání a ekonomika pohybu pro dlouhé série. Časté chyby: zadržování dechu, tvrdé příjmy, ztráta rytmu.'
WHERE name = 'KB Long Cycle (Clean & Jerk)' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, horní část zad, hýždě, kvadricepsy, střed těla',
    description = 'Push press — tlak kettlebellu nad hlavu s dopomocí nohou. Krátký dip kolen, extenze předá zátěži impuls a paže ji dotlačí do zámku. Víc opakování než striktní tlak. Časté chyby: hluboký dřep místo krátkého dipu, předklon, neúplný zámek.'
WHERE name = 'KB Push Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'lats, biceps, zadní ramena, střed těla',
    description = 'Renegade row — přítah kettlebellu v pozici planku (opora o dva zvony). Tělo jako prkno, anti-rotace, přitáhnout jeden zvon k boku bez otáčení pánve, druhá ruka tlačí do země. Časté chyby: rotace boků, propadlá bedra, příliš úzká opora.'
WHERE name = 'KB Renegade Row' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'biceps, zadní ramena, střed těla',
    description = 'Přítah kettlebellu v předklonu (single-arm row). Hinge s neutrálními zády, opora volné ruky, přitáhnout zvon k boku, lopatka vede pohyb, loket u těla. Časté chyby: trhání trupem, kulatá záda, tahání jen bicepsem.'
WHERE name = 'KB Row' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, horní část zad, hrudník',
    description = 'Halo — kroužení kettlebellu kolem hlavy (dnem vzhůru). Mobilizace ramen a hrudní páteře, zpevný střed těla. Zvon obtéká hlavu těsně, lokty vedou, trup se nehýbá. Střídat směry. Časté chyby: záklon, pohyb beder, daleký okruh od hlavy.'
WHERE name = 'KB Halo' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, hýždě, ramena, střed těla',
    description = 'Windmill — zátěž zafixovaná nad hlavou, předklon do strany s rotací. Trénuje mobilitu kyčle a hrudní páteře a stabilitu ramene. Pohled na zvon, vrchní paže svisle, hinge do boku, dolní ruka klouže po noze. Časté chyby: ohnutá vrchní paže, kulatá záda, dřep místo hinge.'
WHERE name = 'KB Windmill' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, hýždě, kvadricepsy, střed těla',
    description = 'Half get-up — první polovina tureckého vstávání (do mostu/sedu a zpět). Nácvik stabilního ramene a aktivace středu těla. Sled: roll-to-elbow → opora o dlaň → most. Pohled na zvon, rameno zapojené. Časté chyby: spěch, odpojené rameno, ohnutý loket.'
WHERE name = 'KB Half Get-Up' AND is_system = TRUE;

-- ============================ KB CARRIES ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'trapézy, střed těla, lýtka, horní část zad',
    description = 'Farmer carry — chůze s kettlebelly podél těla. Hrudník nahoru, ramena zatažená, pevný úchop, krátké svižné kroky, trup vzpřímený. Buduje grip, trapézy a stabilitu trupu. Časté chyby: hrbení, kolébání trupu, kettlebelly se houpou.'
WHERE name = 'KB Farmer Carry' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, triceps, horní část zad, střed těla',
    description = 'Overhead carry — chůze s kettlebellem zafixovaným nad hlavou. Plný zámek paže, biceps u ucha, žebra dolů, zaťatý střed těla. Vysoký nárok na stabilitu ramene. Časté chyby: záklon v bedrech, ohnutý loket, ztráta vertikály paže.'
WHERE name = 'KB Overhead Carry' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'horní část zad, ramena, předloktí, hýždě',
    description = 'Rack carry — chůze s kettlebellem (kettlebelly) v rackové pozici u hrudníku. Lokty u těla, zvon na předloktí, vzpřímený trup, zaťatý střed. Náročné na dýchání pod tlakem. Časté chyby: záklon, povolený střed těla, krčení ramen.'
WHERE name = 'KB Rack Carry' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'obliky, hýžďový střední sval, trapéz, předloktí',
    description = 'Suitcase carry — chůze s jedním kettlebellem u boku (jako kufr). Silná anti-laterální flexe: trup nesmí padat do strany, ramena a boky vodorovné. Časté chyby: náklon k zátěži, krčení ramene, rychlé klopýtavé kroky.'
WHERE name = 'KB Suitcase Carry' AND is_system = TRUE;

-- ============================ ČINKA — TAHY/TLAKY ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'zadní ramena, biceps, vzpřimovače páteře, předloktí',
    description = 'Veslování v předklonu s osou. Hinge cca 45°, neutrální záda, osa visí pod rameny. Přítah k podbřišku/spodním žebrům, lokty u těla, lopatky se stahují. Časté chyby: trhání trupem, kulatá záda, tahání jen pažemi.'
WHERE name = 'Bent-Over Row' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'zadní ramena, biceps, vzpřimovače páteře, předloktí',
    description = 'Pendlay row — explozivní veslo z mrtvého bodu na zemi. Trup vodorovně, každé opakování startuje ze země, ostrý přítah k hrudníku, kontrolované spuštění zpět na zem. Časté chyby: zvedání trupu, použití nohou, neúplné položení na zem.'
WHERE name = 'Pendlay Row' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, přední ramena, horní hrudník',
    description = 'Tlak s činkami na lavici. Větší rozsah a nezávislá práce stran oproti ose. Lopatky stažené, lokty cca 45°, činky klesají k hrudníku a tlačí se nad ramena (mírně k sobě). Časté chyby: rozjeté lokty, ztráta opory lopatek, nesouměrné strany.'
WHERE name = 'DB Bench Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'horní hrudník, přední ramena, triceps',
    description = 'Tlak na šikmé lavici (cca 30°) — důraz na horní hrudník. Lopatky stažené, osa klesá na horní hrudník/klíčky, tlak po mírné diagonále. Časté chyby: příliš velký úhel (mění se v tlak na ramena), odraz osy, rozjeté lokty.'
WHERE name = 'Incline Bench Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, horní část zad, střed těla',
    description = 'Tlak nad hlavu s činkami ve stoje/sedě. Předloktí svisle, žebra dolů, zaťatý střed, tlak do plného zámku nad rameny. Nezávislé strany odhalí asymetrie. Časté chyby: záklon v bedrech, rozjeté lokty, neúplný zámek.'
WHERE name = 'DB Shoulder Press' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'trapézy, nadhřbetní sval',
    description = 'Upažování s jednoručkami — izolace středních deltů. Mírně pokrčené lokty, vést pohyb lokty (ne zápěstími), zvednout do výše ramen, kontrolovaně dolů. Bez švihu. Časté chyby: houpání trupem, krčení ramen, příliš těžká váha.'
WHERE name = 'DB Lateral Raise' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'předloktí, přední rameno',
    description = 'Bicepsový zdvih s jednoručkami. Lokty u těla a fixní, zvednout činky kontrolovaně, supinace zápěstí, plný stah nahoře, pomalá excentrika. Časté chyby: houpání trupem, posun loktů vpřed, švih.'
WHERE name = 'DB Curl' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Goblet dřep s jednoručkou u hrudníku. Lokty dolů mezi kolena, paty na zemi, vzpřímený trup, dřep do hloubky. Výborný nácvik dřepu. Časté chyby: předklon, zvedání pat, kolena dovnitř.'
WHERE name = 'DB Goblet Squat' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Výpad s jednoručkami podél těla. Krok do výpadu, kolena 90°, trup vzpřímený, přední holeň svislá, odraz zpět. Zátěž u boků zvyšuje nárok na grip a stabilitu. Časté chyby: koleno přes špičku, náklon trupu, krátký krok.'
WHERE name = 'DB Lunge' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, střed těla',
    description = 'Bulharský dřep s jednoručkami — zadní noha na lavici. Většina váhy na přední noze, trup mírně vpřed, svislá holeň, kontrolovaně dolů. Silný unilaterální cvik na nohy. Časté chyby: tlak ze zadní nohy, koleno dovnitř, ztráta rovnováhy.'
WHERE name = 'DB Bulgarian Split Squat' AND is_system = TRUE;

-- ============================ ČINKA — OLYMPIC ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hamstringy, trapézy, kvadricepsy, vzpřimovače páteře, předloktí',
    description = 'Power clean — výbušný tah osy ze země do rackové pozice (příjem ve stoji/polodřepu). Tři fáze: tah od země, explozivní extenze (triple extension), podsed pod osu do racky. Časté chyby: brzké tahání rukama, osa daleko od těla, pomalý podsed.'
WHERE name = 'Power Clean' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, triceps, hamstringy, trapézy, kvadricepsy, vzpřimovače páteře',
    description = 'Clean & jerk — osa ze země do racky (clean) a pak výraz nad hlavu pomocí nohou (jerk). Kombinace síly a výbušnosti. Clean s podsedem, jerk s dipem a podsedem pod osu do zámku. Časté chyby: tah rukama, měkký dip, nestabilní zámek nad hlavou.'
WHERE name = 'Clean & Jerk' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, trapézy, hamstringy, kvadricepsy, vzpřimovače páteře, střed těla',
    description = 'Snatch — osa ze země jedním plynulým tahem nad hlavu s podsedem. Technicky nejnáročnější olympijský zdvih. Široký úchop, triple extension, agresivní podsed pod osu do zámku s aktivními rameny. Časté chyby: brzký ohyb loktů, osa daleko, pomalý/mělký podsed.'
WHERE name = 'Snatch' AND is_system = TRUE;

-- ============================ UPPER BODY — BODYWEIGHT ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, přední ramena, střed těla',
    description = 'Klik. Tělo jako prkno (zaťaté hýždě a břicho), ruce pod rameny, lokty cca 45° od těla. Klesnout hrudníkem k zemi, tlak do plné extenze. Časté chyby: propadlá bedra, vystrčená pánev, rozjeté lokty do stran, neúplný rozsah.'
WHERE name = 'Push-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hrudník, přední ramena, střed těla',
    description = 'Diamantový klik — dlaně blízko sebe (prsty do tvaru diamantu), důraz na triceps. Lokty vedou těsně podél těla, tělo jako prkno. Náročnější varianta kliku. Časté chyby: rozjeté lokty, propadlá bedra, krátký rozsah.'
WHERE name = 'Diamond Push-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'triceps, horní část zad, střed těla',
    description = 'Pike push-up — klik ve „stříšce" (boky vysoko), nácvik tlaku nad hlavu vlastní vahou. Hlava klesá mezi ruce, lokty vpřed, tlak zpět. Předstupeň handstand push-upu. Časté chyby: nízké boky (mění se v běžný klik), prohnutá bedra.'
WHERE name = 'Pike Push-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hrudník, přední ramena, triceps, střed těla',
    description = 'Plyometrický klik s tlesknutím. Explozivní tlak tak, aby se ruce odlepily od země a stihlo se tlesknutí, měkké přijetí. Vysoká intenzita pro výbušnou sílu. Časté chyby: propadlá bedra v letu, tvrdé doskočení, neúplný odraz.'
WHERE name = 'Clap Push-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'biceps, zadní ramena, střed těla',
    description = 'Shyb nadhmatem. Mrtvý vis, lopatky se nejdřív zapojí (deprese), tah loktů dolů a vzad, brada nad hrazdu, kontrolované spuštění do plného visu. Časté chyby: kopání nohama (kipping bez záměru), neúplný rozsah, krčení ramen na začátku.'
WHERE name = 'Pull-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'lats, zadní ramena, střed těla',
    description = 'Shyb podhmatem — větší zapojení bicepsu. Mrtvý vis, lopatky zapojené, tah loktů dolů k tělu, brada nad hrazdu. Obvykle snazší než pull-up. Časté chyby: švihání, neúplný vis, brada přes hrazdu jen „natažením krku".'
WHERE name = 'Chin-Up' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'biceps, zadní ramena, střed těla',
    description = 'Australský šplh / nízké veslo — tělo pod tyčí, přitáhnout hrudník k tyči. Tělo jako prkno, paty na zemi, lopatky se stahují, loket u těla. Regrese k pull-upu. Časté chyby: propadlá bedra, neúplný přítah, krčení ramen.'
WHERE name = 'Inverted Row' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'přední ramena, hrudník, střed těla',
    description = 'Dip na bradlech — tlak vlastní vahou. Ramena dole (deprese), mírný náklon trupu vpřed, klesnout do cca 90° v lokti, tlak do extenze. Náročné na ramenní kloub. Časté chyby: příliš hluboké klesání, krčení ramen, houpání.'
WHERE name = 'Dip' AND is_system = TRUE;

-- ============================ CORE ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'bedrokyčlostehenní sval, předloktí, hýždě',
    description = 'Zvedání nohou ve visu. Mrtvý vis, aktivní lopatky, zvednout nohy (pokrčené nebo natažené) bez švihu, kontrolovaně dolů. Anti-extenze a síla středu těla. Časté chyby: houpání, tah z kyčlí bez zapojení břicha, krčení ramen.'
WHERE name = 'Hanging Leg Raise' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'bedrokyčlostehenní sval, kvadricepsy',
    description = 'Hollow hold — izometrická „lodička" na zádech. Bedra přitisknutá k zemi, ramena a nohy mírně nad zem, tělo do mírného „C", napjatý střed těla. Základ gymnastické stability. Časté chyby: prohnutá bedra (mezera pod zády), zadržování dechu, příliš vysoké nohy.'
WHERE name = 'Hollow Hold' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'obliky, hýžďový střední sval, rameno',
    description = 'Boční prkno — opora o předloktí a hranu chodidla. Tělo v přímce, boky nahoře, anti-laterální flexe. Buduje obliky a stabilitu boku. Časté chyby: propadlé boky, rotace trupu, rameno u ucha.'
WHERE name = 'Side Plank' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'bedrokyčlostehenní sval, břišní svaly',
    description = 'Ruský twist — sed v záklonu, rotace trupu ze strany na stranu (s váhou nebo bez). Pata nad zemí, záda rovná, rotace z hrudní páteře, kontrolovaně. Časté chyby: kulatá záda, pohyb jen rukama, příliš rychlé švihy.'
WHERE name = 'Russian Twist' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'bedrokyčlostehenní sval, kvadricepsy',
    description = 'V-up — současné zvednutí natažených nohou a trupu do tvaru „V", dotek rukou k chodidlům. Pohyb z břicha, kontrolovaná excentrika. Časté chyby: švih, pokrčená kolena, prohnutá bedra při spouštění.'
WHERE name = 'V-Up' AND is_system = TRUE;

-- ============================ FULL BODY / KONDICE ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hrudník, ramena, kvadricepsy, hýždě, střed těla',
    description = 'Burpee — z kliku do výskoku. Dřep, ruce na zem, výhoz nohou do planku (klik), přitažení nohou, výskok s tlesknutím nad hlavou. Celotělová kondiční klasika. Časté chyby: propadlá bedra v planku, vynechaný klik/výskok, ztráta rytmu dechu.'
WHERE name = 'Burpee' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, kvadricepsy, hýždě, hamstringy',
    description = 'Horolezec — z planku střídavé rychlé přitahování kolen k hrudníku. Tělo jako prkno (boky nízko), tempo dle cíle (kondice vs. stabilita). Časté chyby: vystrčená pánev, propadlá bedra, dupání místo plynulého pohybu.'
WHERE name = 'Mountain Climber' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, hýždě, kvadricepsy, hrudní páteř',
    description = 'Leopardí lezení — lezení po čtyřech, koleno nad zemí, kontralaterální koordinace (protilehlá ruka–noha). Záda rovná, boky nízko, plynulý pohyb. Rozcvička i kondice/stabilita. Časté chyby: vysoká pánev, rotace trupu, kolena na zemi.'
WHERE name = 'Crawling (Leopard)' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, hrudní páteř, ramena, adduktory',
    description = 'Worlds Greatest Stretch — komplexní dynamický strečink. Výpad vpřed, loket vnitřní ruky k zemi vedle chodidla, pak rotace s otevřením hrudníku a paží nahoru, narovnání zadní nohy. Skvělá rozcvička celého těla. Časté chyby: spěch, zadržování dechu, kulatá záda.'
WHERE name = 'World''s Greatest Stretch' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, kvadricepsy, hrudní páteř',
    description = '90/90 — sed s oběma koleny v pravém úhlu (jedno před tělem, druhé do strany), mobilizace vnitřní a vnější rotace kyčle. Vzpřímený trup, plynulé přepínání stran. Mobilita kyčlí. Časté chyby: hrbení, tlačení do bolesti, nadzvedávání zadku.'
WHERE name = '90/90 Hip Stretch' AND is_system = TRUE;

-- ============================ MONOSTRUKTURNÍ KARDIO ============================

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, lýtka, střed těla',
    description = 'Běh — vytrvalostní lokomoce. Vzpřímené držení, mírný náklon vpřed z kotníků, dopad pod těžiště, uvolněná ramena, rytmické dýchání. Časté chyby: přešlapování (dopad před tělo), záklon, ztuhlá ramena.'
WHERE name = 'Running' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, hamstringy, lýtka, kvadricepsy, střed těla',
    description = 'Sprint — maximální běžecké úsilí na krátké vzdálenosti. Výrazná práce paží, vysoké koleno, agresivní odraz, dopad pod těžiště. Vyžaduje rozcvičení. Časté chyby: nedostatečné zahřátí, přešlapování, ztuhlý horní trup.'
WHERE name = 'Sprint' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'lýtka, předloktí, ramena',
    description = 'Švihadlo — skoky přes lano. Skoky z přední části chodidel, nízké a rychlé, zápěstí točí lano (ne paže), lokty u těla, vzpřímený trup. Koordinace a kondice. Časté chyby: vysoké skoky, točení z ramen, pokrčená kolena při dopadu.'
WHERE name = 'Jumping Rope' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'ramena, hrudník, hýždě, kvadricepsy',
    description = 'Assault bike — kolo s pohonem rukama i nohama. Synchronní tlak/tah pažemi a šlapání, vzpřímený trup, rovnoměrné dýchání. Brutální kondiční nástroj. Časté chyby: práce jen nohama, hrbení, příliš vysoký start bez rozjezdu.'
WHERE name = 'Assault Bike' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'lýtka, hýždě, hamstringy',
    description = 'Jízda na kole / ergometru. Sedlo ve správné výšce (mírně pokrčené koleno dole), kulatý záběr přes celou otáčku, vzpřímená nebo aerodynamická poloha dle cíle. Časté chyby: nízké sedlo (přetížení kolen), kymácení boků, šlapání jen „dolů".'
WHERE name = 'Cycling' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'hýždě, kvadricepsy, lats, vzpřimovače páteře, biceps',
    description = 'Veslařský trenažér. Pořadí: nohy → trup (mírný záklon) → paže k hrudníku, návrat opačně (paže → trup → nohy). Síla z nohou, ne z paží. Časté chyby: brzký záklon, tah jen pažemi, kulatá záda, spěšný návrat.'
WHERE name = 'Rowing (Ergometr)' AND is_system = TRUE;

UPDATE exercise_catalog_item SET secondary_muscles = 'lats, triceps, hýždě, střed těla',
    description = 'Ski erg — imitace klasického lyžování (double poling). Tah shora dolů: zapojit lats a trup hinge pohybem, paže dotáhnou pohyb k bokům, návrat plynule nahoru. Časté chyby: tah jen pažemi, nedostatečný hinge, trhaný rytmus.'
WHERE name = 'Ski Erg' AND is_system = TRUE;
