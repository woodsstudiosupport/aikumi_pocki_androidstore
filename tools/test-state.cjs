const test = require('node:test');
const assert = require('node:assert/strict');
const { normalise } = require('../remote/ui/state.js');
test('first install does not assume consent or completion', () => {
  assert.deepEqual(normalise(null), {schemaVersion:1,onboardingComplete:false,onboardingStep:0,name:'',interests:'',saveHistory:false});
});
test('partial onboarding resumes without declaring completion', () => {
  assert.equal(normalise({onboardingStep:1,name:'  Flynn  '}).onboardingStep,1);
  assert.equal(normalise({onboardingStep:1}).onboardingComplete,false);
  assert.equal(normalise({name:'  Flynn  '}).name,'Flynn');
});
test('untrusted profile values are bounded and consent requires booleans', () => {
  const state=normalise({name:'x'.repeat(100),interests:'x'.repeat(1000),saveHistory:'true',onboardingComplete:1,onboardingStep:99,secret:'excluded'});
  assert.equal(state.name.length,60); assert.equal(state.interests.length,300);
  assert.equal(state.saveHistory,false); assert.equal(state.onboardingComplete,false);
  assert.equal(state.onboardingStep,2); assert.ok(!('secret' in state));
});
