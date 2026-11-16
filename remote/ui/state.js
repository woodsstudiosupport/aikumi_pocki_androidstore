/* Shared normalisation for the interface, preview, and tests. */
(function (scope) {
  'use strict';
  const clean = (value, limit) => typeof value === 'string' ? value.trim().slice(0, limit) : '';
  function normalise(value) {
    const s = value && typeof value === 'object' ? value : {};
    return { schemaVersion: 1, onboardingComplete: s.onboardingComplete === true,
      onboardingStep: Number.isInteger(s.onboardingStep) ? Math.min(2, Math.max(0, s.onboardingStep)) : 0,
      name: clean(s.name, 60), interests: clean(s.interests, 300), saveHistory: s.saveHistory === true };
  }
  const api = { normalise };
  if (typeof module !== 'undefined') module.exports = api;
  scope.AikumiState = api;
})(typeof window === 'undefined' ? globalThis : window);
