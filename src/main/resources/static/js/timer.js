/**
 * Timer modul pro EMOM, Tabata, Circuit, AMRAP.
 *
 * Veřejné API:
 *   TrainingTimer.runEmom({ totalMinutes, intervalSeconds, defaultReps, defaultWeightKg, minuteOverrides })
 *   TrainingTimer.runTabata({ rounds, workSeconds, restSeconds, defaultReps, defaultWeightKg })
 *   TrainingTimer.runCircuit({ rounds, steps: [{name, durationSeconds, restSeconds, ...}] })
 *   TrainingTimer.runAmrap({ timecapSeconds, targetRepsPerRound })
 *
 * Závisí na <div id="timerModal"> v DOM (modal z fragmentu).
 *
 * Funkce:
 *  - Velký mm:ss displej
 *  - Beep při přechodu fází (Web Audio API)
 *  - Wake Lock — telefon nezhasne
 *  - Start / Pause / Resume / Reset / Close
 *  - Mute toggle
 */
(function (root) {
    'use strict';

    let intervalId = null;
    let wakeLock = null;
    let audioCtx = null;
    let muted = false;

    // ---------- Audio (Web Audio API) ----------
    function ensureAudio() {
        if (audioCtx) return audioCtx;
        try {
            audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            console.warn('AudioContext not available', e);
        }
        return audioCtx;
    }

    function beep(frequency, durationMs) {
        if (muted) return;
        const ctx = ensureAudio();
        if (!ctx) return;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.value = frequency;
        gain.gain.value = 0.18;
        osc.connect(gain).connect(ctx.destination);
        osc.start();
        setTimeout(() => { osc.stop(); }, durationMs);
    }

    function beepShort() { beep(880, 120); }
    function beepLong()  { beep(660, 300); }
    function beepDouble() { beepShort(); setTimeout(beepShort, 200); }

    // ---------- Wake Lock ----------
    async function acquireWakeLock() {
        if (!('wakeLock' in navigator)) return;
        try {
            wakeLock = await navigator.wakeLock.request('screen');
        } catch (e) {
            console.warn('Wake lock failed', e);
        }
    }
    function releaseWakeLock() {
        if (wakeLock) { wakeLock.release().catch(() => {}); wakeLock = null; }
    }

    // ---------- UI ----------
    function format(seconds) {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    }

    function ui() {
        return {
            modal: document.getElementById('timerModal'),
            display: document.getElementById('timerDisplay'),
            phase: document.getElementById('timerPhase'),
            sub: document.getElementById('timerSub'),
            btnPause: document.getElementById('timerBtnPause'),
            btnClose: document.getElementById('timerBtnClose'),
            btnMute: document.getElementById('timerBtnMute'),
        };
    }

    function showModal(title) {
        const els = ui();
        if (!els.modal) { alert('Timer modal není v DOM — chybí fragment.'); return false; }
        els.modal.style.display = 'flex';
        document.getElementById('timerTitle').textContent = title;
        return true;
    }

    function closeModal() {
        clearInterval(intervalId); intervalId = null;
        releaseWakeLock();
        const els = ui();
        if (els.modal) els.modal.style.display = 'none';
    }

    function setDisplay(seconds, phaseLabel, sub) {
        const els = ui();
        if (els.display) els.display.textContent = format(seconds);
        if (els.phase) els.phase.textContent = phaseLabel || '';
        if (els.sub) els.sub.textContent = sub || '';
    }

    // Generic tick loop. tickFn(elapsedSec) returns {seconds, phase, sub, done?}
    function start(getState) {
        clearInterval(intervalId);
        let elapsed = 0;
        let paused = false;
        acquireWakeLock();
        beepDouble();

        const els = ui();
        if (els.btnPause) {
            els.btnPause.textContent = 'Pauza';
            els.btnPause.onclick = () => {
                paused = !paused;
                els.btnPause.textContent = paused ? 'Pokračovat' : 'Pauza';
            };
        }
        if (els.btnClose) els.btnClose.onclick = closeModal;
        if (els.btnMute) {
            els.btnMute.textContent = muted ? '🔇' : '🔊';
            els.btnMute.onclick = () => {
                muted = !muted;
                els.btnMute.textContent = muted ? '🔇' : '🔊';
            };
        }

        // initial frame
        const initial = getState(0);
        setDisplay(initial.seconds, initial.phase, initial.sub);

        intervalId = setInterval(() => {
            if (paused) return;
            elapsed++;
            const s = getState(elapsed);
            setDisplay(s.seconds, s.phase, s.sub);
            if (s.beepShort) beepShort();
            if (s.beepLong)  beepLong();
            if (s.done) {
                clearInterval(intervalId); intervalId = null;
                beepLong();
                setTimeout(() => alert('Hotovo!'), 50);
            }
        }, 1000);
    }

    // ---------- EMOM ----------
    function runEmom(cfg) {
        if (!cfg.totalMinutes) { alert('EMOM: chybí totalMinutes'); return; }
        const intervalS = cfg.intervalSeconds || 60;
        const totalSeconds = cfg.totalMinutes * intervalS;
        if (!showModal('EMOM ' + cfg.totalMinutes + ' min')) return;

        start(elapsed => {
            const remaining = totalSeconds - elapsed;
            if (remaining <= 0) return { seconds: 0, phase: 'Hotovo', sub: '', done: true };
            const currentInterval = Math.floor(elapsed / intervalS) + 1;
            const inIntervalLeft = intervalS - (elapsed % intervalS);
            const phase = 'Minuta ' + currentInterval + ' / ' + cfg.totalMinutes;
            // Reps default + případný override pro tuto minutu
            let sub = '';
            if (cfg.defaultReps) sub += cfg.defaultReps + ' reps';
            if (cfg.defaultWeightKg) sub += (sub ? ' · ' : '') + cfg.defaultWeightKg + ' kg';
            const override = (cfg.minuteOverrides || []).find(o => o.minuteIndex === currentInterval);
            if (override) {
                let oSub = '';
                if (override.reps != null) oSub += override.reps + ' reps';
                if (override.weightKg != null) oSub += (oSub ? ' · ' : '') + override.weightKg + ' kg';
                if (override.note) oSub += (oSub ? ' · ' : '') + override.note;
                if (oSub) sub = '⚡ ' + oSub;
            }
            // Beep když začíná nová minuta (každých intervalS sekund)
            const beepNow = inIntervalLeft === intervalS && elapsed > 0;
            return { seconds: inIntervalLeft, phase, sub, beepShort: beepNow };
        });
    }

    // ---------- TABATA ----------
    function runTabata(cfg) {
        const rounds = cfg.rounds || 8;
        const work = cfg.workSeconds || 20;
        const rest = cfg.restSeconds || 10;
        const total = rounds * (work + rest);
        if (!showModal('Tabata ' + rounds + ' × ' + work + '/' + rest)) return;

        start(elapsed => {
            if (elapsed >= total) return { seconds: 0, phase: 'Hotovo', sub: '', done: true };
            const roundDur = work + rest;
            const round = Math.floor(elapsed / roundDur) + 1;
            const inRound = elapsed % roundDur;
            const isWork = inRound < work;
            const remaining = isWork ? (work - inRound) : (roundDur - inRound);
            let sub = '';
            if (cfg.defaultReps) sub += cfg.defaultReps + ' reps';
            if (cfg.defaultWeightKg) sub += (sub ? ' · ' : '') + cfg.defaultWeightKg + ' kg';
            return {
                seconds: remaining,
                phase: (isWork ? '💪 PRÁCE' : '😮‍💨 Pauza') + ' · kolo ' + round + '/' + rounds,
                sub,
                beepShort: (isWork && remaining === work) || (!isWork && remaining === rest)
            };
        });
    }

    // ---------- AMRAP ----------
    function runAmrap(cfg) {
        if (!cfg.timecapSeconds) { alert('AMRAP: chybí timecapSeconds'); return; }
        if (!showModal('AMRAP ' + Math.floor(cfg.timecapSeconds / 60) + ' min')) return;

        start(elapsed => {
            const remaining = cfg.timecapSeconds - elapsed;
            if (remaining <= 0) return { seconds: 0, phase: 'Hotovo — spočítej kola', sub: '', done: true };
            let sub = '';
            if (cfg.targetRepsPerRound) sub += 'cíl ' + cfg.targetRepsPerRound + ' reps/kolo';
            // Pípnutí v posledních 10 sekundách
            return {
                seconds: remaining,
                phase: 'AMRAP — kolik kol stihneš',
                sub,
                beepShort: remaining <= 10 && remaining > 0
            };
        });
    }

    // ---------- CIRCUIT ----------
    function runCircuit(cfg) {
        const rounds = cfg.rounds || 3;
        const steps = (cfg.steps || []).filter(s => s.durationSeconds);
        if (steps.length === 0) { alert('Circuit: žádný krok s durationSeconds — nedá se měřit timerem.'); return; }
        if (!showModal('Circuit ' + rounds + ' kol')) return;

        // Generuj sekvenci: pro každé kolo, pro každý step: work + (rest if exists), pak rest_between_rounds
        const sequence = [];
        for (let r = 1; r <= rounds; r++) {
            steps.forEach((s, i) => {
                sequence.push({ kind: 'work', duration: s.durationSeconds, label: s.name, round: r });
                if (s.restSeconds && s.restSeconds > 0) {
                    sequence.push({ kind: 'rest', duration: s.restSeconds, label: 'Pauza', round: r });
                }
            });
            if (r < rounds && cfg.restBetweenRoundsS && cfg.restBetweenRoundsS > 0) {
                sequence.push({ kind: 'roundrest', duration: cfg.restBetweenRoundsS, label: 'Mezi koly', round: r });
            }
        }
        const totalDuration = sequence.reduce((sum, p) => sum + p.duration, 0);

        start(elapsed => {
            if (elapsed >= totalDuration) return { seconds: 0, phase: 'Hotovo', sub: '', done: true };
            // Najdi aktuální fázi
            let acc = 0;
            for (const phase of sequence) {
                if (elapsed < acc + phase.duration) {
                    const inPhase = elapsed - acc;
                    const remaining = phase.duration - inPhase;
                    const icon = phase.kind === 'work' ? '💪 ' : '😮‍💨 ';
                    return {
                        seconds: remaining,
                        phase: icon + phase.label,
                        sub: 'Kolo ' + phase.round + '/' + rounds,
                        beepShort: remaining === phase.duration
                    };
                }
                acc += phase.duration;
            }
            return { seconds: 0, phase: 'Hotovo', sub: '', done: true };
        });
    }

    root.TrainingTimer = {
        runEmom, runTabata, runAmrap, runCircuit, close: closeModal
    };
})(window);
