#ifndef SECURE_VALIDATOR_HPP
#define SECURE_VALIDATOR_HPP

#include <jni.h>

namespace secure_validator {
    inline void maybeForceDisconnectOrUpdate(JNIEnv* env, int instanceNum, int reason) {}
    inline bool has_jni_hook(JNIEnv* env) { return false; }
    inline bool has_xhook() { return false; }
    inline bool validate_signature(JNIEnv* env) { return true; }
}

#endif // SECURE_VALIDATOR_HPP