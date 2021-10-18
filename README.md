# Android IDVoice sample application

**The application was tested with VoiceSDK 3.2.4.**

This sample application is intended to demonstrate the VoiceSDK voice verification and anti-spoofing
capabilities:

- It provides a simple text-dependent enrollment and voice verification scenario: user enrolls with
  three entries of their voice to create a strong voiceprint. Then they can use the created
  voiceprint for voice verification. By enrolling for the second time user overwrites their
  voiceprint.

- It provides simple text-independent enrollment and verification scenario: user enrolls with 10
  seconds of their speech to create a strong voiceprint. By enrolling for the second time user
  overwrites their voiceprint.

The application also demonstrates the way to validate user audio input by performing speech endpoint
detection and estimating net speech length.

**All source code contains commentary that should help developers.**

## Tips for voice verification process

- **Do enrolls in quiet conditions. Speak clearly and without changing the intonation of your usual
  voice.**

Please refer to [IDVoice quick start guide][1], [IDLive quick start guide][2] and [signal validation
guide][3] in order to get more detailed information and best practices for the listed capabilities.

## Developer tips

- This repository does not contain VoiceSDK distribution itself. Please copy the
  `java/voicesdk-aar-full-release.aar` file from the IDVoice + IDLive Android package received from
  ID R&D to the `voicesdk/` folder in order to be able to build and run the application.

- If you prefer not to use one of the voice verification methods, you can simply remove the
  corresponding folder from the AAR's `voicesdk/voicesdk.aar/assets/verify_init_data/` folder
  (text-independent - `TI-mic`, text-dependent - `TD-mic`).

- For _x86_ and _x86_64_ emulator builds poor performance is expected, even with KVM enabled. Also,
  as QEMU emulation is imperfect, you can see occasional crashes. Please use newer images to
  mitigate both of these problems.

- It is strongly advised to enable [ABI splits][4] so that only relevant of AAR native libraries is
  present in each apk, greatly reducing their size.

- You can find:
  - initialization of VoiceSDK engines in `SharedViewModel.kt`;
  - using of them in `PhraseEnrollerViewModel.kt`, `EnrollerViewModel.kt` and `VerifierViewModel.kt`.
  
- Be careful:
  - all VoiceSDK classes that inherit from `VoiceSdkNativePeer` (most VoiceSDK classes) must be closed after using.

[1]: https://docs.idrnd.net/voice/#idvoice-speaker-verification
[2]: https://docs.idrnd.net/voice/#idlive-voice-anti-spoofing
[3]: https://docs.idrnd.net/voice/#signal-validation-utilities
[4]: https://developer.android.com/studio/build/configure-apk-splits
