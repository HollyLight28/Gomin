import os
from PIL import Image


def pad_image(img_path, scale_factor=0.66):
    try:
        img = Image.open(img_path).convert("RGBA")
        width, height = img.size

        # Calculate new size
        new_width = int(width * scale_factor)
        new_height = int(height * scale_factor)

        # Resize image
        resized_img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

        # Create a new transparent image of the original size
        new_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))

        # Paste resized image into the center
        offset_x = (width - new_width) // 2
        offset_y = (height - new_height) // 2
        new_img.paste(resized_img, (offset_x, offset_y), resized_img)

        # Save back
        new_img.save(img_path)
        print(f"Padded {img_path}")
    except Exception as e:
        print(f"Failed to pad {img_path}: {e}")


# Find all relevant images
targets = ["ic_launcher_foreground.png", "cg_logo_bird.png"]

for root, dirs, files in os.walk("TMessagesProj/src/main/res-cherrygram"):
    for file in files:
        if file in targets:
            pad_image(os.path.join(root, file))
