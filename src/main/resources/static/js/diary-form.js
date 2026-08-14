/**
 * Dynamický editor cviků a setů ve formuláři tréninku.
 *
 * Strategie: každý cvik je <div class="exercise-card"> s data-exercise-index.
 * Při přidání nového cviku z <template id="exerciseTemplate"> a setu z <template id="setRowTemplate">
 * klonujeme template, doplňujeme name="exercises[N].field" / "exercises[N].sets[M].field" a vkládáme
 * do containeru. Při mazání přečíslujeme zbytek (Spring řadí podle indexu v form-binding).
 *
 * Per-type config sekce (.type-config.type-XYZ) se zobrazuje/skrývá podle vybraného typu cviku.
 *
 * ScoutMeto kolo 8: KB sport podrobný záznam po částech, EMOM/Tabata inline tabulky,
 * generovaná tabulka série (Ladder/Stepladder/Pyramid) s validací, Carry jednotka,
 * prefill náčiní z katalogu.
 */
(function () {
    'use strict';

    const container = document.getElementById('exercisesContainer');
    const addExerciseBtn = document.getElementById('addExerciseBtn');
    const exerciseTemplate = document.getElementById('exerciseTemplate');
    const setRowTemplate = document.getElementById('setRowTemplate');

    if (!container || !addExerciseBtn) return;

    // Phase 12: typy, které sdružují víc cviků do kroků (steps) — pro ně skrýváme
    // horní pojmenování cviku (redundantní, A8) a doplňujeme zástupný customName.
    // kolo 10: SUPERSET/COMPLEX jsou režimem CIRCUITu, ne samostatnými typy.
    const GROUPED_TYPES = { CIRCUIT: 'Circuit' };

    // kolo 8 (P37): mapování katalogového náčiní (systémové EN hodnoty) na názvy v "Moje náčiní"
    const EQUIPMENT_LABELS = {
        KETTLEBELL: 'kettlebell', BARBELL: 'osa', BODYWEIGHT: 'bez pomůcek', NONE: 'bez pomůcek',
        DUMBBELL: 'jednoručka', BAND: 'guma', MACHINE: 'stroj', CABLE: 'kladka',
        OTHER: null  // review fix: anglické "other" do pole Náčiní nepatří — prefill přeskočit
    };

    function populateCatalogSelect(selectEl) {
        const items = window.__catalogItems || [];
        items.forEach(ci => {
            const opt = document.createElement('option');
            opt.value = ci.id;
            opt.textContent = ci.name + ' (' + (ci.equipment || '') + ')';
            selectEl.appendChild(opt);
        });
    }

    // Phase 12 (A6/A7): naplní dropdown katalogu v rámci kroku kruhového tréninku.
    // value = název cviku (denormalizovaně se ukládá do textového pole kroku).
    function populateStepCatalog(selectEl) {
        if (!selectEl || selectEl.dataset.populated === '1') return;
        const items = window.__catalogItems || [];
        items.forEach(ci => {
            const opt = document.createElement('option');
            opt.value = ci.name;
            opt.textContent = ci.name + ' (' + (ci.equipment || '') + ')';
            selectEl.appendChild(opt);
        });
        selectEl.dataset.populated = '1';
    }

    function addCircuitStepRow(tbody) {
        const tpl = document.getElementById('circuitStepTemplate');
        const row = tpl.content.firstElementChild.cloneNode(true);
        populateStepCatalog(row.querySelector('.step-catalog-select'));
        tbody.appendChild(row);
        return row;
    }

    // kolo 10: CIRCUIT zastřešuje i SUPERSET a COMPLEX (režim se volí uvnitř bloku)
    const TYPE_LABELS = { CIRCUIT: 'CIRCUIT/SUPERSET/COMPLEX' };

    function populateTypeSelect(selectEl, currentValue) {
        const types = window.__exerciseTypes || ['FREEFORM'];
        types.forEach(t => {
            const opt = document.createElement('option');
            opt.value = t;
            opt.textContent = TYPE_LABELS[t] || t;
            if (t === currentValue) opt.selected = true;
            selectEl.appendChild(opt);
        });
    }

    function updateTypeConfigVisibility(exerciseCard) {
        const typeSelect = exerciseCard.querySelector('.type-select');
        if (!typeSelect) return;
        const selected = typeSelect.value;
        exerciseCard.querySelectorAll('.type-config').forEach(div => {
            const isMatch = div.classList.contains('type-' + selected.toLowerCase());
            div.style.display = isMatch ? '' : 'none';
        });
        // Phase 12 (A6): tabulka sérií jen pro FREEFORM
        const setsBlock = exerciseCard.querySelector('.freeform-sets');
        if (setsBlock) {
            setsBlock.style.display = (selected === 'FREEFORM') ? '' : 'none';
        }

        // Phase 12 (A8): u sdružených typů skryjeme horní pojmenování cviku
        const isGrouped = Object.prototype.hasOwnProperty.call(GROUPED_TYPES, selected);
        exerciseCard.querySelectorAll('.naming-col').forEach(col => {
            col.style.display = isGrouped ? 'none' : '';
        });
        const catalogSel = exerciseCard.querySelector('.catalog-select');
        const customInput = exerciseCard.querySelector('.custom-name-input');
        if (isGrouped) {
            if (catalogSel) catalogSel.value = '';
            if (customInput && !customInput.value.trim()) {
                customInput.value = GROUPED_TYPES[selected];
            }
            ensureMinSteps(exerciseCard, selected);
        }

        // kolo 8: type-specifické inicializace
        if (selected === 'KB_SPORT_TIME') updateKbState(exerciseCard);
        if (selected === 'EMOM') emomSync(exerciseCard, false);
        if (selected === 'TABATA') tabataSync(exerciseCard, false);
        if (selected === 'LADDER' || selected === 'STEPLADDER' || selected === 'PYRAMID') {
            seriesValidate(exerciseCard);
        }
        if (selected === 'CIRCUIT') {
            updateCircuitMode(exerciseCard);
            circuitRoundRestsSync(exerciseCard);
        }
        if (selected === 'STRAIGHT_SETS') {
            straightSetsApplyWeight(exerciseCard);
        }
        if (selected === 'INTERVAL') {
            intervalApplyWeight(exerciseCard);
        }
        if (selected === 'AMRAP') {
            const tbody = exerciseCard.querySelector('.amrap-steps-tbody');
            if (tbody && tbody.querySelectorAll('.amrap-step-row').length === 0) {
                addAmrapStepRow(tbody);
                renumberExercises();
            }
            exerciseCard.querySelectorAll('.amrap-step-row').forEach(updateStepUnits);
            amrapRoundsSync(exerciseCard);
        }
    }

    // ----- kolo 10: Straight Sets + Interval (propis váhy náčiní, generovaná tabulka) -----

    /** Váha náčiní (u dvou zátěží součet) se propisuje do „Váha (kg)" typu cviku. */
    function applyEquipmentWeightTo(card, selector) {
        const input = card.querySelector(selector);
        if (!input) return;
        const eq = equipmentWeights(card);
        if (eq.sum != null) input.value = eq.sum;
    }

    function straightSetsApplyWeight(card) {
        applyEquipmentWeightTo(card, '.ss-weight');
        straightSetsSync(card, false);
    }

    function intervalApplyWeight(card) {
        applyEquipmentWeightTo(card, '.interval-weight');
    }

    /**
     * Tabulka setů se generuje z předpisu (počet setů × opakování × váha × pauza).
     * `overwrite=true` přepíše i vyplněné řádky (změna předpisu), jinak doplní jen prázdné.
     */
    function straightSetsSync(card, overwrite) {
        const block = card.querySelector('.type-config.type-straight_sets');
        if (!block) return;
        const wrap = block.querySelector('.ss-rows-wrap');
        const tbody = block.querySelector('.ss-rows-tbody');
        if (!wrap || !tbody) return;

        const count = parseInt(block.querySelector('.ss-set-count')?.value, 10);
        if (!count || count < 1) { wrap.style.display = 'none'; return; }

        const reps = block.querySelector('.ss-reps')?.value || '';
        const weight = block.querySelector('.ss-weight')?.value || '';
        const restMin = parseInt(block.querySelector('.ss-rest-min')?.value, 10) || 0;
        const restSec = parseInt(block.querySelector('.ss-rest-sec')?.value, 10) || 0;
        const rest = (restMin * 60 + restSec) || '';

        while (tbody.querySelectorAll('tr').length > count) tbody.lastElementChild.remove();
        while (tbody.querySelectorAll('tr').length < count) {
            const tr = document.createElement('tr');
            tr.className = 'ss-row';
            tr.innerHTML = '<td class="row-num"></td>'
                + '<td><input type="number" data-row-field="reps" class="form-control form-control-sm"></td>'
                + '<td><input type="number" step="0.25" data-row-field="weightKg" class="form-control form-control-sm"></td>'
                + '<td><input type="number" data-row-field="restSeconds" class="form-control form-control-sm"></td>';
            tbody.appendChild(tr);
        }
        tbody.querySelectorAll('tr').forEach((tr, i) => {
            tr.querySelector('.row-num').textContent = (i + 1);
            const set = (sel, val) => {
                const el = tr.querySelector(sel);
                if (el && (overwrite || !el.value)) el.value = val;
            };
            set('[data-row-field="reps"]', reps);
            set('[data-row-field="weightKg"]', weight);
            set('[data-row-field="restSeconds"]', rest);
        });
        wrap.style.display = '';
        renumberExercises();
    }

    // ----- kolo 10: AMRAP (sada cviků + záznam po kolech) -----

    function addAmrapStepRow(tbody) {
        const tpl = document.getElementById('amrapStepTemplate');
        if (!tpl || !tbody) return null;
        const row = tpl.content.firstElementChild.cloneNode(true);
        populateStepCatalog(row.querySelector('.step-catalog-select'));
        tbody.appendChild(row);
        return row;
    }

    /**
     * Tabulka odcvičených kol: kolo × cvik. Poslední kolo bývá rozjeté, proto se
     * generuje i ono — u cviku, na který nezbyl čas, se zaškrtne „Nestihnuto".
     * Vyplněné hodnoty se při přegenerování zachovávají (klíč kolo|cvik).
     */
    function amrapRoundsSync(card) {
        const block = card.querySelector('.type-config.type-amrap');
        if (!block) return;
        const wrap = block.querySelector('.amrap-rounds-wrap');
        const tbody = block.querySelector('.amrap-rounds-tbody');
        if (!wrap || !tbody) return;

        const rounds = parseInt(block.querySelector('.amrap-rounds')?.value, 10);
        const steps = [...block.querySelectorAll('.amrap-step-row')];
        if (!rounds || rounds < 1 || steps.length === 0) { wrap.style.display = 'none'; return; }

        // zapamatovat vyplněné hodnoty, ať přegenerování nic nesmaže
        const prev = {};
        tbody.querySelectorAll('tr').forEach(tr => {
            prev[tr.dataset.key] = {
                reps: tr.querySelector('[data-round-field="actualReps"]')?.value || '',
                weight: tr.querySelector('[data-round-field="actualWeightKg"]')?.value || '',
                skipped: !!tr.querySelector('[data-round-field="skipped"]')?.checked
            };
        });

        tbody.innerHTML = '';
        for (let r = 1; r <= rounds; r++) {
            steps.forEach((step, sIdx) => {
                const key = r + '|' + sIdx;
                const saved = prev[key] || {};
                const name = (step.querySelector('.step-name-input')?.value || '').trim() || ('cvik ' + (sIdx + 1));
                const plannedReps = step.querySelector('[data-step-field="reps"]')?.value || '';
                const plannedWeight = step.querySelector('.step-equipment-weight')?.value || '';

                const tr = document.createElement('tr');
                tr.className = 'amrap-round-row';
                tr.dataset.key = key;
                tr.innerHTML =
                    '<td class="round-num">' + (sIdx === 0 ? r : '') + '</td>'
                    + '<td class="small step-name-cell"></td>'
                    + '<td><input type="number" data-round-field="actualReps" class="form-control form-control-sm"></td>'
                    + '<td><input type="number" step="0.25" data-round-field="actualWeightKg" class="form-control form-control-sm"></td>'
                    + '<td class="text-center"><input type="checkbox" data-round-field="skipped" class="form-check-input"></td>';
                // název cviku vkládáme textem (může obsahovat uvozovky/apostrofy)
                tr.querySelector('.step-name-cell').textContent = name;

                const hiddenRound = document.createElement('input');
                hiddenRound.type = 'hidden';
                hiddenRound.setAttribute('data-round-field', 'roundIndex');
                hiddenRound.value = r;
                const hiddenStep = document.createElement('input');
                hiddenStep.type = 'hidden';
                hiddenStep.setAttribute('data-round-field', 'stepOrder');
                hiddenStep.value = sIdx;
                tr.firstElementChild.appendChild(hiddenRound);
                tr.firstElementChild.appendChild(hiddenStep);

                tr.querySelector('[data-round-field="actualReps"]').value =
                        saved.reps !== undefined && saved.reps !== '' ? saved.reps : plannedReps;
                tr.querySelector('[data-round-field="actualWeightKg"]').value =
                        saved.weight !== undefined && saved.weight !== '' ? saved.weight : plannedWeight;
                tr.querySelector('[data-round-field="skipped"]').checked = !!saved.skipped;

                tbody.appendChild(tr);
            });
        }
        wrap.style.display = '';
        renumberExercises();
    }

    // ----- kolo 10: CIRCUIT / SUPERSET / COMPLEX -----

    const CIRCUIT_MODE_HELP = {
        CIRCUIT: '',
        SUPERSET: 'Superset je zjednodušeně a velmi zkrácená forma kruhového tréninku, bez pauzy '
            + 'mezi cviky, ale na rozdíl od COMPLEXů je možné pracovat s odlišnou váhou pro cviky. '
            + 'Superset se zaměřuje na stejné svalové skupiny, ale jinak, NEBO na opačné svalové '
            + 'skupiny (biceps, triceps), NEBO na odlišné části těla - horní končetiny X dolní končetiny.',
        COMPLEX: 'Complex je definován jako sled cviků se stejným náčiním o stejné váze pro každý '
            + 'cvik, bez pauzy mezi cviky a ideálně bez přerušení - tedy s absolutní návazností pohybů.'
    };

    function circuitMode(card) {
        const checked = card.querySelector('.circuit-mode:checked');
        return checked ? checked.value : 'CIRCUIT';
    }

    /**
     * Režim mění jen pravidla, ne strukturu:
     * SUPERSET/COMPLEX = bez pauzy mezi cviky (pole zašedne), COMPLEX navíc sdílí
     * náčiní a váhu prvního cviku se všemi ostatními. Pauza na konci kola zůstává vždy.
     */
    function updateCircuitMode(card) {
        const block = card.querySelector('.type-config.type-circuit');
        if (!block) return;
        const mode = circuitMode(card);

        const title = block.querySelector('.circuit-title');
        if (title) title.textContent = mode === 'COMPLEX' ? 'Complex schéma'
                : mode === 'SUPERSET' ? 'Superset schéma' : 'Circuit schéma';

        const help = block.querySelector('.circuit-mode-help');
        if (help) {
            help.textContent = CIRCUIT_MODE_HELP[mode] || '';
            help.style.display = CIRCUIT_MODE_HELP[mode] ? '' : 'none';
        }

        // pauza mezi cviky: jen CIRCUIT
        const restAllowed = mode === 'CIRCUIT';
        block.querySelectorAll('.step-rest-input').forEach(inp => {
            inp.disabled = !restAllowed;
            if (!restAllowed) inp.value = '';
        });

        if (mode === 'COMPLEX') applyComplexSharedEquipment(block);
    }

    /** COMPLEX: náčiní + váha (+ název) prvního cviku se propíše k ostatním. */
    function applyComplexSharedEquipment(block) {
        const rows = block.querySelectorAll('.circuit-step-row');
        if (rows.length < 2) return;
        const first = rows[0];
        const read = sel => { const el = first.querySelector(sel); return el ? el.value : null; };
        const fields = ['.step-equipment-name', '.step-equipment-weight',
                        '.step-equipment-count', '.step-equipment-second-weight'];
        const src = {};
        fields.forEach(sel => { src[sel] = read(sel); });
        rows.forEach((row, i) => {
            if (i === 0) return;
            fields.forEach(sel => {
                const el = row.querySelector(sel);
                if (el && src[sel] !== null) el.value = src[sel];
            });
            // druhá zátěž: viditelnost se řídí počtem zátěží prvního cviku
            const secondWrap = row.querySelector('.step-equipment-second');
            if (secondWrap) secondWrap.style.display = (src['.step-equipment-count'] === '2') ? '' : 'none';
        });
    }

    /** kolo 10: předvyplněná (editovatelná) tabulka pauz na konci každého kola. */
    function circuitRoundRestsSync(card) {
        const block = card.querySelector('.type-config.type-circuit');
        if (!block) return;
        const wrap = block.querySelector('.circuit-round-rests');
        const tbody = block.querySelector('.circuit-round-rests-tbody');
        if (!wrap || !tbody) return;

        const roundsInput = block.querySelector('[data-name="circuit.rounds"]');
        const rounds = parseInt(roundsInput && roundsInput.value, 10);
        if (!rounds || rounds < 1) { wrap.style.display = 'none'; return; }

        const defMin = block.querySelector('.circuit-rest-min');
        const defSec = block.querySelector('.circuit-rest-sec');

        // dorovnat počet řádků (existující hodnoty se nepřepisují)
        while (tbody.querySelectorAll('tr').length > rounds) tbody.lastElementChild.remove();
        while (tbody.querySelectorAll('tr').length < rounds) {
            const i = tbody.querySelectorAll('tr').length;
            const tr = document.createElement('tr');
            tr.className = 'circuit-round-rest-row';
            tr.innerHTML = '<td class="round-num">' + (i + 1) + '</td>'
                + '<td><input type="number" min="0" data-round-field="restMin" class="form-control form-control-sm"></td>'
                + '<td><input type="number" min="0" max="59" data-round-field="restSec" class="form-control form-control-sm"></td>';
            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.setAttribute('data-round-field', 'roundIndex');
            hidden.value = i;
            tr.querySelector('td').appendChild(hidden);
            tbody.appendChild(tr);
        }
        // předvyplnit prázdné řádky výchozí pauzou mezi koly
        tbody.querySelectorAll('tr').forEach((tr, i) => {
            const num = tr.querySelector('.round-num');
            if (num) num.childNodes[0].nodeValue = (i + 1);
            const ri = tr.querySelector('[data-round-field="roundIndex"]');
            if (ri) ri.value = i;
            const m = tr.querySelector('[data-round-field="restMin"]');
            const sc = tr.querySelector('[data-round-field="restSec"]');
            if (m && !m.value && defMin) m.value = defMin.value;
            if (sc && !sc.value && defSec) sc.value = defSec.value;
        });
        wrap.style.display = '';
        renumberExercises();
    }

    // Phase 12: pro sdružené typy doplní minimální počet prázdných kroků, pokud žádné nejsou.
    function ensureMinSteps(card, selected) {
        if (selected !== 'CIRCUIT') return;
        const tbody = card.querySelector('.circuit-steps-tbody');
        if (tbody && tbody.querySelectorAll('.circuit-step-row').length === 0) {
            for (let i = 0; i < 2; i++) addCircuitStepRow(tbody);
            renumberExercises();
        }
    }

    function renumberExercises() {
        const cards = container.querySelectorAll('.exercise-card');
        cards.forEach((card, idx) => {
            card.setAttribute('data-exercise-index', idx);
            const numEl = card.querySelector('.exercise-number');
            if (numEl) numEl.textContent = (idx + 1) + '.';

            // Top-level fields (type, catalogItemId, customName, rpe, notes, equipment..., setUnit)
            ['type', 'catalogItemId', 'customName', 'rpe', 'notes', 'setUnit',
             'equipmentName', 'equipmentWeightKg', 'equipmentCount', 'equipmentSecondWeightKg'].forEach(fieldName => {
                const el = card.querySelector('[data-name="' + fieldName + '"]');
                if (el) el.name = 'exercises[' + idx + '].' + fieldName;
            });

            // Phase 11 (A2): per-exercise tag checkboxy (víc elementů se stejným name).
            // Pozor: NE tagy uvnitř circuit kroků (ty mají data-step-field).
            card.querySelectorAll('[data-name="tagIds"]').forEach(el => {
                el.name = 'exercises[' + idx + '].tagIds';
            });

            // Per-type config fields (data-name="emom.totalMinutes", "kbSport.intervals[0].reps", ...)
            card.querySelectorAll('[data-name]').forEach(el => {
                const dn = el.getAttribute('data-name');
                if (dn && dn.includes('.')) {
                    el.name = 'exercises[' + idx + '].' + dn;
                }
            });

            // Sets
            const setRows = card.querySelectorAll('.set-row');
            setRows.forEach((row, sIdx) => {
                const numTd = row.querySelector('.set-number');
                if (numTd) numTd.textContent = (sIdx + 1);
                ['weightKg', 'reps', 'restSeconds', 'rpe', 'note'].forEach(fieldName => {
                    const el = row.querySelector('[data-name="' + fieldName + '"]');
                    if (el) el.name = 'exercises[' + idx + '].sets[' + sIdx + '].' + fieldName;
                });
            });

            // kolo 10: Straight Sets — generovaná tabulka setů
            card.querySelectorAll('.ss-row').forEach((row, rIdx) => {
                row.querySelectorAll('[data-row-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].straightSets.rows[' + rIdx + '].'
                            + el.getAttribute('data-row-field');
                });
            });

            // kolo 10: AMRAP — cviky v kole a záznam po kolech
            card.querySelectorAll('.amrap-step-row').forEach((row, sIdx) => {
                const numEl = row.querySelector('.step-num');
                if (numEl) numEl.textContent = (sIdx + 1);
                row.querySelectorAll('[data-step-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].amrap.steps[' + sIdx + '].'
                            + el.getAttribute('data-step-field');
                });
            });
            card.querySelectorAll('.amrap-round-row').forEach((row, rIdx) => {
                row.querySelectorAll('[data-round-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].amrap.roundEntries[' + rIdx + '].'
                            + el.getAttribute('data-round-field');
                });
            });

            // kolo 10: pauzy na konci jednotlivých kol kruhového tréninku
            card.querySelectorAll('.circuit-round-rest-row').forEach((row, rIdx) => {
                row.querySelectorAll('[data-round-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].circuit.roundRests[' + rIdx + '].'
                            + el.getAttribute('data-round-field');
                });
            });

            // Phase 12: circuit kroky (kolo 8: bloky <div> s náčiním a tagy)
            card.querySelectorAll('.circuit-step-row').forEach((row, sIdx) => {
                const numTd = row.querySelector('.step-num');
                if (numTd) numTd.textContent = (sIdx + 1);
                row.querySelectorAll('[data-step-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].circuit.steps[' + sIdx + '].' + el.getAttribute('data-step-field');
                });
            });
        });
    }

    function addExercise() {
        const newCard = exerciseTemplate.content.firstElementChild.cloneNode(true);
        populateCatalogSelect(newCard.querySelector('.catalog-select'));
        populateTypeSelect(newCard.querySelector('.type-select'), 'FREEFORM');
        container.appendChild(newCard);

        for (let i = 0; i < 3; i++) {
            const newRow = setRowTemplate.content.firstElementChild.cloneNode(true);
            newCard.querySelector('.sets-tbody').appendChild(newRow);
        }

        updateTypeConfigVisibility(newCard);
        renumberExercises();
    }

    // =========================================================================
    // kolo 8 — helpery
    // =========================================================================

    /** Váhy náčiní na kartě cviku: {count, w1, w2, sum}. (review fix: 0 kg je platná váha) */
    function equipmentWeights(card) {
        const count = (card.querySelector('.equipment-count') || {}).value || '1';
        const p1 = parseFloat((card.querySelector('[data-name="equipmentWeightKg"]') || {}).value);
        const p2 = parseFloat((card.querySelector('[data-name="equipmentSecondWeightKg"]') || {}).value);
        const w1 = isNaN(p1) ? null : p1;
        const w2 = isNaN(p2) ? null : p2;
        let sum = null;
        if (count === '2' && w1 != null && w2 != null) sum = w1 + w2;
        else if (w1 != null) sum = w1;
        return { count, w1, w2, sum };
    }

    /** Přepíše index v data-name typu "kbSport.intervals[3].reps" na nový. */
    function reindexDataName(el, newIndex) {
        const dn = el.getAttribute('data-name');
        if (dn) el.setAttribute('data-name', dn.replace(/\[\d+\]/, '[' + newIndex + ']'));
    }

    function makeCell(inner) {
        const td = document.createElement('td');
        td.appendChild(inner);
        return td;
    }

    function makeNumberInput(dataName, value, min, max, step) {
        const inp = document.createElement('input');
        inp.type = 'number';
        if (min != null) inp.min = min;
        if (max != null) inp.max = max;
        if (step != null) inp.step = step;
        inp.className = 'form-control form-control-sm';
        inp.setAttribute('data-name', dataName);
        if (value != null && value !== '') inp.value = value;
        return inp;
    }

    // ----- KB sport time (P29) -----

    function updateKbState(card) {
        const block = card.querySelector('.type-config.type-kb_sport_time');
        if (!block) return;
        const eq = equipmentWeights(card);
        const uni = block.querySelector('.kb-detail-unilateral');
        const bi = block.querySelector('.kb-detail-bilateral');
        const uniOk = eq.count === '1' && eq.w1 != null;
        const biOk = eq.count === '2' && eq.w1 != null && eq.w2 != null;
        // Disable jen když není zaškrtnuto (disabled checkbox by se neodeslal → ztráta dat)
        if (uni) uni.disabled = !uniOk && !uni.checked;
        if (bi) bi.disabled = !biOk && !bi.checked;
        const anyChecked = (uni && uni.checked) || (bi && bi.checked);
        const split = block.querySelector('.kb-split-parts');
        if (split) split.disabled = !anyChecked;
        // Strana (L/P) jen pro unilaterální handswitch
        const uniMode = uni && uni.checked;
        block.querySelectorAll('.kb-side-col').forEach(el => {
            el.style.display = uniMode ? '' : 'none';
        });
        if (!uniMode) {
            block.querySelectorAll('.kb-part-side').forEach(sel => { sel.value = ''; });
        }
        // Tabulka viditelná jen s aktivním podrobným záznamem a aspoň 1 řádkem
        const table = block.querySelector('.kb-parts-table');
        const rows = block.querySelectorAll('.kb-part-row').length;
        if (table) table.style.display = (anyChecked && rows > 0) ? '' : 'none';
        if (!anyChecked) {
            // vypnutý podrobný záznam → části se neodešlou (smažeme řádky)
            const tbody = block.querySelector('.kb-parts-tbody');
            if (tbody && rows > 0) { tbody.innerHTML = ''; if (split) split.value = ''; }
            kbValidate(card);
        }
    }

    function kbBuildRow(block, i) {
        const tr = document.createElement('tr');
        tr.className = 'kb-part-row';
        const num = document.createElement('td');
        num.className = 'kb-part-num';
        num.textContent = i + 1;
        tr.appendChild(num);
        const reps = makeNumberInput('kbSport.intervals[' + i + '].reps', '', 0);
        reps.classList.add('kb-part-reps');
        tr.appendChild(makeCell(reps));
        const dm = makeNumberInput('kbSport.intervals[' + i + '].durationMinutes', '', 0, 600);
        dm.classList.add('kb-part-dur-min');
        tr.appendChild(makeCell(dm));
        const ds = makeNumberInput('kbSport.intervals[' + i + '].durationSeconds', '', 0, 59);
        ds.classList.add('kb-part-dur-sec');
        tr.appendChild(makeCell(ds));
        const sideTd = document.createElement('td');
        sideTd.className = 'kb-side-col';
        const sel = document.createElement('select');
        sel.className = 'form-select form-select-sm kb-part-side';
        sel.setAttribute('data-name', 'kbSport.intervals[' + i + '].side');
        ['', 'L', 'P'].forEach(v => {
            const o = document.createElement('option');
            o.value = v; o.textContent = v || '—';
            sel.appendChild(o);
        });
        sideTd.appendChild(sel);
        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.className = 'kb-part-index';
        hidden.setAttribute('data-name', 'kbSport.intervals[' + i + '].intervalIndex');
        hidden.value = i;
        sideTd.appendChild(hidden);
        tr.appendChild(sideTd);
        return tr;
    }

    /** Změna počtu částí → resize tabulky (existující řádky zachová). */
    function kbResizeParts(card) {
        const block = card.querySelector('.type-config.type-kb_sport_time');
        if (!block) return;
        const split = block.querySelector('.kb-split-parts');
        const tbody = block.querySelector('.kb-parts-tbody');
        if (!split || !tbody) return;
        // review fix: přechodně prázdné/nulové pole nesmí zahodit vyplněné řádky
        const want = Math.min(parseInt(split.value, 10) || 0, 120);
        const rows = tbody.querySelectorAll('.kb-part-row');
        if (want <= 0) { updateKbState(card); return; }
        for (let i = rows.length; i < want; i++) tbody.appendChild(kbBuildRow(block, i));
        for (let i = rows.length - 1; i >= want; i--) rows[i].remove();
        // reindex (po odebrání) + přečíslování
        tbody.querySelectorAll('.kb-part-row').forEach((row, i) => {
            row.querySelector('.kb-part-num').textContent = i + 1;
            row.querySelectorAll('[data-name]').forEach(el => reindexDataName(el, i));
            const hid = row.querySelector('.kb-part-index');
            if (hid) hid.value = i;
        });
        renumberExercises();
        updateKbState(card);
        kbValidate(card);
    }

    /** Nevynucující kontrola součtů (opakování + čas) — červená výstraha. */
    function kbValidate(card) {
        const block = card.querySelector('.type-config.type-kb_sport_time');
        if (!block) return;
        const warn = block.querySelector('.kb-parts-warning');
        if (!warn) return;
        const rows = block.querySelectorAll('.kb-part-row');
        if (rows.length === 0) { warn.style.display = 'none'; return; }
        const totalReps = parseInt((block.querySelector('.kb-total-reps') || {}).value, 10);
        const totMin = parseInt((block.querySelector('.kb-total-min') || {}).value, 10) || 0;
        const totSec = parseInt((block.querySelector('.kb-total-sec') || {}).value, 10) || 0;
        const totalTime = totMin * 60 + totSec;
        let sumReps = 0, sumTime = 0;
        rows.forEach(row => {
            sumReps += parseInt((row.querySelector('.kb-part-reps') || {}).value, 10) || 0;
            sumTime += (parseInt((row.querySelector('.kb-part-dur-min') || {}).value, 10) || 0) * 60
                     + (parseInt((row.querySelector('.kb-part-dur-sec') || {}).value, 10) || 0);
        });
        const msgs = [];
        if (!isNaN(totalReps) && totalReps > 0 && sumReps !== totalReps) {
            msgs.push('⚠ Součet opakování v částech (' + sumReps + ') neodpovídá poli „Celkem opakování" (' + totalReps + ').');
        }
        if (totalTime > 0 && sumTime !== totalTime) {
            msgs.push('⚠ Součet časů v částech (' + fmtTime(sumTime) + ') neodpovídá času setu (' + fmtTime(totalTime) + ').');
        }
        warn.textContent = msgs.join(' ');
        warn.style.display = msgs.length ? '' : 'none';
    }

    function fmtTime(sec) {
        return Math.floor(sec / 60) + ':' + String(sec % 60).padStart(2, '0');
    }

    // ----- EMOM (P30) -----

    function emomBuildRow(i, reps, kg) {
        const tr = document.createElement('tr');
        tr.className = 'emom-set-row';
        const num = document.createElement('td');
        num.className = 'num emom-set-num';
        num.textContent = i + 1;
        tr.appendChild(num);
        tr.appendChild(makeCell(makeNumberInput('emom.minuteOverrides[' + i + '].reps', reps, 0)));
        const kgTd = document.createElement('td');
        kgTd.appendChild(makeNumberInput('emom.minuteOverrides[' + i + '].weightKg', kg, null, null, '0.25'));
        const hid = document.createElement('input');
        hid.type = 'hidden';
        hid.className = 'emom-set-index';
        hid.setAttribute('data-name', 'emom.minuteOverrides[' + i + '].minuteIndex');
        hid.value = i + 1; // minuta 1..N
        kgTd.appendChild(hid);
        tr.appendChild(kgTd);
        return tr;
    }

    /** Synchronizace tabulky setů s „Celkem minut" (checkbox „Editovat jednotlivé sety"). */
    function emomSync(card, prefillAll) {
        const block = card.querySelector('.type-config.type-emom');
        if (!block) return;
        const chk = block.querySelector('.emom-edit-sets');
        const setsBlock = block.querySelector('.emom-sets-block');
        const tbody = block.querySelector('.emom-sets-tbody');
        if (!chk || !setsBlock || !tbody) return;
        if (!chk.checked) {
            setsBlock.style.display = 'none';
            tbody.innerHTML = ''; // bez checkboxu se overrides neukládají
            renumberExercises();
            return;
        }
        setsBlock.style.display = '';
        const want = Math.min(parseInt((block.querySelector('.emom-total-minutes') || {}).value, 10) || 0, 120);
        // review fix: přechodně prázdné pole Celkem minut nesmí zahodit vyplněné řádky
        if (want <= 0) return;
        const defReps = (block.querySelector('.emom-default-reps') || {}).value || '';
        const eq = equipmentWeights(card);
        const defKg = eq.sum != null ? eq.sum : '';
        const rows = tbody.querySelectorAll('.emom-set-row');
        for (let i = rows.length; i < want; i++) tbody.appendChild(emomBuildRow(i, defReps, defKg));
        for (let i = rows.length - 1; i >= want; i--) rows[i].remove();
        if (prefillAll) {
            tbody.querySelectorAll('.emom-set-row').forEach(row => {
                const inputs = row.querySelectorAll('input[type="number"]');
                if (inputs[0]) inputs[0].value = defReps;
                if (inputs[1]) inputs[1].value = defKg;
            });
        }
        // review fix: sekvenční přečíslování (legacy řídké overrides by jinak nechaly
        // duplicitní/přeházené minuteIndex)
        tbody.querySelectorAll('.emom-set-row').forEach((row, i) => {
            const num = row.querySelector('.emom-set-num');
            if (num) num.textContent = i + 1;
            const hid = row.querySelector('.emom-set-index');
            if (hid) hid.value = i + 1;
            row.querySelectorAll('[data-name]').forEach(el => reindexDataName(el, i));
        });
        renumberExercises();
    }

    /**
     * kolo 9: změna náčiní (váha / počet zátěží) se OKAMŽITĚ propíše do celé EMOM
     * tabulky (2 zátěže = součet). Konkrétní řádek si pak uživatel upraví sám.
     */
    function emomApplyKg(card) {
        const block = card.querySelector('.type-config.type-emom');
        if (!block) return;
        const chk = block.querySelector('.emom-edit-sets');
        if (!chk || !chk.checked) return;
        const eq = equipmentWeights(card);
        block.querySelectorAll('.emom-set-row').forEach(row => {
            const kgInput = row.querySelectorAll('input[type="number"]')[1];
            if (kgInput) kgInput.value = eq.sum != null ? eq.sum : '';
        });
    }

    /** kolo 9: „Přednastavený počet opakování" okamžitě přepíše všechny řádky EMOM tabulky. */
    function emomApplyReps(card) {
        const block = card.querySelector('.type-config.type-emom');
        if (!block) return;
        const chk = block.querySelector('.emom-edit-sets');
        if (!chk || !chk.checked) return;
        const defReps = (block.querySelector('.emom-default-reps') || {}).value || '';
        block.querySelectorAll('.emom-set-row').forEach(row => {
            const repsInput = row.querySelectorAll('input[type="number"]')[0];
            if (repsInput) repsInput.value = defReps;
        });
    }

    /** kolo 9: změna náčiní okamžitě aktualizuje váhu ve všech řádcích tabulky série. */
    function seriesApplyWeight(card) {
        const eq = equipmentWeights(card);
        card.querySelectorAll('.series-row-weight').forEach(inp => {
            inp.value = eq.sum != null ? eq.sum : '';
        });
    }

    /** kolo 9: po smazání řádku série přepočítá labely („N. série") a indexy. */
    function seriesReindex(card) {
        const tbody = card.querySelector('.series-rows-tbody');
        if (!tbody) return;
        let prevRung = null;
        tbody.querySelectorAll('.series-row').forEach((row, i) => {
            row.querySelectorAll('[data-name]').forEach(el => reindexDataName(el, i));
            const idxHid = row.querySelector('.series-row-index');
            if (idxHid) idxHid.value = i;
            const rungHid = row.querySelector('.series-row-rung');
            const label = row.querySelector('.series-row-label');
            const rung = rungHid ? rungHid.value : null;
            if (label) label.textContent = (rung && rung !== prevRung) ? rung + '. série' : '';
            prevRung = rung;
        });
        renumberExercises();
    }

    // ----- TABATA (P34) -----

    function tabataBuildRow(i, reps) {
        const tr = document.createElement('tr');
        tr.className = 'tabata-round-row';
        const num = document.createElement('td');
        num.className = 'num tabata-round-num';
        num.textContent = i + 1;
        tr.appendChild(num);
        const td = document.createElement('td');
        td.appendChild(makeNumberInput('tabata.roundOverrides[' + i + '].reps', reps, 0));
        const hid = document.createElement('input');
        hid.type = 'hidden';
        hid.className = 'tabata-round-index';
        hid.setAttribute('data-name', 'tabata.roundOverrides[' + i + '].roundIndex');
        hid.value = i + 1;
        td.appendChild(hid);
        tr.appendChild(td);
        return tr;
    }

    /** Tabulka kol dle „Kola" — předvyplněná předdefinovaným počtem opakování. */
    function tabataSync(card, prefillAll) {
        const block = card.querySelector('.type-config.type-tabata');
        if (!block) return;
        const tbody = block.querySelector('.tabata-rounds-tbody');
        const wrap = block.querySelector('.tabata-rounds-block');
        if (!tbody || !wrap) return;
        const want = Math.min(parseInt((block.querySelector('.tabata-rounds') || {}).value, 10) || 0, 30);
        const defReps = (block.querySelector('.tabata-default-reps') || {}).value || '';
        // Tabulka vzniká až vyplněním předpisu (Scout: „Vyplněním tohoto pole vznikne předpis").
        // review fix: přechodně prázdné pole NEmaže existující řádky (jen skryje/nechá být)
        if (want <= 0) return;
        if (defReps === '' && tbody.querySelectorAll('.tabata-round-row').length === 0) {
            wrap.style.display = 'none';
            return;
        }
        wrap.style.display = '';
        const rows = tbody.querySelectorAll('.tabata-round-row');
        for (let i = rows.length; i < want; i++) tbody.appendChild(tabataBuildRow(i, defReps));
        for (let i = rows.length - 1; i >= want; i--) rows[i].remove();
        if (prefillAll) {
            tbody.querySelectorAll('.tabata-round-row input[type="number"]').forEach(inp => { inp.value = defReps; });
        }
        // review fix: sekvenční přečíslování (legacy řídké overrides)
        tbody.querySelectorAll('.tabata-round-row').forEach((row, i) => {
            const num = row.querySelector('.tabata-round-num');
            if (num) num.textContent = i + 1;
            const hid = row.querySelector('.tabata-round-index');
            if (hid) hid.value = i + 1;
            row.querySelectorAll('[data-name]').forEach(el => reindexDataName(el, i));
        });
        renumberExercises();
    }

    // ----- Ladder / Stepladder / Pyramid (P33) -----

    /** Vygeneruje posloupnost hodnot dle typu; null když parametry nesedí. */
    function seriesSequence(type, start, peak, step) {
        if (!peak || !start || start < 1 || peak < start || !step || step < 1) return null;
        if ((peak - start) % step !== 0) return null;
        const up = [];
        for (let v = start; v <= peak; v += step) up.push(v);
        if (type === 'LADDER') return up;
        // STEPLADDER a PYRAMID: nahoru a zase dolů (vrchol jen jednou)
        const down = up.slice(0, -1).reverse();
        return up.concat(down);
    }

    function seriesValidate(card) {
        const block = card.querySelector('.type-config.type-ladder, .type-config.type-stepladder, .type-config.type-pyramid');
        if (!block) return null;
        const warn = block.querySelector('.series-warning');
        const type = (card.querySelector('.type-select') || {}).value;
        const start = parseInt((block.querySelector('.series-start') || {}).value, 10);
        const peak = parseInt((block.querySelector('.series-peak') || {}).value, 10);
        const step = parseInt((block.querySelector('.series-step') || {}).value, 10);
        if (!peak) { if (warn) warn.style.display = 'none'; return null; }
        const seq = seriesSequence(type, start, peak, step);
        if (!seq) {
            if (warn) {
                warn.textContent = '⚠ Krok, start a vrchol nesedí — s krokem ' + (step || '?')
                    + ' se ze startu ' + (start || '?') + ' vrchol ' + peak + ' netrefí. Uprav hodnoty.';
                warn.style.display = '';
            }
            return null;
        }
        if (warn) warn.style.display = 'none';
        return { type, seq };
    }

    /** Přegeneruje tabulku série (Start/Peak/Krok změněn) — hodnoty se předvyplní. */
    function seriesRegenerate(card) {
        const block = card.querySelector('.type-config.type-ladder, .type-config.type-stepladder, .type-config.type-pyramid');
        if (!block) return;
        const tbody = block.querySelector('.series-rows-tbody');
        const wrap = block.querySelector('.series-preview');
        if (!tbody || !wrap) return;
        const valid = seriesValidate(card);
        if (!valid) { wrap.style.display = 'none'; tbody.innerHTML = ''; renumberExercises(); return; }
        const eq = equipmentWeights(card);
        // kolo 9: 2 zátěže = součet (dle Scouta pro Pyramid/Stepladder i ostatní)
        const weight = eq.sum != null ? eq.sum : '';
        tbody.innerHTML = '';
        let rowIdx = 0;
        valid.seq.forEach((value, si) => {
            // Ladder/Stepladder: série s hodnotou V má V řádků; Pyramid: 1 řádek na stupeň
            const rowsInRung = (valid.type === 'PYRAMID') ? 1 : value;
            for (let r = 0; r < rowsInRung; r++) {
                const tr = document.createElement('tr');
                tr.className = 'series-row';
                const label = document.createElement('td');
                label.className = 'series-row-label';
                label.textContent = (r === 0) ? ((si + 1) + '. série') : '';
                tr.appendChild(label);
                const repsTd = document.createElement('td');
                repsTd.appendChild(makeNumberInput('numericSeries.rows[' + rowIdx + '].reps', value, 0));
                const hidRung = document.createElement('input');
                hidRung.type = 'hidden';
                hidRung.className = 'series-row-rung';
                hidRung.setAttribute('data-name', 'numericSeries.rows[' + rowIdx + '].rung');
                hidRung.value = si + 1;
                repsTd.appendChild(hidRung);
                const hidIdx = document.createElement('input');
                hidIdx.type = 'hidden';
                hidIdx.className = 'series-row-index';
                hidIdx.setAttribute('data-name', 'numericSeries.rows[' + rowIdx + '].rowIndex');
                hidIdx.value = rowIdx;
                repsTd.appendChild(hidIdx);
                tr.appendChild(repsTd);
                const wTd = document.createElement('td');
                const wInp = makeNumberInput('numericSeries.rows[' + rowIdx + '].weightKg', weight, null, null, '0.25');
                wInp.classList.add('series-row-weight');
                wTd.appendChild(wInp);
                tr.appendChild(wTd);
                // kolo 9: řádek série lze smazat
                const rmTd = document.createElement('td');
                const rmBtn = document.createElement('button');
                rmBtn.type = 'button';
                rmBtn.className = 'btn btn-sm btn-outline-danger remove-series-row';
                rmBtn.title = 'Smazat řádek';
                rmBtn.textContent = '×';
                rmTd.appendChild(rmBtn);
                tr.appendChild(rmTd);
                tbody.appendChild(tr);
                rowIdx++;
            }
        });
        wrap.style.display = '';
        renumberExercises();
    }

    // ----- Jednotky Carry / Isometrie (P38 + kolo 9) -----

    function unitLabel(v) {
        return v === 'METERS' ? 'Metry' : v === 'SECONDS' ? 'Sekundy' : 'Opakování';
    }

    /**
     * Zjistí zaškrtnuté spouštěcí tagy v kontejneru checkboxů.
     *
     * kolo 10: rozhoduje `data-system-key` (CARRY / ISOMETRY), ne text labelu — názvy
     * systémových tagů se počešťují a dřívější porovnávání podle názvu logiku tiše rozbilo.
     */
    function triggerTags(container) {
        const res = { carry: false, iso: false };
        if (!container) return res;
        container.querySelectorAll('input[type="checkbox"]').forEach(chk => {
            if (!chk.checked) return;
            const key = chk.dataset.systemKey;
            if (key === 'CARRY') res.carry = true;
            if (key === 'ISOMETRY') res.iso = true;
        });
        return res;
    }

    /**
     * Jednotka záznamu cviku. kolo 10: Nošení = metry/sekundy, Izometrie = jen sekundy —
     * ani jeden z těchto tagů NEumožňuje záznam na opakování (dřív šlo REPS nechat).
     * Platí pro freeform sety i tabulku série.
     */
    function updateExerciseUnits(card) {
        const select = card.querySelector('.set-unit-select');
        if (!select) return;
        const tags = triggerTags(card.querySelector('.exercise-tags'));
        const carry = tags.carry, iso = tags.iso;
        const active = carry || iso;
        const wrap = card.querySelector('.set-unit-wrap');
        const seriesWrap = card.querySelector('.series-unit-wrap');
        if (wrap) wrap.style.display = active ? '' : 'none';
        if (seriesWrap) seriesWrap.style.display = active ? '' : 'none';
        // Isometrie bez Nošení → Metry nejsou k dispozici (výdrž se nepřekonává na vzdálenost)
        const allowMeters = carry;
        card.querySelectorAll('.set-unit-select option[value="METERS"], .series-unit-meters').forEach(opt => {
            opt.hidden = !allowMeters;
        });
        // kolo 10: s Nošením/Izometrií se opakování nepočítají → volba REPS zmizí
        card.querySelectorAll('.set-unit-select option[value="REPS"], .series-unit-reps').forEach(opt => {
            opt.hidden = active;
        });
        if (!active) select.value = 'REPS';
        if (active && select.value === 'REPS') select.value = carry ? 'METERS' : 'SECONDS';
        if (!allowMeters && select.value === 'METERS') select.value = 'SECONDS';
        // synchronizace viditelného série-selectu s kanonickým setUnit selectem
        const seriesSel = card.querySelector('.series-unit-select');
        if (seriesSel) seriesSel.value = select.value;
        const header = card.querySelector('.sets-reps-header');
        if (header) header.textContent = unitLabel(select.value);
        const seriesHeader = card.querySelector('.series-reps-header');
        if (seriesHeader) seriesHeader.textContent = unitLabel(select.value);
    }

    /**
     * kolo 9: jednotky + XOR na úrovni circuit kroku. Carry tag kroku → volba
     * Sekundy/Metry místo opakování a pevné pole Sekundy mizí; Isometrie → jen Sekundy.
     */
    function updateStepUnits(stepRow) {
        const sel = stepRow.querySelector('.step-unit-select');
        if (!sel) return;
        const tags = triggerTags(stepRow);
        const active = tags.carry || tags.iso;
        sel.style.display = active ? '' : 'none';
        const repsLabel = stepRow.querySelector('.step-reps-label');
        if (repsLabel) repsLabel.style.display = active ? 'none' : '';
        const metersOpt = sel.querySelector('option[value="METERS"]');
        if (metersOpt) metersOpt.hidden = !tags.carry;
        // kolo 10: Nošení/Izometrie nepřipouští záznam na opakování
        const repsOpt = sel.querySelector('option[value="REPS"]');
        if (repsOpt) repsOpt.hidden = active;
        if (!active) sel.value = 'REPS';
        if (active && sel.value === 'REPS') sel.value = tags.carry ? 'METERS' : 'SECONDS';
        if (!tags.carry && sel.value === 'METERS') sel.value = 'SECONDS';
        // pevné pole Sekundy mizí, když je aktivní volba jednotky
        const secondsCol = stepRow.querySelector('.step-seconds-col');
        const secondsInput = stepRow.querySelector('.step-seconds-input');
        if (secondsCol) secondsCol.style.display = active ? 'none' : '';
        if (active && secondsInput) secondsInput.value = '';
        updateStepXor(stepRow);
    }

    /** kolo 9: u kroku lze vyplnit jen Opakování NEBO Sekundy (vzájemně se blokují). */
    function updateStepXor(stepRow) {
        const reps = stepRow.querySelector('.step-reps-input');
        const secs = stepRow.querySelector('.step-seconds-input');
        if (!reps || !secs) return;
        const secondsVisible = !stepRow.querySelector('.step-seconds-col')
                || stepRow.querySelector('.step-seconds-col').style.display !== 'none';
        if (!secondsVisible) { reps.disabled = false; secs.disabled = false; return; }
        // legacy data s oběma hodnotami nesmí zablokovat obě pole (disabled se neodešle)
        if (reps.value !== '' && secs.value !== '') { reps.disabled = false; secs.disabled = false; return; }
        secs.disabled = reps.value !== '';
        reps.disabled = secs.value !== '';
    }

    // ----- Prefill náčiní z katalogu (P37) -----

    function prefillEquipmentFromCatalog(card, catalogId) {
        const eqInput = card.querySelector('[data-name="equipmentName"]');
        if (!eqInput) return;
        // Uživatelova ruční volba je nadřazená — přepisujeme jen prázdné/autofillnuté pole
        if (eqInput.value && eqInput.dataset.autofilled !== '1') return;
        const items = window.__catalogItems || [];
        const ci = items.find(c => String(c.id) === String(catalogId));
        if (!ci || !ci.equipment) return;
        const label = EQUIPMENT_LABELS[ci.equipment] !== undefined
                ? EQUIPMENT_LABELS[ci.equipment] : ci.equipment.toLowerCase();
        if (!label) return; // OTHER apod. — nemá smysluplný český název
        eqInput.value = label;
        eqInput.dataset.autofilled = '1';
        updateKbState(card);
    }

    // =========================================================================
    // Event delegation
    // =========================================================================

    container.addEventListener('click', function (e) {
        const target = e.target;
        if (target.classList.contains('remove-exercise')) {
            const card = target.closest('.exercise-card');
            if (card && container.querySelectorAll('.exercise-card').length > 1) {
                card.remove();
                renumberExercises();
            } else {
                alert('Musí být aspoň jedna část. Pokud nechceš žádnou, smaž celý trénink.');
            }
        } else if (target.classList.contains('add-set')) {
            const card = target.closest('.exercise-card');
            const tbody = card.querySelector('.sets-tbody');
            tbody.appendChild(setRowTemplate.content.firstElementChild.cloneNode(true));
            renumberExercises();
        } else if (target.classList.contains('remove-set')) {
            const row = target.closest('.set-row');
            const tbody = row.parentElement;
            row.remove();
            if (tbody.querySelectorAll('.set-row').length === 0) {
                tbody.appendChild(setRowTemplate.content.firstElementChild.cloneNode(true));
            }
            renumberExercises();
        } else if (target.classList.contains('add-amrap-step')) {
            const card = target.closest('.exercise-card');
            addAmrapStepRow(card.querySelector('.amrap-steps-tbody'));
            renumberExercises();
            amrapRoundsSync(card);
        } else if (target.classList.contains('add-circuit-step')) {
            const card = target.closest('.exercise-card');
            addCircuitStepRow(card.querySelector('.circuit-steps-tbody'));
            renumberExercises();
        } else if (target.classList.contains('remove-step')) {
            const card = target.closest('.exercise-card');
            const row = target.closest('.circuit-step-row, .amrap-step-row');
            const wasAmrap = row && row.classList.contains('amrap-step-row');
            if (row) row.remove();
            renumberExercises();
            if (wasAmrap && card) amrapRoundsSync(card);
        } else if (target.classList.contains('remove-series-row')) {
            // kolo 9: smazání řádku série (např. nedokončená pyramida)
            const card = target.closest('.exercise-card');
            const row = target.closest('.series-row');
            if (row) row.remove();
            if (card) seriesReindex(card);
        }
    });

    addExerciseBtn.addEventListener('click', addExercise);

    container.addEventListener('change', function (e) {
        const t = e.target;
        const card = t.closest('.exercise-card');
        if (!card) return;
        if (t.classList.contains('catalog-select') && t.value) {
            const customInput = card.querySelector('.custom-name-input');
            if (customInput) customInput.value = '';
            // kolo 8 (P37): náčiní z katalogu se propíše do pole Náčiní
            prefillEquipmentFromCatalog(card, t.value);
        } else if (t.classList.contains('type-select')) {
            updateTypeConfigVisibility(card);
            // review fix: přepnutí Ladder↔Stepladder↔Pyramid má jinou sémantiku tabulky →
            // přegenerovat (jinak by se uložily řádky podle staré logiky)
            if (['LADDER', 'STEPLADDER', 'PYRAMID'].includes(t.value)) {
                seriesRegenerate(card);
            }
        } else if (t.classList.contains('equipment-count')) {
            updateEquipmentSecondVisibility(card);
            updateKbState(card);
            // kolo 9: změna počtu zátěží se okamžitě propíše do všech tabulek
            emomApplyKg(card);
            seriesApplyWeight(card);
        } else if (t.classList.contains('step-equipment-count')) {
            // kolo 8: druhá zátěž per circuit krok
            const stepRow = t.closest('.circuit-step-row');
            const second = stepRow && stepRow.querySelector('.step-equipment-second');
            if (second) second.style.display = (t.value === '2') ? '' : 'none';
        } else if (t.classList.contains('step-catalog-select') && t.value) {
            const wrap = t.parentElement;
            const nameInput = wrap && wrap.querySelector('.step-name-input');
            if (nameInput) nameInput.value = t.value;
            // kolo 10: tabulka kol AMRAPu ukazuje názvy cviků → přegenerovat
            if (t.closest('.amrap-step-row')) amrapRoundsSync(card);
        } else if ((t.getAttribute('data-step-field') === 'name'
                    || t.getAttribute('data-step-field') === 'reps')
                && t.closest('.amrap-step-row')) {
            amrapRoundsSync(card);
        } else if (t.classList.contains('kb-detail-unilateral') || t.classList.contains('kb-detail-bilateral')) {
            // vzájemně výlučné checkboxy
            const block = card.querySelector('.type-config.type-kb_sport_time');
            if (t.checked && block) {
                const other = t.classList.contains('kb-detail-unilateral')
                        ? block.querySelector('.kb-detail-bilateral')
                        : block.querySelector('.kb-detail-unilateral');
                if (other) other.checked = false;
            }
            updateKbState(card);
        } else if (t.classList.contains('emom-edit-sets')) {
            emomSync(card, t.checked); // při zapnutí předvyplnit
        } else if (t.classList.contains('set-unit-select')) {
            updateExerciseUnits(card);
        } else if (t.classList.contains('series-unit-select')) {
            // viditelný select u série je jen zrcadlo kanonického setUnit selectu
            const canonical = card.querySelector('.set-unit-select');
            if (canonical) canonical.value = t.value;
            updateExerciseUnits(card);
        } else if (t.getAttribute('data-step-field') === 'tagIds') {
            // kolo 9/10: Nošení/Izometrie na kroku kruhového tréninku i AMRAPu → jednotky
            const stepRow = t.closest('.circuit-step-row, .amrap-step-row');
            if (stepRow) updateStepUnits(stepRow);
        } else if (t.closest('.exercise-tags')) {
            updateExerciseUnits(card);
        } else if (t.classList.contains('kb-split-parts')) {
            // review fix: resize tabulek až na change (blur/šipky) — resize na každý
            // stisk klávesy mazal vyplněné řádky během přepisování čísla
            kbResizeParts(card);
        } else if (t.classList.contains('emom-total-minutes')) {
            emomSync(card, false);
        } else if (t.classList.contains('emom-default-reps')) {
            // kolo 9: přednastavený počet opakování okamžitě přepíše všechny řádky
            emomSync(card, false);
            emomApplyReps(card);
        } else if (t.classList.contains('ss-set-count')) {
            straightSetsSync(card, false);
        } else if (t.classList.contains('ss-reps') || t.classList.contains('ss-weight')
                || t.classList.contains('ss-rest-min') || t.classList.contains('ss-rest-sec')) {
            // změna předpisu přepíše celou tabulku (uživatel ji pak může upravit)
            straightSetsSync(card, true);
        } else if (t.classList.contains('amrap-rounds')) {
            amrapRoundsSync(card);
        } else if (t.classList.contains('circuit-mode')) {
            // kolo 10: přepnutí CIRCUIT/SUPERSET/COMPLEX
            updateCircuitMode(card);
        } else if (t.getAttribute('data-name') === 'circuit.rounds'
                || t.classList.contains('circuit-rest-min') || t.classList.contains('circuit-rest-sec')) {
            circuitRoundRestsSync(card);
        } else if (t.getAttribute('data-step-field') === 'equipmentName'
                || t.getAttribute('data-step-field') === 'equipmentWeightKg'
                || t.getAttribute('data-step-field') === 'equipmentCount'
                || t.getAttribute('data-step-field') === 'equipmentSecondWeightKg') {
            // COMPLEX sdílí náčiní prvního cviku se všemi ostatními
            const block = card.querySelector('.type-config.type-circuit');
            if (block && circuitMode(card) === 'COMPLEX') applyComplexSharedEquipment(block);
        } else if (t.classList.contains('tabata-rounds')) {
            tabataSync(card, false);
        } else if (t.classList.contains('tabata-default-reps')) {
            tabataSync(card, true); // předpis: přepíše všechna kola
        } else if (t.classList.contains('series-start') || t.classList.contains('series-peak')
                || t.classList.contains('series-step')) {
            seriesRegenerate(card);
        } else if (t.getAttribute('data-name') === 'equipmentWeightKg'
                || t.getAttribute('data-name') === 'equipmentSecondWeightKg') {
            // kolo 9/10: změna váhy náčiní se okamžitě propíše do všech tabulek
            emomApplyKg(card);
            seriesApplyWeight(card);
            straightSetsApplyWeight(card);
            intervalApplyWeight(card);
        }
    });

    function updateEquipmentSecondVisibility(card) {
        const countSel = card.querySelector('.equipment-count');
        const second = card.querySelector('.equipment-second');
        if (countSel && second) {
            second.style.display = (countSel.value === '2') ? '' : 'none';
        }
    }

    container.addEventListener('input', function (e) {
        const t = e.target;
        const card = t.closest('.exercise-card');
        if (!card) return;
        if (t.classList.contains('custom-name-input') && t.value.trim()) {
            const catalogSelect = card.querySelector('.catalog-select');
            if (catalogSelect) catalogSelect.value = '';
        } else if (t.getAttribute('data-name') === 'equipmentName') {
            // ruční zápis náčiní = nadřazený katalogu
            t.dataset.autofilled = '';
            updateKbState(card);
        } else if (t.getAttribute('data-name') === 'equipmentWeightKg'
                || t.getAttribute('data-name') === 'equipmentSecondWeightKg') {
            updateKbState(card);
            // kolo 9/10: „bezprostředně" — už během psaní váhy se tabulky aktualizují
            emomApplyKg(card);
            seriesApplyWeight(card);
            straightSetsApplyWeight(card);
            intervalApplyWeight(card);
        } else if (t.classList.contains('step-reps-input') || t.classList.contains('step-seconds-input')) {
            // kolo 9: Opakování XOR Sekundy u kroku circuitu
            const stepRow = t.closest('.circuit-step-row');
            if (stepRow) updateStepXor(stepRow);
        } else if (t.classList.contains('emom-default-reps')) {
            // kolo 9: „okamžitě" — přepis všech řádků už během psaní (nedestruktivní)
            emomApplyReps(card);
        } else if (t.classList.contains('tabata-default-reps')) {
            const tblock = card.querySelector('.type-config.type-tabata');
            if (tblock) tblock.querySelectorAll('.tabata-round-row input[type="number"]')
                    .forEach(inp => { inp.value = t.value; });
        } else if (t.classList.contains('kb-total-min') || t.classList.contains('kb-total-sec')
                || t.classList.contains('kb-total-reps')
                || t.classList.contains('kb-part-reps')
                || t.classList.contains('kb-part-dur-min') || t.classList.contains('kb-part-dur-sec')) {
            // živá (nevynucující) kontrola součtů — nic nemaže, může běžet na každý stisk
            kbValidate(card);
        } else if (t.classList.contains('series-start') || t.classList.contains('series-peak')
                || t.classList.contains('series-step')) {
            // živá validace parametrů; přegenerování tabulky až na change (viz výše)
            seriesValidate(card);
        }
        // review fix: destruktivní resize tabulek (KB části / EMOM minuty / Tabata kola /
        // série) se spouští až na 'change', ne na každý stisk klávesy
    });

    // ----- kolo 10: synchronizace tagů „obecné informace o tréninku" ↔ „náplň tréninku" -----

    // Kontejner tagů tréninku existuje jen v klientském formuláři deníku; admin šablony
    // sdílejí jen editor cviků, takže se celá sekce chová jako no-op.
    const trainingTagsBox = document.querySelector('.training-tags');
    const diaryForm = trainingTagsBox ? trainingTagsBox.closest('form') : null;

    /** Id tagů skutečně použitých u konkrétních cviků (včetně kroků kruhového tréninku). */
    function usedExerciseTagIds() {
        const ids = new Set();
        container.querySelectorAll('.exercise-tags input[type="checkbox"]:checked,'
                + ' input[data-step-field="tagIds"]:checked').forEach(chk => ids.add(chk.value));
        return ids;
    }

    /** Tagy tréninku, které nemá žádný cvik → červené zvýraznění (uložit ale jde). */
    function highlightUnusedTrainingTags(used) {
        if (!trainingTagsBox) return;
        const usedIds = used || usedExerciseTagIds();
        trainingTagsBox.querySelectorAll('.form-check-inline').forEach(wrap => {
            const chk = wrap.querySelector('input[type="checkbox"]');
            if (!chk) return;
            wrap.classList.toggle('tag-unused', chk.checked && !usedIds.has(chk.value));
        });
    }

    /** Tag zaškrtnutý u cviku se automaticky zatrhne i v obecných informacích o tréninku. */
    function syncTrainingTags() {
        if (!trainingTagsBox) return;
        const used = usedExerciseTagIds();
        used.forEach(id => {
            const box = trainingTagsBox.querySelector('input[type="checkbox"][value="' + id + '"]');
            if (box && !box.checked) box.checked = true;
        });
        highlightUnusedTrainingTags(used);
    }

    /** Názvy zvýrazněných (nepoužitých) tagů pro upozornění před uložením. */
    function unusedTrainingTagNames() {
        if (!trainingTagsBox) return [];
        const used = usedExerciseTagIds();
        const names = [];
        trainingTagsBox.querySelectorAll('.form-check-inline').forEach(wrap => {
            const chk = wrap.querySelector('input[type="checkbox"]');
            if (!chk || !chk.checked || used.has(chk.value)) return;
            const label = wrap.querySelector('label');
            names.push(label ? label.textContent.trim() : chk.value);
        });
        return names;
    }

    if (trainingTagsBox) {
        trainingTagsBox.addEventListener('change', () => highlightUnusedTrainingTags());
        container.addEventListener('change', function (e) {
            const t = e.target;
            if (t.getAttribute('data-step-field') === 'tagIds' || t.closest('.exercise-tags')) {
                syncTrainingTags();
            }
        });
        // Upozornění je nevynucující: uživatel může uložit i s nepoužitými tagy.
        if (diaryForm) {
            diaryForm.addEventListener('submit', function (e) {
                const unused = unusedTrainingTagNames();
                if (!unused.length) return;
                const ok = window.confirm(
                        'Tyhle tagy máš v obecných informacích o tréninku, ale nepoužil(a) jsi je '
                        + 'u žádného konkrétního cviku:\n\n' + unused.join(', ')
                        + '\n\nOK = uložit tak, jak to je.\nZrušit = vrátit se k úpravě tréninku.');
                if (!ok) e.preventDefault();
            });
        }
        highlightUnusedTrainingTags();
    }

    // Při načtení existujících (server-rendered) cviků nastavíme viditelnost
    // a naplníme dropdowny katalogu v krocích.
    container.querySelectorAll('.step-catalog-select').forEach(populateStepCatalog);
    container.querySelectorAll('.exercise-card').forEach(card => {
        updateTypeConfigVisibility(card);
        updateEquipmentSecondVisibility(card);
        updateExerciseUnits(card);
        // kolo 9: jednotky + XOR u circuit kroků (server-rendered)
        card.querySelectorAll('.circuit-step-row').forEach(updateStepUnits);
        card.querySelectorAll('.amrap-step-row').forEach(updateStepUnits);
        // kolo 10: režim kruhového tréninku + tabulka pauz po kolech
        updateCircuitMode(card);
        circuitRoundRestsSync(card);
        // kolo 8: druhá zátěž u circuit kroků (server-rendered)
        card.querySelectorAll('.circuit-step-row').forEach(stepRow => {
            const sel = stepRow.querySelector('.step-equipment-count');
            const second = stepRow.querySelector('.step-equipment-second');
            if (sel && second) second.style.display = (sel.value === '2') ? '' : 'none';
        });
        kbValidate(card);
        amrapRoundsSync(card);
        straightSetsSync(card, false);
    });
    // Phase 12: server-rendered kroky kruhového tréninku mají jen data-step-field,
    // jméno pole doplníme až tady → nutné zavolat renumber na load.
    renumberExercises();
})();
