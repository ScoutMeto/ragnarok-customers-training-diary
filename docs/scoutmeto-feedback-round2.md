# Odpověď na zpětnou vazbu (kolo 2)

> Legenda: ✅ **HOTOVO** (nasazeno, prosím o retest) · 🔜 **PŘIJDE** (naplánováno, fáze) · ❓ **OTÁZKA** (potřebuju upřesnit)

---

## ADMIN

### Cviky → pohybový vzorec — chybí možnost pojmenovat vlastní
🔜 **PŘIJDE (Fáze 20a).** Přidám k roletě pohybových vzorců možnost zadat vlastní (volný text).
❓ **OTÁZKA:** Stačí volný text (libovolný název), nebo to chceš jako rozšiřitelný číselník
(systémové + tvoje vlastní, jako u tagů)?

### Cviky → náčiní — chybí možnost zadat vlastní
🔜 **PŘIJDE (Fáze 20b).** Stejný princip jako u pohybového vzorce. (Pozn.: u **klienta** vlastní
náčiní už funguje — píše ho přímo do tréninku; tohle je o **admin katalogu cviků**.)

### Vkládání tréninku jako prostý text (poznámkový blok)
🔜 **PŘIJDE (Fáze 21).**
❓ **OTÁZKA:** V appce už existuje **„Plán od trenéra"** (Markdown text, který klient vidí v menu
„Můj plán"). Je tohle přesně ono, nebo to chceš jinak? Konkrétně:
- má být ten text **u konkrétního tréninku/dne**, nebo jako **samostatný dlouhodobý plán** (jako teď „Můj plán")?
- má si ho klient umět **„rozkliknout do formuláře"** (předvyplnit), nebo stačí jen zobrazit k přečtení?

### Admin nepotřebuje „Můj deník" a „Statistiky" jako User → výběr uživatele
🔜 **PŘIJDE (Fáze 20c).** Přejmenuju na **„Deník uživatele"** / **„Statistiky uživatele"**, po kliknutí
nejdřív roleta výběru klienta, pak jeho deník/statistiky s plnými právy (přidávat, editovat, mazat).
❓ **OTÁZKA:** Když admin v cizím deníku něco upraví/smaže — je OK, že se to chová 1:1 jako by to dělal
sám klient (žádná zvláštní stopa „upravil admin")? Nebo chceš někde poznámku, že zásah udělal trenér?

---

## USER

### Chybí možnost vytvářet vlastní tagy
✅ **HOTOVO.** **Nastavení účtu → „Moje tagy"** — vytvoříš/smažeš vlastní tag (název + barva),
systémové tagy jsou jen ke čtení. Vlastní tag pak vidíš u cviků i tréninků (jen ty).

### Nastavení účtu — notifikace
✅ **HOTOVO.**
- odebráno: „Úvítací mail po registraci" + „Připomínka skupinového tréninku"
- přidáno: **„Potvrzení o vytvoření tréninku"** + **„Deaktivace účtu"** (mail, když tě admin přepne do read-only)
- zbytek beze změny

### Nový trénink → obtížnost
✅ **HOTOVO.** Roleta má teď 3 řádky:
1) Lehký trénink, Deload, Rychlost
2) Silově-kondiční trénink, Sběr opakování, Drill, Výuka
3) Rozvoj maximální síly, Testovací trénink
ℹ️ Pozn.: statistiky pořád počítají na 3 úrovně (beze změny). Výběr jedné z 9 konkrétních variant
už v roletě není (sloučeno do 3) — pokud bys přesto chtěl ukládat i konkrétní variantu, dej vědět.

