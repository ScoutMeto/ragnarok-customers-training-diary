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

    function populateCatalogSelect(selectEl) {
        const items = window.__catalogItems || [];
        items.forEach(ci => {
            const opt = document.createElement('option');
            opt.value = ci.id;
            opt.textContent = ci.name + ' (' + (ci.equipment || '') + ')';
            selectEl.appendChild(opt);
        });
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
    }

    function renumberExercises() {
        const cards = container.querySelectorAll('.exercise-card');
        cards.forEach((card, idx) => {
            card.setAttribute('data-exercise-index', idx);
            const numEl = card.querySelector('.exercise-number');
            if (numEl) numEl.textContent = (idx + 1) + '.';

            // Top-level fields (type, catalogItemId, customName, rpe, notes)
            ['type', 'catalogItemId', 'customName', 'rpe', 'notes'].forEach(fieldName => {
                const el = card.querySelector('[data-name="' + fieldName + '"]');
                if (el) el.name = 'exercises[' + idx + '].' + fieldName;
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
        }
    });

    container.addEventListener('input', function (e) {
        if (e.target.classList.contains('custom-name-input') && e.target.value.trim()) {
            const catalogSelect = e.target.closest('.exercise-card').querySelector('.catalog-select');
            if (catalogSelect) catalogSelect.value = '';
        }
    });

    // Při načtení existujících (server-rendered) cviků nastavíme viditelnost
    container.querySelectorAll('.exercise-card').forEach(updateTypeConfigVisibility);
})();
