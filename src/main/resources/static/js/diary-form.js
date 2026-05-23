/**
 * Dynamický editor cviků a setů ve formuláři tréninku.
 *
 * Strategie: každý cvik je <div class="exercise-card"> s data-exercise-index.
 * Při přidání nového cviku z <template id="exerciseTemplate"> a setu z <template id="setRowTemplate">
 * klonujeme template, doplňujeme name="exercises[N].field" / "exercises[N].sets[M].field" a vkládáme
 * do containeru. Při mazání přečíslujeme zbytek (Spring řadí podle indexu v form-binding).
 *
 * Pozn.: katalog se klonuje z <option>s předaných ze server-side (window.__catalogItems),
 * aby JS nemusel volat /api/exercise-catalog při každém přidání cviku.
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

    function renumberExercises() {
        const cards = container.querySelectorAll('.exercise-card');
        cards.forEach((card, idx) => {
            card.setAttribute('data-exercise-index', idx);
            card.querySelector('.exercise-number').textContent = (idx + 1) + '.';

            // Hidden type field
            const typeField = card.querySelector('[data-name="type"]') || card.querySelector('input[type="hidden"]');
            if (typeField) typeField.name = 'exercises[' + idx + '].type';

            // Other named inputs
            ['catalogItemId', 'customName', 'rpe', 'notes'].forEach(fieldName => {
                const el = card.querySelector('[data-name="' + fieldName + '"], [name$=".' + fieldName + '"]:not([name*=".sets"])');
                if (el) el.name = 'exercises[' + idx + '].' + fieldName;
            });

            // Sets
            const setRows = card.querySelectorAll('.set-row');
            setRows.forEach((row, sIdx) => {
                row.querySelector('.set-number').textContent = (sIdx + 1);
                ['weightKg', 'reps', 'rpe', 'note'].forEach(fieldName => {
                    const el = row.querySelector('[data-name="' + fieldName + '"], [name$=".' + fieldName + '"]');
                    if (el) el.name = 'exercises[' + idx + '].sets[' + sIdx + '].' + fieldName;
                });
            });
        });
    }

    function addSetRow(exerciseCard) {
        const tbody = exerciseCard.querySelector('.sets-tbody');
        const newRow = setRowTemplate.content.firstElementChild.cloneNode(true);
        tbody.appendChild(newRow);
        renumberExercises();
    }

    function addExercise() {
        const newCard = exerciseTemplate.content.firstElementChild.cloneNode(true);
        populateCatalogSelect(newCard.querySelector('.catalog-select'));
        container.appendChild(newCard);

        // Přidej 3 prázdné sety automaticky
        for (let i = 0; i < 3; i++) {
            const newRow = setRowTemplate.content.firstElementChild.cloneNode(true);
            newCard.querySelector('.sets-tbody').appendChild(newRow);
        }

        renumberExercises();
    }

    // Event delegation pro tlačítka uvnitř container
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
            addSetRow(card);
        } else if (target.classList.contains('remove-set')) {
            const row = target.closest('.set-row');
            const tbody = row.parentElement;
            row.remove();
            // Necháme alespoň jeden řádek
            if (tbody.querySelectorAll('.set-row').length === 0) {
                const newRow = setRowTemplate.content.firstElementChild.cloneNode(true);
                tbody.appendChild(newRow);
            }
            renumberExercises();
        }
    });

    addExerciseBtn.addEventListener('click', addExercise);

    // XOR validace: pokud uživatel vybere z katalogu, vymaž custom name (a naopak)
    container.addEventListener('change', function (e) {
        if (e.target.classList.contains('catalog-select') && e.target.value) {
            const customInput = e.target.closest('.exercise-card').querySelector('.custom-name-input');
            if (customInput) customInput.value = '';
        }
    });

    container.addEventListener('input', function (e) {
        if (e.target.classList.contains('custom-name-input') && e.target.value.trim()) {
            const catalogSelect = e.target.closest('.exercise-card').querySelector('.catalog-select');
            if (catalogSelect) catalogSelect.value = '';
        }
    });
})();
