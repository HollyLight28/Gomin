import re
import os

def generate():
    res_dir = "TMessagesProj/src/main/res-cherrygram/drawable"
    
    # Read white bird paths
    with open(os.path.join(res_dir, "icon_foreground_gomin.xml"), "r", encoding="utf-8") as f:
        white_content = f.read()
    
    # Find all path elements inside group
    paths_match = re.findall(r'(<path\s+[^>]*android:fillColor="[^"]*"[^>]*android:pathData="[^"]*"[^>]*/>)', white_content)
    
    # Clean paths for scale group
    white_bird_paths = ""
    for path in paths_match:
        # Enforce white color
        path_clean = re.sub(r'android:fillColor="[^"]*"', 'android:fillColor="#FFFFFFFF"', path)
        white_bird_paths += "        " + path_clean + "\n"
        
    # Read black bird paths
    black_bird_paths = ""
    for path in paths_match:
        # Enforce black color
        path_clean = re.sub(r'android:fillColor="[^"]*"', 'android:fillColor="#FF121212"', path)
        black_bird_paths += "        " + path_clean + "\n"

    # 1. Gomin Default (Blue Telegram)
    default_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- Blue Telegram Circle Disc in Center -->
    <path
        android:fillColor="#FF2AABEE"
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0" />
    <!-- White Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{white_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_default.xml"), "w", encoding="utf-8") as f:
        f.write(default_xml)

    # 2. Gomin Dark
    dark_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- Dark Circle Disc in Center -->
    <path
        android:fillColor="#FF121212"
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0" />
    <!-- White Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{white_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_dark.xml"), "w", encoding="utf-8") as f:
        f.write(dark_xml)

    # 3. Gomin White
    white_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- White Circle Disc in Center -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0" />
    <!-- Black Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{black_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_white.xml"), "w", encoding="utf-8") as f:
        f.write(white_xml)

    # 4. Gomin Aqua
    aqua_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- Aqua Circle Disc in Center -->
    <path
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="860"
                android:startY="136"
                android:endX="170"
                android:endY="894">
                <item
                    android:color="#FF2AEA7B"
                    android:offset="0"/>
                <item
                    android:color="#FF00C4EE"
                    android:offset="0.52"/>
                <item
                    android:color="#FF0B94FE"
                    android:offset="1"/>
            </gradient>
        </aapt:attr>
    </path>
    <!-- White Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{white_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_aqua.xml"), "w", encoding="utf-8") as f:
        f.write(aqua_xml)

    # 5. Gomin Lavanda
    lavanda_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- Lavanda Circle Disc in Center -->
    <path
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="1024"
                android:endX="1024"
                android:endY="0">
                <item
                    android:color="#FF54305E"
                    android:offset="0.11"/>
                <item
                    android:color="#FF673A6E"
                    android:offset="0.33"/>
                <item
                    android:color="#FFA75EA7"
                    android:offset="1"/>
            </gradient>
        </aapt:attr>
    </path>
    <!-- White Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{white_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_lavanda.xml"), "w", encoding="utf-8") as f:
        f.write(lavanda_xml)

    # 6. Gomin Sunset (OUN-UPA)
    sunset_xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <!-- OUN-UPA Circle Disc in Center -->
    <path
        android:pathData="M 512, 512 m -300, 0 a 300,300 0 1,0 600,0 a 300,300 0 1,0 -600,0">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="512"
                android:startY="212"
                android:endX="512"
                android:endY="812">
                <item
                    android:color="#FF1A0000"
                    android:offset="0"/>
                <item
                    android:color="#FF000000"
                    android:offset="0.4"/>
                <item
                    android:color="#FFB71C1C"
                    android:offset="1.0"/>
            </gradient>
        </aapt:attr>
    </path>
    <!-- White Bird -->
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.52"
        android:scaleY="0.52">
{white_bird_paths}    </group>
</vector>
"""
    with open(os.path.join(res_dir, "icon_foreground_gomin_sunset.xml"), "w", encoding="utf-8") as f:
        f.write(sunset_xml)

    print("SUCCESS")

if __name__ == "__main__":
    generate()
