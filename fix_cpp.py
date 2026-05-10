import re

with open("TMessagesProj/jni/TgNetWrapper.cpp", "r") as f:
    content = f.read()

# Replace skCrypt("...") with "..."
content = re.sub(r'skCrypt\("([^"]*)"\)', r'"\1"', content)

# Remove includes
content = re.sub(
    r'#include "security/secure_validator\.hpp"',
    r'// #include "security/secure_validator.hpp"',
    content,
)
content = re.sub(
    r'#include "security/skCrypter\.hpp"',
    r'// #include "security/skCrypter.hpp"',
    content,
)

# Remove secure_validator calls
content = re.sub(
    r"secure_validator::[^;]+;", r"/* removed secure_validator call */", content
)
content = re.sub(r"if \(secure_validator::[^)]+\) \{", r"if (false) {", content)

with open("TMessagesProj/jni/TgNetWrapper.cpp", "w") as f:
    f.write(content)

with open("TMessagesProj/jni/jni.c", "r") as f:
    content = f.read()
    content = re.sub(
        r'#include "colorado/colorado\.h"',
        r'// #include "colorado/colorado.h"',
        content,
    )
with open("TMessagesProj/jni/jni.c", "w") as f:
    f.write(content)
