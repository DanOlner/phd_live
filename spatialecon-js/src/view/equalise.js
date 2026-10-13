// @ts-check

/**
 * The equalising line on C1, drawn over the settlement. CODE_ANATOMY §5.3.
 *
 * C1's result is that People arrange themselves until nobody can improve by moving, which
 * means utility is equal everywhere — density traded off exactly against distance. It is
 * *almost* true, and the exception is the interesting part.
 *
 * With one Firm the whole thing has a closed form. A Person at distance d earns the wage w,
 * pays a density cost D(d) out of it, and faces a delivered price p₀ + c·d, so
 *
 *     U(d) = (w − D(d)) / (p₀ + c·d)
 *
 * For dU/dd = 0 at some common level U*, the density profile must be exactly
 *
 *     D(d) = w − U*·(p₀ + c·d)
 *
 * — linear in distance, and reaching **zero** at
 *
 *     d* = (w/U* − p₀) / c
 *
 * The model produces that line: corr(distance, density) = −0.985 here, −0.986 in the Java.
 * But density cannot go below zero. A Bundle is discarded when its budget is non-positive,
 * and density is subtracted from the wage, so past d* there is nothing left to trade away.
 * Those People pay pure distance cost, their utility tracks w/(p₀ + c·d), and it falls.
 *
 * They are not in disequilibrium — no move improves them. They are outside the range over
 * which equalisation is *possible*. It is a finite-population boundary effect, and it thins
 * with numbers exactly as that implies: over 125 / 250 / 500 / 1000 People the fringe falls
 * 43.2% → 20.4% → 7.2% → 1.9%.
 *
 * So the overlay draws d* as a ring and rings the People outside it. The pleasing part, and
 * the reason this is worth a picture rather than a paragraph, is that C8 is the same
 * mechanism: raising the density cost pulls d* inward until the fringe swallows the
 * settlement. Turn the density cost up with this on and watch the ring close. C1 and C8 are
 * one continuum, not two regimes.
 */

/**
 * Applicable only where the closed form is: one Firm, and a density cost to trade off.
 * @param {import('../world.js').World} world
 */
export function equalisingLineApplies(world) {
  return world.firms.length === 1 && world.config.densityCost > 0 && world.people.length > 1;
}

/**
 * The cutoff, and who is beyond it. Returns null where the closed form does not apply, or
 * where the settlement has not developed enough for it to mean anything.
 * @param {import('../world.js').World} world
 */
export function equalisingCutoff(world) {
  if (!equalisingLineApplies(world)) return null;
  const firm = world.firm(0);

  // The equalised level, estimated as the MEDIAN utility.
  //
  // The distribution is a spike at U* — everyone in the interior, who by construction all
  // reach the same level — plus a left tail of fringe People who have no density left to
  // trade. So as long as the fringe is under half the population, the median *is* U*, and
  // it is unmoved by how long that tail is.
  //
  // Taking the maximum instead is the obvious thing and is wrong: it picks the luckiest draw
  // rather than the shared level, so it overstates U*, understates d*, and counts interior
  // People as fringe. It is worth being precise about because the estimator is checkable —
  // the fringe series it produces over 125 / 250 / 500 / 1000 People is 44.0 / 20.0 / 7.6 /
  // 2.0 per cent, against the 43.2 / 20.4 / 7.2 / 1.9 measured independently in the Java
  // (CODE_ANATOMY §5.3). The maximum gives 93.6 / 48.0 / 18.6 / 4.9, which matches nothing.
  const sorted = world.people.map((p) => p.utility).sort((a, b) => a - b);
  const level = sorted[Math.floor(sorted.length / 2)];
  if (!(level > 0)) return null;

  const c = firm.deliveryCost;
  if (c <= 0) return null;
  const cutoff = (firm.wage / level - firm.goodCost) / c;
  if (!Number.isFinite(cutoff) || cutoff <= 0) return null;

  let beyond = 0;
  for (const p of world.people) if (world.distance(p, firm) > cutoff) beyond++;

  return { cutoff, level, beyond, fraction: beyond / world.people.length };
}

/**
 * @param {CanvasRenderingContext2D} ctx
 * @param {import('../world.js').World} world
 * @param {(v: number) => number} s model units → device pixels
 * @param {number} dpr
 */
export function drawEqualisingLine(ctx, world, s, dpr) {
  const found = equalisingCutoff(world);
  if (!found) return;
  const firm = world.firm(0);
  const x = s(firm.position.x);
  const y = s(firm.position.y);

  // The ring itself: where density runs out and equalisation stops being possible.
  ctx.strokeStyle = '#000';
  ctx.lineWidth = Math.max(1.4 * dpr, 1.4);
  ctx.setLineDash([7 * dpr, 5 * dpr]);
  ctx.beginPath();
  ctx.arc(x, y, s(found.cutoff), 0, Math.PI * 2);
  ctx.stroke();
  ctx.setLineDash([]);

  // The fringe, ringed. Naming them individually is the point: without this the claim is a
  // number in a caption, and with it you can see that they are a boundary, not a scatter.
  ctx.lineWidth = Math.max(1 * dpr, 1);
  ctx.strokeStyle = 'rgba(0,0,0,0.75)';
  for (const p of world.people) {
    if (world.distance(p, firm) <= found.cutoff) continue;
    ctx.beginPath();
    ctx.arc(s(p.position.x), s(p.position.y), Math.max(s(0.02), 4 * dpr), 0, Math.PI * 2);
    ctx.stroke();
  }

  const font = Math.max(Math.round(s(0.028)), 10 * dpr);
  ctx.font = `${font}px ui-monospace, Menlo, monospace`;
  ctx.textBaseline = 'top';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#000';
  const lines = [
    `equalised U* = ${round(found.level)}`,
    `cutoff d* = ${round(found.cutoff)}`,
    `fringe ${found.beyond}/${world.people.length} (${(found.fraction * 100).toFixed(1)}%)`,
  ];
  lines.forEach((line, i) => ctx.fillText(line, font * 0.6, font * (0.6 + i * 1.3)));
}

/** @param {number} v */
function round(v) {
  return Number(v.toPrecision(3)).toString();
}
