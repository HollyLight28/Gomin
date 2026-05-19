import sys

file_path = './TMessagesProj/src/main/AndroidManifest.xml'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@mipmap/icon_launcher_cherry"')

with open(file_path, 'w') as f:
    f.write(content)