### Zadávání cviků → unilateralita (levá/pravá)
✅ **HOTOVO.** K tagu **„Unilaterální"** přidány **„Unilaterální levá"** a **„Unilaterální pravá"**
(zaškrtneš podle potřeby; když stranu řešit nechceš, necháš jen „Unilaterální").
❓ **OTÁZKA:** Chápu správně, že to má být formou **tagu** (jak je teď)? Nebo jsi to myslel jako
samostatné pole „strana: L/P" u cviku?

### 2. (a další) cvik nabízí jen FREEFORM / „+Přidat cvik" nereaguje
✅ **OPRAVENO (Fáze 18).** Byla to chyba serializace katalogu, kvůli které se stránka nedorenderovala
celá. Teď má každý přidaný cvik na výběr všechny typy provedení a tlačítko „+Přidat cvik" funguje.
**Prosím otestuj** přidání více cviků různých typů.

### Náčiní — možnost odebrat („−")
✅ **HOTOVO.** **Nastavení účtu → „Moje náčiní"** — u každé vlastní pomůcky tlačítko „−" pro smazání.
(Systémové pomůcky smazat nelze.)

---

## USER i ADMIN

### Editace tréninku nefunguje (chybí tlačítka, IDE error „response already committed")
✅ **OPRAVENO (Fáze 18).** Šlo o stejnou příčinu jako u „2. cviku jen FREEFORM" — chyba při skládání
stránky. Editace tréninku (klient i admin, deník i skupinový i šablona) teď funguje.
**Prosím otestuj** editaci existujícího tréninku (otevřít, změnit, uložit).

---

## Shrnutí pro retest (co je nasazené teď)
1. ✅ **Editace tréninku** (klient i admin) — funguje
2. ✅ **Přidání 2.+ cviku** s plným výběrem typu
3. ✅ **Vlastní tagy** (Nastavení → Moje tagy)
4. ✅ **Mazání vlastního náčiní** (Nastavení → Moje náčiní)
5. ✅ **Notifikace** překopané dle požadavku
6. ✅ **Obtížnost** = 3 řádky
7. ✅ **Unilaterální levá/pravá** (tagy)

## Co přijde v dalším kole
- Admin katalog: vlastní pohybový vzorec + náčiní (20a/b)
- Admin „Deník/Statistiky uživatele" s výběrem klienta (20c)
- Trénink jako prostý text / poznámkový blok (21)

## Otázky → ODPOVĚZENO ScoutMetem (rozhodnutí pro další kolo)
1. **Pohybový vzorec/náčiní v katalogu (20a/b):** ✅ **rozšiřitelný číselník** (system hodnoty
   zůstanou + admin přidá vlastní), u vlastních tlačítko „−" pro odebrání. Jako tagy.
2. **Textový trénink (21):** ✅ **samostatný dlouhodobý plán** (ne k tréninku). Admin: vedle
   „+ Nová šablona" nové tlačítko **„+ Nová šablona v textovém formátu"** = prostý text (poznámkový blok).
   Přiřadí konkrétnímu uživateli → objeví se v **„Můj plán"** jako seznam (identifikátor = datum + název),
   po rozkliknutí jen text. **Uživatel si smí editovat svou kopii** (originál zůstává u trenéra jako šablona).
   Jen čtení + případná editace jako prostý text, žádné „rozbalení do formuláře".
3. **Admin v cizím deníku (20c):** ✅ **bez stopy** — chová se 1:1 jako User.
4. **Unilateralita:** ✅ forma tagu je OK (hotovo v 19d).

---

## 🔧 Retest kolo 2 — opraveno (2026-06-07)
- ✅ **„Přidat tag" nefungovalo** + ✅ **„Moje náčiní" se neaktualizovalo** — **jedna příčina**:
  iterační proměnná `th:each` se jmenovala `eq` (rezervovaný SpEL operátor) → render /settings se uťal
  na smyčce náčiní → náčiní se nezobrazilo a inline tag-JS na konci stránky se vůbec nenačetl
  (mrtvé tlačítko). Přejmenováno → obojí funguje. **Prosím otestuj znovu.**

## ⏭️ Další kolo (dle odpovědí výše)
20a/b (katalog rozšiřitelný číselník), 21 (textová šablona → Můj plán, editovatelná kopie),
20c (admin Deník/Statistiky uživatele).
