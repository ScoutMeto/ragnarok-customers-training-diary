/**
 * Dynamický editor cviků a setů ve formuláři tréninku.
 *
 * Strategie: každý cvik je <div class="exercise-card"> s data-exercise-index.
 * Při přidání nového cviku z <template id="exerciseTemplate"> a setu z <template id="setRowTemplate">
 * klonujeme template, doplňujeme name="exercises[N].field" / "exercises[N].sets[M].field" a vkládáme
 * do containeru. Při mazání přečíslujeme zbytek (Spring řadí podle indexu v form-binding).
 *
 * Per-type config sekce (.type-config.type-XYZ) se zobrazuje/skrývá podle vybraného typu cviku.
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
    const GROUPED_TYPES = { SUPERSET: 'Superset', COMPLEX: 'Complex', CIRCUIT: 'Circuit' };

    function populateCatalogSelect(selectEl) {
        const items = window.__catalogItems || [];
        items.forEach(ci => {
            const opt = document.createElement('option');
            opt.value = ci.id;
            opt.textContent = ci.name + ' (' + (ci.equipment || '') + ')';
            selectEl.appendChild(opt);
        });
    }

    // Phase 12 (A6/A7): naplní dropdown katalogu v rámci kroku (composite/circuit).
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

    function addCompositeStepRow(tbody) {
        const tpl = document.getElementById('compositeStepTemplate');
        const row = tpl.content.firstElementChild.cloneNode(true);
        populateStepCatalog(row.querySelector('.step-catalog-select'));
        tbody.appendChild(row);
        return row;
    }

    function addCircuitStepRow(tbody) {
        const tpl = document.getElementById('circuitStepTemplate');
        const row = tpl.content.firstElementChild.cloneNode(true);
        populateStepCatalog(row.querySelector('.step-catalog-select'));
        tbody.appendChild(row);
        return row;
    }

    function populateTypeSelect(selectEl, currentValue) {
        const types = window.__exerciseTypes || ['FREEFORM'];
        types.forEach(t => {
            const opt = document.createElement('option');
            opt.value = t;
            opt.textContent = t;
            if (t === currentValue) opt.selected = true;
            selectEl.appendChild(opt);
        });
    }

    function updateTypeConfigVisibility(exerciseCard) {
        const typeSelect = exerciseCard.querySelector('.type-select');
        if (!typeSelect) return;
        const selected = typeSelect.value;
        exerciseCard.querySelectorAll('.type-config').forEach(div => {
            // div has class type-emom / type-tabata / type-amrap / ...
            const isMatch = div.classList.contains('type-' + selected.toLowerCase());
            div.style.display = isMatch ? '' : 'none';
        });
        // Phase 12 (A6): tabulka sérií (Váha/reps/RPE/pozn.) jen pro FREEFORM —
        // pro typované cviky ji nahrazuje per-type config.
        const setsBlock = exerciseCard.querySelector('.freeform-sets');
        if (setsBlock) {
            setsBlock.style.display = (selected === 'FREEFORM') ? '' : 'none';
        }

        // Phase 12 (A8): u sdružených typů (superset/complex/circuit) skryjeme horní
        // pojmenování cviku (kroky se pojmenovávají samy) a doplníme zástupný název.
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
            // zajisti aspoň 2 prázdné kroky pro pohodlí
            ensureMinSteps(exerciseCard, selected);
        }
    }

    // Phase 12: pro sdružené typy doplní minimální počet prázdných kroků, pokud žádné nejsou.
    function ensureMinSteps(card, selected) {
        if (selected === 'CIRCUIT') {
            const tbody = card.querySelector('.circuit-steps-tbody');
            if (tbody && tbody.querySelectorAll('.circuit-step-row').length === 0) {
                for (let i = 0; i < 2; i++) addCircuitStepRow(tbody);
                renumberExercises();
            }
        } else { // SUPERSET / COMPLEX
            const tbody = card.querySelector('.composite-steps-tbody');
            if (tbody && tbody.querySelectorAll('.composite-step-row').length === 0) {
                for (let i = 0; i < 2; i++) addCompositeStepRow(tbody);
                renumberExercises();
            }
        }
    }

    function renumberExercises() {
        const cards = container.querySelectorAll('.exercise-card');
        cards.forEach((card, idx) => {
            card.setAttribute('data-exercise-index', idx);
            const numEl = card.querySelector('.exercise-number');
            if (numEl) numEl.textContent = (idx + 1) + '.';

            // Top-level fields (type, catalogItemId, customName, rpe, notes, equipment...)
            ['type', 'catalogItemId', 'customName', 'rpe', 'notes',
             'equipmentName', 'equipmentWeightKg', 'equipmentCount', 'equipmentSecondWeightKg'].forEach(fieldName => {
                const el = card.querySelector('[data-name="' + fieldName + '"]');
                if (el) el.name = 'exercises[' + idx + '].' + fieldName;
            });

            // Phase 11 (A2): per-exercise tag checkboxy (víc elementů se stejným name)
            card.querySelectorAll('[data-name="tagIds"]').forEach(el => {
                el.name = 'exercises[' + idx + '].tagIds';
            });

            // Per-type config fields (data-name="emom.totalMinutes", "tabata.rounds", ...)
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
                ['weightKg', 'reps', 'rpe', 'note'].forEach(fieldName => {
                    const el = row.querySelector('[data-name="' + fieldName + '"]');
                    if (el) el.name = 'exercises[' + idx + '].sets[' + sIdx + '].' + fieldName;
                });
            });

            // Phase 12: composite (superset/complex) kroky
            card.querySelectorAll('.composite-step-row').forEach((row, sIdx) => {
                const numTd = row.querySelector('.step-num');
                if (numTd) numTd.textContent = (sIdx + 1);
                row.querySelectorAll('[data-step-field]').forEach(el => {
                    el.name = 'exercises[' + idx + '].composite.steps[' + sIdx + '].' + el.getAttribute('data-step-field');
                });
            });

            // Phase 12: circuit kroky
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

        // 3 prázdné sety pro pohodlí
        for (let i = 0; i < 3; i++) {
            const newRow = setRowTemplate.content.firstElementChild.cloneNode(true);
            newCard.querySelector('.sets-tbody').appendChild(newRow);
        }

        updateTypeConfigVisibility(newCard);
        renumberExercises();
    }

    // Event delegation
    container.addEventListener('click', function (e) {
        const target = e.target;
        if (target.classList.contains('remove-exercise')) {
            const card = target.closest('.exercise-card');
            if (card && container.querySelectorAll('.exercise-card').length > 1) {
                card.remove();
                renumberExercises();
            } else {
                alert('Musí být aspoň jeden cvik. Pokud nechceš žádný, smaž celý trénink.');
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
        } else if (target.classList.contains('add-composite-step')) {
            const card = target.closest('.exercise-card');
            addCompositeStepRow(card.querySelector('.composite-steps-tbody'));
            renumberExercises();
        } else if (target.classList.contains('add-circuit-step')) {
            const card = target.closest('.exercise-card');
            addCircuitStepRow(card.querySelector('.circuit-steps-tbody'));
            renumberExercises();
        } else if (target.classList.contains('remove-step')) {
            const row = target.closest('tr');
            row.remove();
            renumberExercises();
        }
    });

    addExerciseBtn.addEventListener('click', addExercise);

    // XOR + type-switch handling
    container.addEventListener('change', function (e) {
        if (e.target.classList.contains('catalog-select') && e.target.value) {
            const customInput = e.target.closest('.exercise-card').querySelector('.custom-name-input');
            if (customInput) customInput.value = '';
        } else if (e.target.classList.contains('type-select')) {
            updateTypeConfigVisibility(e.target.closest('.exercise-card'));
        } else if (e.target.classList.contains('equipment-count')) {
            updateEquipmentSecondVisibility(e.target.closest('.exercise-card'));
        } else if (e.target.classList.contains('step-catalog-select') && e.target.value) {
            // Phase 12: výběr z katalogu vyplní textový název kroku.
            const nameInput = e.target.closest('td').querySelector('.step-name-input');
            if (nameInput) nameInput.value = e.target.value;
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
        if (e.target.classList.contains('custom-name-input') && e.target.value.trim()) {
            const catalogSelect = e.target.closest('.exercise-card').querySelector('.catalog-select');
            if (catalogSelect) catalogSelect.value = '';
        }
    });

    // Při načtení existujících (server-rendered) cviků nastavíme viditelnost
    // a naplníme dropdowny katalogu v krocích.
    container.querySelectorAll('.step-catalog-select').forEach(populateStepCatalog);
    container.querySelectorAll('.exercise-card').forEach(card => {
        updateTypeConfigVisibility(card);
        updateEquipmentSecondVisibility(card);
    });
    // Phase 12: server-rendered kroky (composite/circuit) mají jen data-step-field,
    // jméno pole doplníme až tady → nutné zavolat renumber na load.
    renumberExercises();
})();
