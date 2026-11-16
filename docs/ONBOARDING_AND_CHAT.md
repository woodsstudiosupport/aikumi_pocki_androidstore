# Aikumi — Onboarding and Chat

Status: design agreed by the user on 2026-11-16; implementation pending.
Related: [Living plan](PLANNING.md), [Personality core](PERSONALITY_CORE.md).

## Approved implementation direction

Kotlin Android shell with remotely updated HTML/CSS/JavaScript interface. First code milestone: launch, check repository, retrieve and verify compatible resources, then display the interface. OpenAI chat and local traits follow. Final AAB packaging remains after planning finalisation.

## Entry flow

After the first successful sync, display onboarding before chat. Save completion locally. Subsequent launches check/sync then open chat without repeating onboarding. An interrupted onboarding resumes without marking completion prematurely. Resource failure handling follows the loader design still to be finalised.

## Onboarding

Three short screens with back navigation and optional profile fields:

1. “A little company, wherever you are.” Introduce Aikumi clearly as an AI companion, with her artwork and a soft purple glow. Approved artwork asset remains to be supplied or selected.
2. “What should I call you?” Optional name and optional interests, forming the first locally stored traits. Allow continuing without either.
3. “Make yourself comfortable.” Explain local personalisation, that messages and selected trait context are sent to OpenAI, and the choice to save chat history. Finish with “Meet Aikumi.” The history choice must be explicit; the default and retention details remain to be planned.

Save personal profile data only in app-private local storage; exclude from cloud/device-transfer backups. No traits or chat history are committed to GitHub. History retention and deletion controls remain to be specified.

## Chat appearance

| Element | Agreed direction |
| --- | --- |
| Background | Near-black with subtle plum tint |
| Header | Small Aikumi portrait, her name, menu |
| Aikumi messages | Soft charcoal rounded bubbles |
| User messages | Muted purple rounded bubbles |
| Typography | Warm off-white, readable sizing and generous spacing |
| Composer | Rounded multiline input, space for attachments, microphone, send |
| Waiting state | Gentle animated dots |
| Motion | Short fades and reduced-motion support |

Keep the conversation spacious and uncluttered. Hide timestamps until a message is tapped; make them accessible to assistive technology. Place settings behind the header menu. Attachment space is a layout provision; attachment types, processing, and availability are not yet agreed. Show actionable controls only when their functions are implemented.

## First conversation

Suggested approved opening: “Hey… what should we talk about first?”

If optional interests were supplied, Aikumi may naturally open with one instead. Follow the personality core: usually one to three short sentences, no emojis, soft and curious rather than repetitive reassurance.

## Permissions

Request microphone permission when voice is first tapped. Request notification permission when the user enables optional check-ins. Do not request these during onboarding without their corresponding action. Notification scheduling and voice interaction details remain to be planned.

## Remaining design details

- Approved logo/portrait assets and final visual tokens.
- Chat history default, retention, deletion, and behaviour when saving is disabled.
- Keyboard, scrolling, sending, streaming, cancellation, retries, and error states.
- Voice mode and attachment support.
- Settings contents and optional check-in controls.
- Accessibility contrast, text scaling, focus, labels, and touch targets to verify during implementation.
