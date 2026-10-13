// @ts-check

/**
 * The interaction layer: run loop, controls, readouts, keys, capture.
 *
 * The original's scheme is better than its reputation and mostly survives (CODE_ANATOMY
 * §5.9). Left and right mouse buttons each bind to a parameter; dragging changes it; ←/→
 * change a sensitivity multiplier in powers of two, so one binding covers coarse and fine.
 * The live value prints in the corner.
 *
 * What is added, because a website has visitors who have never used it: a labelled slider
 * per exposed parameter alongside the drag binding, and pause/step/reset/reroll. The
 * original had `modelRunDays = 0` and a kill switch, which suited an experiment station and
 * does not suit a page someone lands on.
 *
 * Nothing here reaches into the engine: it drives a scenario through the same `Parameter`
 * seam the headless sweeps use.
 */

import { Recorder } from '../record.js';
import { World } from '../world.js';

/**
 * @param {object} spec
 * @param {HTMLElement} spec.container
 * @param {import('../scenario.js').Scenario} spec.scenario
 * @param {(canvas: HTMLCanvasElement) => {draw: (w: World) => void}} spec.makeView
 * @param {(world: World) => Record<string, string>} [spec.readout]
 * @param {string[]} [spec.shadeOptions] display variables the view accepts, if any
 * @param {{make: (c: HTMLCanvasElement) => {draw: (w: World) => void}, title?: string}} [spec.aux]
 *   a second panel, where one view cannot carry the result on its own — see `choose.js`
 * @param {{key: string, label: string}[]} [spec.overlays] optional layers the view can draw
 * @param {{when: (w: World) => boolean, caption: string}[]} [spec.steps] a walkthrough, fired
 *   on model *state* — never on day numbers, so it survives a reroll (BUILD_PLANNER §3.2)
 */
