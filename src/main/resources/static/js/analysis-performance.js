/**
 * Výkonnostní analytika (ScoutMeto kolo 10).
 *
 * Doplňuje analysis.js o sekce, které stojí na novém endpointu
 * /api/analysis/performance/*: souhrn cviku podle charakteru provedení, tabulku
 * tréninků s rozpadem na cviky, pět výkonnostních oblastí, rozpady do koláčů a
 * sloupců, filtraci podle tagů a vývoj sledovaných proměnných.
 *
 * Období se bere ze stejného filtru nahoře na stránce jako zbytek statistik.
 */
(function () {
    'use strict';

    if (!document.getElementById('trainingRows')) return;

    const charts = {};
    const COLORS = ['#9F371B', '#1976D2', '#2E7D32', '#F57C00', '#7B1FA2',
                    '#00838F', '#5D4037', '#455A64', '#C2185B', '#689F38'];

    function isoDaysAgo(n) {
        const d = new Date();
        d.setDate(d.getDate() - n);
        return d.toISOString().slice(0, 10);
    }
    function isoToday() { return new Date().toISOString().slice(0, 10); }

    function period() {
        const sel = document.getElementById('periodSelect');
        const val = sel ? sel.value : '30';
        if (val === 'custom') {
            const f = document.getElementById('fromDate');
            const t = document.getElementById('toDate');
            return { from: (f && f.value) || isoDaysAgo(30), to: (t && t.value) || isoToday() };
        }
        return { from: isoDaysAgo(parseInt(val, 10)), to: isoToday() };
    }

    function api(path, extra) {
        const p = period();
        return fetch('/api/analysis/performance/' + path + '?from=' + p.from + '&to=' + p.to
                     + (extra || ''), { credentials: 'same-origin' })
            .then(r => r.ok ? r.json() : Promise.reject(new Error(r.status)));
    }

    const nf = new Intl.NumberFormat('cs-CZ', { maximumFractionDigits: 1 });
    function fmt(v) { return v == null ? '—' : nf.format(parseFloat(v)); }
    function fmtTime(sec) {
        if (!sec) return '0:00';
        const h = Math.floor(sec / 3600);
        const m = Math.floor((sec % 3600) / 60);
        const s = Math.round(sec % 60);
        const pad = n => (n < 10 ? '0' + n : '' + n);
        return (h > 0 ? h + ':' + pad(m) : m) + ':' + pad(s);
    }

    function destroy(id) {
        if (charts[id]) { charts[id].destroy(); delete charts[id]; }
    }

    /** Buňka s textem — nikdy nevkládáme uživatelský text jako HTML. */
    function td(text, className) {
        const cell = document.createElement('td');
        cell.textContent = text == null ? '—' : text;
        if (className) cell.className = className;
        return cell;
    }

    // =====================================================================
    // P58: souhrn pro vybraný cvik podle charakteru provedení
    // =====================================================================

    const CHARACTER_LABEL = {
        REPETITIVE: 'Repetitivní provedení',
        CARRY: 'Nošení',
        ISOMETRY: 'Izometrie'
    };

    function renderExerciseSummary(s) {
        const box = document.getElementById('exStatResult');
        const empty = document.getElementById('exStatEmpty');
        const rows = document.getElementById('exSummaryRows');
        if (!box || !rows) return;

        const hasData = s && (s.sets > 0 || s.reps > 0 || s.meters > 0 || s.seconds > 0);
        if (!hasData) {
            box.style.display = 'none';
            if (empty) empty.style.display = '';
            return;
        }
        document.getElementById('exCharacter').textContent = CHARACTER_LABEL[s.character] || '—';
        rows.innerHTML = '';

        const add = (label, value) => {
            const tr = document.createElement('tr');
            tr.appendChild(td(label));
            tr.appendChild(td(value, 'num'));
            rows.appendChild(tr);
        };

        if (s.character === 'CARRY') {
            if (s.meters > 0) add('Počet metrů', fmt(s.meters) + ' m');
            if (s.seconds > 0) add('Čas pod zátěží', fmtTime(s.seconds));
            add('Nejnižší použitá váha', s.minWeightKg != null ? fmt(s.minWeightKg) + ' kg' : '—');
            add('Nejvyšší použitá váha', s.maxWeightKg != null ? fmt(s.maxWeightKg) + ' kg' : '—');
        } else if (s.character === 'ISOMETRY') {
            add('Čas výdrží', fmtTime(s.seconds));
            add('Počet sérií', fmt(s.sets));
        } else {
            add('Celkový počet sérií', fmt(s.sets));
            add('Celkový počet opakování', fmt(s.reps));
            if (!s.bodyweight) {
                add('Celkem nazvedáno', fmt(s.liftedKg) + ' kg');
                if (s.liftedPerBodyweight != null) {
                    add('Výkonnost (kg / tělesná váha)', fmt(s.liftedPerBodyweight));
                }
            }
        }
        if (empty) empty.style.display = 'none';
        box.style.display = '';
    }

    // =====================================================================
    // P59: tabulka tréninků s rozpadem na cviky
    // =====================================================================

    function summaryCells(tr, sum) {
        tr.appendChild(td(fmt(sum.sets), 'num'));
        tr.appendChild(td(fmt(sum.reps), 'num'));
        tr.appendChild(td(sum.liftedKg && parseFloat(sum.liftedKg) > 0 ? fmt(sum.liftedKg) : '—', 'num'));
        tr.appendChild(td(sum.liftedPerBodyweight != null ? fmt(sum.liftedPerBodyweight) : '—', 'num'));
    }

    function renderTrainings(list) {
        const tbody = document.getElementById('trainingRows');
        const empty = document.getElementById('trainingRowsEmpty');
        tbody.innerHTML = '';
        if (!list.length) { empty.style.display = ''; return; }
        empty.style.display = 'none';

        list.forEach(t => {
            const tr = document.createElement('tr');
            const toggle = document.createElement('td');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-sm btn-outline';
            btn.textContent = '+';
            toggle.appendChild(btn);
            tr.appendChild(toggle);
            tr.appendChild(td(t.date));

            const nameCell = document.createElement('td');
            const link = document.createElement('a');
            link.href = '/diary/' + t.trainingId;
            link.textContent = t.name || 'Trénink';
            nameCell.appendChild(link);
            tr.appendChild(nameCell);

            tr.appendChild(td(t.difficultyLabel));
            summaryCells(tr, t.total);
            tbody.appendChild(tr);

            // podřádky s jednotlivými cviky — skryté, dokud se řádek nerozklikne
            const detail = [];
            t.exercises.forEach(ex => {
                const dtr = document.createElement('tr');
                dtr.style.display = 'none';
                dtr.className = 'training-exercise-row';
                dtr.appendChild(td(''));
                dtr.appendChild(td(''));
                dtr.appendChild(td(ex.name));
                dtr.appendChild(td(CHARACTER_LABEL[ex.character] || '—'));
                if (ex.character === 'CARRY') {
                    dtr.appendChild(td(fmt(ex.sets), 'num'));
                    dtr.appendChild(td(ex.meters > 0 ? fmt(ex.meters) + ' m' : fmtTime(ex.seconds), 'num'));
                    dtr.appendChild(td(ex.minWeightKg != null
                            ? fmt(ex.minWeightKg) + '–' + fmt(ex.maxWeightKg) + ' kg' : '—', 'num'));
                    dtr.appendChild(td('—'));
                } else if (ex.character === 'ISOMETRY') {
                    dtr.appendChild(td(fmt(ex.sets), 'num'));
                    dtr.appendChild(td(fmtTime(ex.seconds), 'num'));
                    dtr.appendChild(td('—'));
                    dtr.appendChild(td('—'));
                } else {
                    summaryCells(dtr, ex);
                }
                tbody.appendChild(dtr);
                detail.push(dtr);
            });

            btn.addEventListener('click', () => {
                const open = detail.length && detail[0].style.display !== 'none';
                detail.forEach(d => { d.style.display = open ? 'none' : ''; });
                btn.textContent = open ? '+' : '−';
            });
        });
    }

    // =====================================================================
    // P60: pět výkonnostních oblastí (relativně, protože jednotky se liší)
    // =====================================================================

    const AREAS = [
        { key: 'externalWeight', label: 'Externí váha (kg × opakování)' },
        { key: 'bodyweightReps', label: 'Vlastní váha (opakování)' },
        { key: 'timeUnderLoad', label: 'Čas pod zátěží (kg × s)' },
        { key: 'distanceUnderLoad', label: 'Vzdálenost pod zátěží (kg × m)' },
        { key: 'longCardioSeconds', label: 'Dlouhé kardio (čistý čas)' }
    ];

    let lastAreas = [];

    function renderAreas(points) {
        lastAreas = points;
        destroy('chartAreas');
        const el = document.getElementById('chartAreas');
        if (!el) return;

        // každou oblast normalizujeme na její vlastní maximum → jinak by jedna
        // jednotka (kg × s) přebila všechny ostatní
        const datasets = AREAS.map((a, i) => {
            const raw = points.map(p => parseFloat(p[a.key]) || 0);
            const max = Math.max.apply(null, raw.concat([0]));
            return {
                label: a.label,
                data: raw.map(v => (max > 0 ? (v / max) * 100 : 0)),
                rawValues: raw,
                borderColor: COLORS[i],
                backgroundColor: COLORS[i] + '33',
                tension: 0.2,
                fill: false
            };
        });

        charts.chartAreas = new Chart(el.getContext('2d'), {
            type: 'line',
            data: { labels: points.map(p => p.date), datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: ctx => ctx.dataset.label + ': '
                                    + fmt(ctx.dataset.rawValues[ctx.dataIndex])
                                    + ' (' + Math.round(ctx.parsed.y) + " % maxima)"
                        }
                    }
                },
                scales: { y: { beginAtZero: true, title: { display: true, text: '% maxima období' } } },
                onClick: (evt, els) => {
                    if (!els.length) return;
                    const point = points[els[0].index];
                    if (point) window.location.href = '/diary/' + point.trainingId;
                }
            }
        });
        renderAreaRadar();
    }

    function renderAreaRadar() {
        destroy('chartAreaRadar');
        const el = document.getElementById('chartAreaRadar');
        if (!el) return;

        const rawTotals = AREAS.map(a => lastAreas.reduce((sum, p) => sum + (parseFloat(p[a.key]) || 0), 0));
        const max = Math.max.apply(null, rawTotals.concat([0]));
        const values = rawTotals.map(v => (max > 0 ? (v / max) * 100 : 0));

        charts.chartAreaRadar = new Chart(el.getContext('2d'), {
            type: 'radar',
            data: {
                labels: AREAS.map(a => a.label),
                datasets: [{
                    label: 'Zastoupení oblastí',
                    data: values,
                    rawValues: rawTotals,
                    borderColor: COLORS[0],
                    backgroundColor: COLORS[0] + '22',
                    pointBackgroundColor: AREAS.map((_, i) => COLORS[i % COLORS.length]),
                    pointBorderColor: '#F1EFE9',
                    pointHoverBackgroundColor: '#F1EFE9',
                    pointHoverBorderColor: AREAS.map((_, i) => COLORS[i % COLORS.length]),
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        onClick: () => {},
                        labels: {
                            generateLabels: () => AREAS.map((a, i) => ({
                                text: a.label,
                                fillStyle: COLORS[i % COLORS.length],
                                strokeStyle: COLORS[i % COLORS.length],
                                lineWidth: 2,
                                hidden: false
                            }))
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: ctx => {
                                const raw = ctx.dataset.rawValues[ctx.dataIndex] || 0;
                                const pct = Math.round(ctx.parsed.r || 0);
                                return ctx.label + ': ' + pct + ' % maxima (' + fmt(raw) + ')';
                            }
                        }
                    }
                },
                scales: {
                    r: {
                        beginAtZero: true,
                        suggestedMax: 100,
                        ticks: { callback: v => v + ' %' }
                    }
                }
            }
        });
    }

    // =====================================================================
    // P61: rozpady (koláče, sloupce, radar obtížnosti)
    // =====================================================================

    function mapChart(id, type, map, clickHandler) {
        destroy(id);
        const el = document.getElementById(id);
        if (!el) return;
        const labels = Object.keys(map || {});
        const values = labels.map(k => map[k]);
        charts[id] = new Chart(el.getContext('2d'), {
            type,
            data: {
                labels,
                datasets: [{
                    label: 'Počet',
                    data: values,
                    backgroundColor: labels.map((_, i) => COLORS[i % COLORS.length]),
                    borderColor: type === 'radar' ? COLORS[0] : undefined
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: type === 'bar' ? 'none' : 'bottom',
                                     display: type !== 'bar' } },
                scales: type === 'bar' ? { y: { beginAtZero: true } } : {},
                onClick: (evt, els) => {
                    if (!els.length || !clickHandler) return;
                    clickHandler(labels[els[0].index]);
                }
            }
        });
    }

    function renderDistributions(d) {
        mapChart('chartTypes', 'pie', d.byExerciseType);
        mapChart('chartDifficultyRadar', 'radar', d.byDifficulty);
        mapChart('chartBodyParts', 'pie', d.byBodyPart);
        mapChart('chartLaterality', 'pie', d.byLaterality);
        mapChart('chartLoadKind', 'pie', d.byLoadKind);
        mapChart('chartIsolation', 'pie', d.byIsolation);
        mapChart('chartCharacter', 'bar', d.byCharacter);
        mapChart('chartAllTags', 'bar', d.byTag);

        // konkrétní čísla k charakteru provedení (P.O. / kg / m / s)
        const out = document.getElementById('characterNumbers');
        if (out) {
            const parts = [];
            parts.push('Repetitivní: ' + fmt(d.repetitiveReps) + ' P.O.'
                       + (parseFloat(d.repetitiveKg) > 0 ? ' · ' + fmt(d.repetitiveKg) + ' kg' : ''));
            parts.push('Nošení: ' + fmt(d.carryMeters) + ' m · ' + fmt(d.carrySeconds) + ' s');
            parts.push('Izometrie: ' + fmt(d.isometrySeconds) + ' s');
            out.textContent = parts.join(' | ');
        }
    }

    // =====================================================================
    // Filtr tagů tréninku
    // =====================================================================

    const TAG_CATEGORIES = [
        { key: 'GENERAL', label: 'Obecné' },
        { key: 'BODY_REGION', label: 'Oblast těla' },
        { key: 'MOVEMENT_PATTERN', label: 'Pohybový vzorec' },
        { key: 'EQUIPMENT', label: 'Náčiní/nářadí/pomůcky' }
    ];

    function sortedTags(tags) {
        return [...tags].sort((a, b) => (a.name || '').localeCompare(b.name || '', 'cs'));
    }

    function tagCheckbox(tag, onChange) {
        const label = document.createElement('label');
        label.className = 'form-check small m-0';
        label.style.cssText = 'display:flex;gap:5px;align-items:center;cursor:pointer;';
        const chk = document.createElement('input');
        chk.type = 'checkbox';
        chk.className = 'form-check-input';
        chk.value = tag.id;
        chk.addEventListener('change', onChange);
        const span = document.createElement('span');
        span.textContent = tag.name;
        label.appendChild(chk);
        label.appendChild(span);
        return label;
    }

    function tagCheckboxes(container, tags, onChange) {
        if (!container) return;
        container.innerHTML = '';
        if (!tags.length) {
            container.textContent = 'Zatím nemáš žádné tagy.';
            return;
        }

        const sorted = sortedTags(tags);
        TAG_CATEGORIES.forEach(category => {
            const categoryTags = sorted.filter(tag => (tag.category || 'GENERAL') === category.key);
            if (!categoryTags.length) return;

            const row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:flex-start;gap:8px;flex-wrap:wrap;margin-bottom:6px;';
            const title = document.createElement('strong');
            title.textContent = category.label + ':';
            title.style.cssText = 'min-width:160px;padding-top:2px;';
            row.appendChild(title);
            categoryTags.forEach(tag => row.appendChild(tagCheckbox(tag, onChange)));
            container.appendChild(row);
        });
    }

    function selectedIds(container) {
        if (!container) return [];
        return Array.from(container.querySelectorAll('input[type="checkbox"]:checked'))
            .map(c => c.value);
    }
    // =====================================================================
    // P63: sledované proměnné
    // =====================================================================

    function renderVariables(points) {
        destroy('chartVariables');
        const el = document.getElementById('chartVariables');
        if (!el) return;

        const series = [
            { label: 'RPE cviků (průměr)', get: p => p.avgExerciseRpe },
            { label: 'RPE tréninku', get: p => p.trainingRpe },
            { label: 'TF ráno', get: p => p.restingHrBpm },
            { label: 'TF průměrná', get: p => p.avgHrBpm },
            { label: 'TF maximální', get: p => p.maxHrBpm },
            { label: 'Tělesná váha (kg)', get: p => p.bodyweightKg },
            { label: 'Kvalita spánku (1–10)', get: p => p.sleepQualityRpe },
            { label: 'Spánek (0–100)', get: p => p.sleepQuality },
            { label: 'Kardio (min)', get: p => (p.cardioSeconds || 0) / 60 },
            { label: 'Nazvedáno (kg)', get: p => p.liftedKg },
            { label: 'Opakování vlastní vahou', get: p => p.bodyweightReps }
        ];

        charts.chartVariables = new Chart(el.getContext('2d'), {
            type: 'line',
            data: {
                labels: points.map(p => p.date + (p.cyclePhaseLetter ? ' ' + p.cyclePhaseLetter : '')),
                datasets: series.map((s, i) => ({
                    label: s.label,
                    data: points.map(p => {
                        const v = s.get(p);
                        return v == null ? null : parseFloat(v);
                    }),
                    borderColor: COLORS[i % COLORS.length],
                    backgroundColor: COLORS[i % COLORS.length] + '33',
                    spanGaps: true,
                    tension: 0.2,
                    fill: false,
                    hidden: i > 2
                }))
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
                scales: { y: { beginAtZero: true } },
                onClick: (evt, els) => {
                    if (!els.length) return;
                    const point = points[els[0].index];
                    if (point) window.location.href = '/diary/' + point.trainingId;
                }
            }
        });
    }

    // =====================================================================
    // Načítání
    // =====================================================================

    const trainingTagBox = document.getElementById('trainingTagFilter');

    function loadTrainings() {
        const ids = selectedIds(trainingTagBox);
        api('trainings', ids.map(i => '&tagIds=' + i).join(''))
            .then(renderTrainings)
            .catch(err => console.error('Tréninky za období', err));
    }
    function loadAll() {
        loadTrainings();
        api('areas').then(renderAreas).catch(err => console.error('Oblasti', err));
        api('distributions').then(renderDistributions).catch(err => console.error('Rozpady', err));
        api('variables').then(renderVariables).catch(err => console.error('Proměnné', err));

        const exSelect = document.getElementById('exStatSelect');
        if (exSelect && exSelect.value) {
            api('exercise-summary', '&name=' + encodeURIComponent(exSelect.value))
                .then(renderExerciseSummary)
                .catch(err => console.error('Souhrn cviku', err));
        }
    }
    // tagy do filtru tréninků
    fetch('/api/tags', { credentials: 'same-origin' })
        .then(r => r.ok ? r.json() : [])
        .then(tags => {
            tagCheckboxes(trainingTagBox, tags, loadTrainings);
        })
        .catch(() => {});

    const exSelect = document.getElementById('exStatSelect');
    if (exSelect) {
        exSelect.addEventListener('change', () => {
            if (!exSelect.value) return;
            api('exercise-summary', '&name=' + encodeURIComponent(exSelect.value))
                .then(renderExerciseSummary)
                .catch(err => console.error('Souhrn cviku', err));
        });
    }

    const refresh = document.getElementById('refreshBtn');
    if (refresh) refresh.addEventListener('click', loadAll);

    loadAll();
})();
