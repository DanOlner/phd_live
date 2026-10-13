// @ts-check

/**
 * `PeopleBuyLines` — a line from each Person to each Firm they bought from, weight
 * proportional to the quantity. CODE_ANATOMY §5.9.
 *
 * Dormant in the source tree and in all seven jars, so it appears in no published figure. It
 * is nonetheless the most explanatory view in the original, because it is the only one that
 * draws the **trade itself** rather than its consequences. Everything else on the plane —
 * marker size, the density ink, where a Firm has crawled to — is an outcome. This is the
 * transactions those outcomes are made of.
 *
 * It answers the question a newcomer actually asks first, which is *what does a Person
 * buying from a Firm mean here?* Two things become obvious the moment it is switched on, and
 * both are load-bearing for the rest of the thesis:
 *
 * - **Everyone buys from everyone.** Under CES with ρ < 1 no Firm is ever starved by being
 *   slightly dearer, which is what keeps the transmission belt away from Bertrand
 *   competition (§6.3). The picture is a mesh, not a partition into catchments — until ρ
 *   approaches 1, when it collapses towards one.
 * - **The weights fall off with distance and nothing enforces it.** No Person is assigned a
 *   Firm and no rule caps a trade radius; delivered price rises with distance, and that is
 *   the whole of it.
 *
 * Drawn beneath the markers, so it never obscures the People it connects.
 *
 * @param {CanvasRenderingContext2D} ctx
 * @param {import('../world.js').World} world
 * @param {(v: number) => number} s model units → device pixels
 * @param {number} dpr
 */
export function drawBuyLines(ctx, world, s, dpr) {
  let heaviest = 0;
  for (const p of world.people) {
    for (const q of p.lastQuantities) if (q > heaviest) heaviest = q;
  }
  if (heaviest <= 0) return;

  // Weight is scaled to the heaviest trade in the picture rather than to an absolute, so the
  // view stays readable across models whose quantities differ by orders of magnitude — a
  // partial model's Person buys on a wage, a transmission-belt Person on a single day.
  const maxWidth = Math.max(s(0.004), 1.2 * dpr);

  for (const person of world.people) {
    const x = s(person.position.x);
    const y = s(person.position.y);
    for (let i = 0; i < person.lastSellers.length; i++) {
      const quantity = i < person.lastQuantities.length ? person.lastQuantities[i] : 0;
      if (quantity <= 0) continue;
      const firm = person.lastSellers[i];
      const t = quantity / heaviest;

      // Double-encoded as width and ink, the same as everything else in these views (§5.9):
      // a thin pale line and a thick dark one are separable even where hundreds overlap.
      ctx.lineWidth = Math.max(maxWidth * t, 0.4 * dpr);
      ctx.strokeStyle = `rgba(0,0,0,${(0.08 + 0.5 * t).toFixed(3)})`;
      ctx.beginPath();
      ctx.moveTo(x, y);
      ctx.lineTo(s(firm.position.x), s(firm.position.y));
      ctx.stroke();
    }
  }
}
