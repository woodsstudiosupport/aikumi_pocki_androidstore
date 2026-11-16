# Aikumi Android — Living Plan

Status: planning continues; the user authorised the startup/onboarding/chat foundation on 2026-11-16. Final AAB packaging follows planning finalisation.
Last updated: 2026-11-16.

## Identity

- App name: Aikumi.
- Developer: Woods Studio.
- Android application ID: `com.woodsstudio.android.aikumi.pocket`.
- Repository: `woodsstudiosupport/aikumi_pocki_androidstore`, branch `main`.
- Conversational AI provider: OpenAI. Exact text/voice interaction design and model selection remain to be planned.

## Workflow

Record decisions and specifications in this repository as planning progresses. Keep confirmed requirements separate from proposals and unresolved decisions. Build the Android shell/AAB around the finalised plan. No implementation or production-ready release is implied by this document.

## Implementation references

- [Onboarding and chat design](ONBOARDING_AND_CHAT.md)
- [Personality core](PERSONALITY_CORE.md)
- [Foundation implementation and validation](FOUNDATION.md)

## Confirmed requirements

### Startup and appearance

- Black startup background.
- Woods Studio logo using the user's linked brand asset; exact approved asset to be recorded.
- Rotating purple-gradient loading spinner during repository checking and retrieval.
- Check the repository on app startup and retrieve the required compatible resources.
- GitHub infrastructure is not named in normal user-facing startup/interface text. Required privacy disclosures must remain accurate.

### App and remote content split

- Keep base UI, startup display, essential runtime data, resource retrieval, and required Android integration in the installed app.
- Load as much additional UI, assets, personality core, compatible interpreted code, and configuration as possible from this repository.
- Remote native Android executable replacement is outside the design: native functionality changes require a new Play-delivered AAB.
- Public Play Store distribution is a later milestone.

### Closed-testing key arrangement

- User explicitly requires the OpenAI key to reside in this repository for closed testing.
- Woods Studio backend services will not hold that testing key.
- A later AAB will change key handling before public distribution; the replacement architecture remains to be specified.
- This arrangement exposes any client-retrieved key to extraction, even with a private repository. It is a testing compromise, not a secret-preserving architecture.
- No live credential has been provided or added by this planning change.

### Personal traits

- Personal traits are stored only in app-private local storage, never in GitHub or a remote traits database.
- Exclude traits from Android cloud backup and device-transfer backup mechanisms.
- The shared personality core comes from the repository; local traits personalise the experience above that core.
- Using selected traits as OpenAI request context transmits those selected details for processing; local-only persistence does not mean wholly on-device inference.

## Proposed implementation details — not finalised

- Versioned release manifest rather than scanning/downloading every repository file on each launch.
- Download changed resources into a staging area; validate integrity and app-version compatibility before activation.
- Cache the last verified release and retain rollback support.
- Bundle the startup logo/spinner so they work before networking.
- On failed first-run retrieval, show a clear retry state; on later failures, use compatible cached content where available.
- Use a dedicated testing OpenAI key and revoke it after testing.

## Decisions still needed

- Main UI, navigation, chat layout, onboarding, settings, and accessibility.
- Voice delivery and further conversational details; base personality is recorded in PERSONALITY_CORE.md.
- Text and voice capabilities, model, audio pipeline, and permissions.
- Trait schema, learning rules, user controls, reset/deletion, and request-context selection.
- Repository visibility and client read authentication, especially for closed-test credentials.
- Whether the app itself needs any repository writes, exactly what it may write, and how that access is constrained. Permission for this assistant to maintain planning files is separate from app runtime write access. Traits must never be written remotely.
- Key file format and retrieval path; do not invent or add live credentials during planning.
- Release manifest, verification/signing, compatibility, caching, timeouts, and failure behaviour.
- Final base-UI versus remote-UI boundary and Android platform support.
- Public-release credential migration, release review, signing, and AAB delivery process.

## Decision history

- 2026-11-16: Established app identity and remote-first architecture.
- 2026-11-16: User clarified that personal traits persist only locally.
- 2026-11-16: User confirmed repository-held OpenAI key specifically for closed testing, with later AAB migration for public release.
- 2026-11-16: User designated this repository for ongoing planning files; AAB construction follows planning finalisation.

- 2026-11-16: User approved the Kotlin shell plus remotely loaded web interface, onboarding, chat styling, and beginning foundation implementation. Canva is the chosen provider for new art assets.