export function mount({
  container, scenario, makeView, readout, shadeOptions = [], aux, overlays = [], steps = [],
}) {
  container.innerHTML = '';
  container.classList.add('se-root');

  const canvas = el('canvas', 'se-canvas');
  const stage = el('div', 'se-stage');
  stage.append(canvas);

  // The walkthrough sits directly under the main canvas — it describes what is in that
  // canvas, and a reader should not have to look past a second chart to find the sentence
  // about the first one.
  const caption = el('div', 'se-caption se-step');
  if (steps.length > 0) stage.append(caption);

  /** @type {{draw: (w: World) => void, reset?: () => void} | null} */
  let auxView = null;
  if (aux) {
    const auxCanvas = el('canvas', 'se-canvas se-canvas-aux');
    if (aux.title) {
      const auxTitle = el('div', 'se-caption');
      auxTitle.textContent = aux.title;
      stage.append(auxTitle);
    }
    stage.append(auxCanvas);
    auxView = aux.make(/** @type {HTMLCanvasElement} */ (auxCanvas));
  }

  const panel = el('div', 'se-panel');
  const status = el('div', 'se-status');
  const controls = el('div', 'se-controls');       // model parameters; rebuilt on reset
  const viewControls = el('div', 'se-controls');   // speed and display; persistent
  const buttons = el('div', 'se-buttons');
  const dragHint = el('div', 'se-hint');
  panel.append(status, controls, viewControls, buttons, dragHint);
  container.append(stage, panel);

  const view = /** @type {{draw: (w: World) => void, reset?: () => void}} */ (
    makeView(/** @type {HTMLCanvasElement} */ (canvas)));

  /** @type {World} */
  let world;
  /** @type {import('../config.js').Parameter[]} */
  let params = [];
  let running = true;
  let daysPerFrame = 1;
  let multiplier = 0; // powers of two, as the original
  let seed = scenario.config().seed;
  const recorder = new Recorder();

  function build() {
    const config = scenario.config();
    config.seed = seed;
    world = new World(config);
    scenario.setUp?.(world);
    params = scenario.controls?.(world) ?? [];
    // The accumulating views hold a run's worth of history. Without this, reset and reroll
    // left the discarded run's line on the chart with the new one drawn over it — harmless
    // on the plane view, which holds no history, and wrong everywhere else.
    view.reset?.();
    auxView?.reset?.();
    reachedStep = -1;
    caption.textContent = '';
    renderControls();
  }

  /**
   * How far the walkthrough has got. Steps only ever advance: a caption fires when its
   * predicate first becomes true and stays until the next one does, so a model that dips back
   * below a threshold does not rewind the narration.
   */
  let reachedStep = -1;

  function advanceSteps() {
    for (let i = reachedStep + 1; i < steps.length; i++) {
      if (!steps[i].when(world)) break;
      reachedStep = i;
      caption.textContent = `${i + 1}/${steps.length}  ${steps[i].caption.replace(/\s+/g, ' ')}`;
    }
  }

  /** Kept in step with the sliders, so a drag or a scenario sweep moves them too.
   * @type {(() => void)[]} */
  let sliderRefreshers = [];

  /** The parameters a drag can move. A drag is a nudge along a range, which a toggle or a
   * list of choices does not have. @type {import('../config.js').Parameter[]} */
  let draggable = [];

  function renderControls() {
    controls.innerHTML = '';
    sliderRefreshers = [];
    draggable = params.filter((p) => p.kind === 'number');
    for (const p of params) {
      // Toggles and choices through the same seam as the sliders (Parameter's `kind`). The
      // value underneath is still a number — 0/1, or an index into `choices` — so sweeps,
      // `setParameter` and the headless runner need not know the difference.
      if (p.kind === 'boolean') {
        const row = el('label', 'se-check');
        const box = /** @type {HTMLInputElement} */ (el('input', 'se-checkbox'));
        box.type = 'checkbox';
        box.checked = p.get() >= 0.5;
        box.addEventListener('change', () => p.set(box.checked ? 1 : 0));
        const name = el('span', 'se-control-name');
        name.textContent = p.label;
        row.append(box, name);
        controls.append(row);
        sliderRefreshers.push(() => { box.checked = p.get() >= 0.5; });
        continue;
      }
      if (p.kind === 'choice' && p.choices) {
        const row = el('label', 'se-control');
        const name = el('span', 'se-control-name');
        name.textContent = p.label;
        const select = /** @type {HTMLSelectElement} */ (el('select', 'se-select'));
        p.choices.forEach((choice, i) => {
          const o = document.createElement('option');
          o.value = String(i);
          o.textContent = choice;
          select.append(o);
        });
        select.value = String(Math.round(p.get()));
        select.addEventListener('change', () => p.set(Number(select.value)));
        row.append(name, select);
        controls.append(row);
        sliderRefreshers.push(() => { select.value = String(Math.round(p.get())); });
        continue;
      }

      const row = el('label', 'se-control');
      const name = el('span', 'se-control-name');
      name.textContent = p.label;
      const value = el('span', 'se-control-value');
      value.textContent = fmt(p.get());

      const slider = /** @type {HTMLInputElement} */ (el('input', 'se-slider'));
      slider.type = 'range';
      slider.min = '0';
      slider.max = '1000';
      slider.value = String(Math.round(p.getNormalised() * 1000));
      slider.addEventListener('input', () => {
        p.setNormalised(Number(slider.value) / 1000);
        value.textContent = fmt(p.get());
      });

      row.append(name, value, slider);
      controls.append(row);
      sliderRefreshers.push(() => {
        slider.value = String(Math.round(p.getNormalised() * 1000));
        value.textContent = fmt(p.get());
      });
    }
    dragHint.textContent = draggable.length > 0
      ? `drag on the model: left = ${draggable[0].label}${draggable[1] ? `, right = ${draggable[1].label}` : ''} · ←/→ sensitivity`
      : '';
  }

  // --- buttons ---

  const pauseButton = button('pause', () => {
    running = !running;
    pauseButton.textContent = running ? 'pause' : 'run';
  });
  buttons.append(
    pauseButton,
    button('step', () => { running = false; pauseButton.textContent = 'run'; stepDay(); draw(); }),
    button('reset', () => { build(); draw(); }),
    // Same run every time, or a different one. E5 and E8 need this: which Firm wins is
    // genuinely chance, and that is the thing being taught.
    button('reroll', () => { seed = Math.floor(Math.random() * 1e9); build(); draw(); }),
    button('save png', capture),
  );

  // Speed gets the same labelled row as any other control. It sat in the button row with
  // nothing but a `title` before, which read as a stray slider that did nothing — and on a
  // model that settles in a few hundred days, moving it changes little you can see.
  const speedRow = el('label', 'se-control');
  const speedName = el('span', 'se-control-name');
  speedName.textContent = 'speed';
  const speedValue = el('span', 'se-control-value');
  speedValue.textContent = '1 day/frame';
  const speed = /** @type {HTMLInputElement} */ (el('input', 'se-slider'));
  speed.type = 'range';
  speed.min = '1';
  speed.max = '50';
  speed.value = '1';
  speed.addEventListener('input', () => {
    daysPerFrame = Number(speed.value);
    speedValue.textContent = `${daysPerFrame} day${daysPerFrame === 1 ? '' : 's'}/frame`;
  });
  speedRow.append(speedName, speedValue, speed);
  viewControls.append(speedRow);

  // What the People markers encode. Worth exposing rather than hardcoding: on the von
  // Thünen models the group is the result, and a density ramp would hide it; on C1 and C8
  // the density ramp is the result. 'auto' picks the right one per model.
  if (shadeOptions.length > 0) {
    const shadeRow = el('label', 'se-control');
    const shadeName = el('span', 'se-control-name');
    shadeName.textContent = 'shade People by';
    const select = /** @type {HTMLSelectElement} */ (el('select', 'se-select'));
    for (const option of shadeOptions) {
      const o = document.createElement('option');
      o.value = option;
      o.textContent = option;
      select.append(o);
    }
    select.addEventListener('change', () => {
      if ('shadeBy' in view) /** @type {any} */ (view).shadeBy = select.value;
      draw();
    });
    shadeRow.append(shadeName, select);
    viewControls.append(shadeRow);
  }

  // Optional layers: the trade lines, C1's equalising cutoff. Checkboxes rather than another
  // select, because they compose — buy lines under the cutoff ring is a legitimate thing to
  // want to look at, and on C1 it is the most informative combination there is.
  for (const overlay of overlays) {
    const row = el('label', 'se-check');
    const box = /** @type {HTMLInputElement} */ (el('input', 'se-checkbox'));
    box.type = 'checkbox';
    box.checked = Boolean(/** @type {any} */ (view)[overlay.key]);
    box.addEventListener('change', () => {
      /** @type {any} */ (view)[overlay.key] = box.checked;
      draw();
    });
    const name = el('span', 'se-control-name');
    name.textContent = overlay.label;
    row.append(box, name);
    viewControls.append(row);
  }

  // --- drag binding, as the original ---

  let dragging = -1;
  let lastX = 0;
  canvas.addEventListener('contextmenu', (e) => e.preventDefault());
  canvas.addEventListener('pointerdown', (e) => {
    dragging = e.button === 2 ? 1 : 0;
    lastX = e.clientX;
    canvas.setPointerCapture(e.pointerId);
  });
  canvas.addEventListener('pointermove', (e) => {
    if (dragging < 0 || !draggable[dragging]) return;
    const p = draggable[dragging];
    const dx = e.clientX - lastX;
    lastX = e.clientX;
    // Normalised drag scaled by the power-of-two multiplier, so one binding covers a coarse
    // sweep and a fine nudge without rebinding anything.
    p.nudge((dx / 300) * (p.max - p.min) * Math.pow(2, multiplier));
    refreshSliders();
  });
  canvas.addEventListener('pointerup', () => { dragging = -1; });

  // --- keys, matching the original where they still make sense ---

  window.addEventListener('keydown', (e) => {
    switch (e.key) {
      case ' ': running = !running; pauseButton.textContent = running ? 'pause' : 'run'; e.preventDefault(); break;
      case 'ArrowRight': multiplier++; break;
      case 'ArrowLeft': multiplier--; break;
      case 'k': running = false; pauseButton.textContent = 'run'; break;
      case 'p': capture(); break;
      case 'r': build(); draw(); break;
      default: break;
    }
  });

  function refreshSliders() {
    for (const fn of sliderRefreshers) fn();
  }

  /**
   * Save what is on screen, both panels together where there are two.
   *
   * The original's `a` key captured every window at one instant, and that was not a
   * convenience — the multi-window layout *was* the figure, and two panels captured a second
   * apart are two pictures of different moments. Composing them into one image keeps that.
   */
  function capture() {
    const panels = /** @type {HTMLCanvasElement[]} */ ([canvas]);
    const auxCanvas = stage.querySelector('.se-canvas-aux');
    if (auxCanvas) panels.push(/** @type {HTMLCanvasElement} */ (auxCanvas));

    const gap = panels.length > 1 ? 12 : 0;
    const width = Math.max(...panels.map((c) => c.width));
    const height = panels.reduce((a, c) => a + c.height, 0) + gap * (panels.length - 1);

    const sheet = document.createElement('canvas');
    sheet.width = width;
    sheet.height = height;
    const ctx = /** @type {CanvasRenderingContext2D} */ (sheet.getContext('2d'));
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, width, height);
    let y = 0;
    for (const c of panels) {
      ctx.drawImage(c, 0, y);
      y += c.height + gap;
    }

    sheet.toBlob((blob) => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${scenario.id}-day${world.day}.png`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  function stepDay() {
    scenario.beforeDay?.(world, world.day);
    world.timeline.nextDay();
  }

  function draw() {
    advanceSteps();
    view.draw(world);
    auxView?.draw(world);
    const lines = [`${scenario.id} · day ${world.day}`];
    const extra = readout?.(world) ?? {};
    for (const [k, v] of Object.entries(extra)) lines.push(`${k} ${v}`);
    if (params.length > 0) {
      lines.push(`sensitivity ×${Math.pow(2, multiplier).toFixed(multiplier < 0 ? 3 : 0)}`);
    }
    status.textContent = lines.join('\n');
  }

  function frame() {
    if (running) {
      for (let i = 0; i < daysPerFrame; i++) stepDay();
      refreshSliders(); // a scenario's beforeDay may have swept a bound parameter
    }
    draw();
    requestAnimationFrame(frame);
  }

  build();
  draw();
  requestAnimationFrame(frame);

  return {
    get world() { return world; },
    get parameters() { return params; },
    recorder,
    pause() { running = false; pauseButton.textContent = 'run'; },
    resume() { running = true; pauseButton.textContent = 'pause'; },

    /**
     * Set an exposed parameter from outside, on the *running* model.
     *
     * Deliberately not a restart. The sweep charts use this — click a swept density cost and
     * the settlement above reorganises towards it — and watching a settled model move to a
     * new equilibrium is worth more than watching a fresh one arrive at it. It is also the
     * only version that works: a restart rebuilds from `scenario.config()` and would throw
     * the clicked value away.
     *
     * @param {string} name @param {number} value
     */
    setParameter(name, value) {
      const p = params.find((x) => x.name === name);
      if (!p) return null;
      p.set(Math.min(p.max, Math.max(p.min, value)));
      refreshSliders();
      running = true;
      pauseButton.textContent = 'pause';
      return p;
    },
  };
}

/** @param {string} tag @param {string} className */
function el(tag, className) {
  const node = document.createElement(tag);
  node.className = className;
  return node;
}

/** @param {string} label @param {() => void} onClick */
function button(label, onClick) {
  const b = /** @type {HTMLButtonElement} */ (el('button', 'se-button'));
  b.type = 'button';
  b.textContent = label;
  b.addEventListener('click', onClick);
  return b;
}

/** @param {number} v */
function fmt(v) {
  if (v === 0) return '0';
  if (Math.abs(v) >= 100) return v.toFixed(0);
  if (Math.abs(v) >= 1) return v.toFixed(2);
  return v.toPrecision(3);
}
