Android IDVoice sample application overview
===========================================

**The application code used VoiceSDK 3.0 and it's not compatible with VoiceSDK 2.13 and 2.14.**

This sample application is intended to demonstrate the VoiceSDK voice verification 
and anti-spoofing capabilities:

* It provides a simple text-dependent enrollment and voice verification scenario: user enrolls with three entries of their voice to create a strong voiceprint. Then they can use the created voiceprint for voice verification. By enrolling for the second time user overwrites their voiceprint.

* It provides simple text-independent enrollment and verification scenario: user enrolls with 10 seconds of their speech to create a strong voiceprint. By enrolling for the second time user overwrites their voiceprint.

* It provides text-independent continuous verification scenario without anti-spoofing check.

* It implements simple logging for voice verification results and anti-spoofing checks along with audio records. Collected logs can be sent via email or instant messaging app.

The application also demonstrates the way to validate user audio input by performing speech endpoint detection and estimating net speech length.

**All source code contains commentary that should help developers.**

Tips for voice verification process
-----------------------------------

- **Do enrolls in quiet conditions. Speak clearly and without changing the intonation of your usual voice.**

Please refer to [IDVoice quick start guide](https://docs.idrnd.net/voice/#idvoice-speaker-verification), [IDLive quick start guide](https://docs.idrnd.net/voice/#idlive-voice-anti-spoofing) and [signal validation guide](https://docs.idrnd.net/voice/#signal-validation-utilities) in order to get more detailed information and best practicies for the listed capabilities.

Developer tips
--------------

- This repository does not contain VoiceSDK distribution itself. Please copy the `java/voicesdk-aar-full-release.aar` file from the IDVoice + IDLive Android package received from ID R&D to the `voicesdk/` folder in order to be able to build and run the application.
- If you prefer not to use one of the voice verification methods, you can simply remove the corresponding folder from the AAR's `voicesdk/voicesdk.aar/assets/verify_init_data/` folder (text-independent - `TI-mic`, text-dependent - `TD-mic`)
- For *x86* and *x86_64* emulator builds poor performance is expected, even with KVM enabled. Also, as QEMU emulation is imperfect, you can see occasional crashes. Please use newer images to mitigate both of these problems.
- It is strongly advised to enable [ABI splits](https://developer.android.com/studio/build/configure-apk-splits) so that only relevant of AAR native libraries is present in each apk, greatly reducing their size.
