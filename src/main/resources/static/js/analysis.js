/**
 * Analysis page (Phase 5) — fetchne /api/analysis/* endpointy a vykreslí Chart.js grafy.
 */
(function () {
    'use strict';

    const charts = {};

    function isoDaysAgo(n) {
        const d = new Date();
        d.setDate(d.getDate() - n);
        return d.toISOString().slice(0, 10);
    }
    function isoToday() {
        return new Date().toISOString().slice(0, 10);
    }

    function getPeriod() {
        const sel = document.getElementById('periodSelect').value;
        if (sel === 'custom') {
            const from = document.getElementById('fromDate').value;
            const to = document.getElementById('toDate').value;
            return { from, to };
        }
        return { from: isoDaysAgo(parseInt(sel, 10)), to: isoToday() };
    }

    function fetchJson(url) {
        return fetch(url, { credentials: 'same-origin' })
            .then(r => r.ok ? r.json() : Promise.reject(r.statusText));
    }

    function destroy(key) {
        if (charts[key]) { charts[key].destroy(); delete charts[key]; }
    }

    function lineChart(canvasId, label, points, color) {
        destroy(canvasId);
        const ctx = document.getElementById(canvasId).getContext('2d');
        charts[canvasId] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: points.map(p => p.date),
                datasets: [{
                    label,
                    data: points.map(p => parseFloat(p.value)),
                    borderColor: color,
                    backgroundColor: color + '33',
                    tension: 0.2,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    function barChart(canvasId, label, points, color) {
        destroy(canvasId);
        const ctx = document.getElementById(canvasId).getContext('2d');
        charts[canvasId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: points.map(p => p.label),
                datasets: [{
                    label,
                    data: points.map(p => parseFloat(p.value)),
                    backgroundColor: color
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    function loadBodyRegion(p) {
        return fetchJson(`/api/analysis/sets-per-body-region?from=${p.from}&to=${p.to}`)
            .then(data => barChart('chartBodyRegion', 'Sety', data, '#0dcaf0'));
    }
    function loadMovementPattern(p) {
        return fetchJson(`/api/analysis/sets-per-movement-pattern?from=${p.from}&to=${p.to}`)
            .then(data => barChart('chartMovementPattern', 'Sety', data, '#198754'));
    }

    function loadPrHistory(p) {
        const exId = document.getElementById('prExerciseSelect').value;
        const prDiv = document.getElementById('prMax');
        if (!exId) {
            destroy('chartPr');
            prDiv.style.display = 'none';
            return Promise.resolve();
        }
        return Promise.all([
            fetchJson(`/api/analysis/pr-history?catalogItemId=${exId}&from=${p.from}&to=${p.to}`),
            fetchJson(`/api/analysis/max-weight?catalogItemId=${exId}&from=${p.from}&to=${p.to}`)
        ]).then(([history, max]) => {
            lineChart('chartPr', 'Max kg', history, '#6f42c1');
            if (max.maxWeightKg) {
                prDiv.querySelector('strong').textContent = max.maxWeightKg + ' kg';
                prDiv.style.display = '';
            } else {
                prDiv.style.display = 'none';
            }
        });
    }

    function loadHeatmap(p) {
        return Promise.all([
            fetchJson(`/api/analysis/frequency-heatmap?from=${p.from}&to=${p.to}`),
            fetchJson(`/api/analysis/rpe-per-day?from=${p.from}&to=${p.to}`)
        ]).then(([freq, rpe]) => {
            const root = document.getElementById('heatmap');
            root.innerHTML = '';
            const fromD = new Date(p.from);
            const toD = new Date(p.to);
            const oneDay = 86400000;
            for (let t = fromD.getTime(); t <= toD.getTime(); t += oneDay) {
                const d = new Date(t);
                const iso = d.toISOString().slice(0, 10);
                const count = freq[iso] || 0;
                const r = rpe[iso];
                const cell = document.createElement('div');
                cell.style.width = '14px';
                cell.style.height = '14px';
                cell.style.borderRadius = '2px';
                if (count === 0) cell.style.backgroundColor = '#e9ecef';
                else if (count === 1) cell.style.backgroundColor = '#9ec5fe';
                else cell.style.backgroundColor = '#0d6efd';
                cell.title = iso + (count ? ` · ${count} trénink(ů)` : ' · žádný') + (r != null ? ` · RPE ${r}` : '');
                root.appendChild(cell);
            }
        });
    }

    function loadAll() {
        const p = getPeriod();
        if (!p.from || !p.to) return;
        Promise.all([
            loadBodyRegion(p),
            loadMovementPattern(p),
            loadPrHistory(p),
            loadHeatmap(p)
        ]).catch(err => {
            console.error('Analysis load error', err);
        });
    }

    // Setup
    document.getElementById('periodSelect').addEventListener('change', function () {
        const isCustom = this.value === 'custom';
        document.getElementById('customFromWrap').style.display = isCustom ? '' : 'none';
        document.getElementById('customToWrap').style.display = isCustom ? '' : 'none';
        if (isCustom) {
            document.getElementById('fromDate').value = document.getElementById('fromDate').value || isoDaysAgo(30);
            document.getElementById('toDate').value = document.getElementById('toDate').value || isoToday();
        }
    });
    document.getElementById('refreshBtn').addEventListener('click', loadAll);
    document.getElementById('prExerciseSelect').addEventListener('change', () => loadPrHistory(getPeriod()));

    // Initial load
    loadAll();
})();
