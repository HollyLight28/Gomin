import os
import re
import shutil

# The aliases and their respective backgrounds from LauncherIconController
icons = {
    "CG_Icon_Dark": "icon_background_dark",
    "CG_Icon_Dark_Bra": "icon_background_dark",
    "CG_Icon_White_Cherry": "icon_background_white",
    "CG_Icon_White_Cherry_Bra": "icon_background_white",
    "CG_Icon_Laguna": "icon_background_laguna",
    "CG_Icon_Aqua": "icon_background_aqua",
    "CG_Icon_Green": "icon_background_green",
    "CG_Icon_Lavanda": "icon_background_lavanda",
    "CG_Icon_Violet_Sunset": "icon_background_violet_sunset",
    "CG_Icon_Violet_Sunset_Bra": "icon_background_violet_sunset",
    "CG_Icon_Sunset": "icon_background_sunset",
    "CG_Icon_Sunrise": "icon_background_sunrise",
    "CG_Icon_Turbo": "icon_5_background_sa",
    "CG_Icon_Night": "icon_2_background_sa",  # Night uses mipmap but we'll refer to it correctly
    "CG_Icon_Dark_NY": "icon_background_dark_ny",
}

res_dir = "TMessagesProj/src/main/res-cherrygram"
anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
os.makedirs(anydpi_dir, exist_ok=True)

# 1. Create adaptive icon XMLs
for alias, bg in icons.items():
    icon_name = alias.lower()

    # Handle Night's mipmap background
    bg_ref = f"@mipmap/{bg}" if bg == "icon_2_background_sa" else f"@drawable/{bg}"

    xml_content = f"""<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="{bg_ref}" />
    <foreground android:drawable="@drawable/icon_foreground_gomin" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, f"ic_{icon_name}.xml"), "w") as f:
        f.write(xml_content)

# 2. Copy fallback PNGs for older Android versions
densities = ["hdpi", "mdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
for density in densities:
    src_png = os.path.join(res_dir, f"mipmap-{density}", "icon_launcher_cherry.png")
    if not os.path.exists(src_png):
        # Check standard res folder if not in res-cherrygram
        src_png = (
            f"TMessagesProj/src/main/res/mipmap-{density}/icon_launcher_cherry.png"
        )

    if os.path.exists(src_png):
        dest_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(dest_dir, exist_ok=True)
        for alias in icons.keys():
            icon_name = alias.lower()
            shutil.copy(src_png, os.path.join(dest_dir, f"ic_{icon_name}.png"))

# 3. Update AndroidManifest.xml
manifest_path = "TMessagesProj/src/main/AndroidManifest.xml"
with open(manifest_path, "r", encoding="utf-8") as f:
    manifest_content = f.read()

# Replace the icon for each specific alias
for alias in icons.keys():
    icon_name = alias.lower()
    # We need to find the activity-alias with android:name="ua.gomin.messenger.{alias}"
    # and replace its android:icon="@mipmap/icon_launcher_cherry" with android:icon="@mipmap/ic_{icon_name}"

    pattern = (
        r'(<activity-alias[^>]*?android:name="ua\.gomin\.messenger\.'
        + alias
        + r'"[^>]*?)android:icon="@mipmap/icon_launcher_cherry"([^>]*?>)'
    )
    replacement = r'\1android:icon="@mipmap/ic_' + icon_name + r'"\2'

    manifest_content = re.sub(pattern, replacement, manifest_content, flags=re.DOTALL)

with open(manifest_path, "w", encoding="utf-8") as f:
    f.write(manifest_content)

print("Icons generated and AndroidManifest.xml updated successfully.")
