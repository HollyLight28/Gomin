from PIL import Image, ImageOps
import os

sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

icon_name = 'icon_launcher_cherry.png'
base_path = 'TMessagesProj/src/main/res/mipmap-{size}'

try:
    img = Image.open('20260507_090635.png')
    
    # ensure it is a square
    width, height = img.size
    if width != height:
        size = min(width, height)
        left = (width - size) / 2
        top = (height - size) / 2
        right = (width + size) / 2
        bottom = (height + size) / 2
        img = img.crop((left, top, right, bottom))
        
    # The "safe zone" for Android icons is ~66% of the icon size.
    # We will resize the logo to 70% and pad the rest with black.
    
    for name, size in sizes.items():
        # The logo inside the icon (safe zone ~66%)
        inner_size = int(size * 0.66)
        padding = (size - inner_size) // 2
        
        resized_img = img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
        
        # Create a new black image of full size
        final_img = Image.new('RGB', (size, size), (0, 0, 0))
        final_img.paste(resized_img, (padding, padding))
        
        # Save to the specific mipmap folder in res-cherrygram
        dir_path = f"TMessagesProj/src/main/res-cherrygram/mipmap-{name}"
        if os.path.exists(dir_path):
            # Save legacy icon (square with logo inside)
            file_path = os.path.join(dir_path, 'ic_launcher.png')
            final_img.save(file_path)
            
            # Save round icon (same image, just explicitly named for round requests)
            round_file_path = os.path.join(dir_path, 'ic_launcher_round.png')
            final_img.save(round_file_path)
            
            print(f"Saved {file_path} and round variant (size: {size}x{size}) with padding")

    print("Icon generation completed with safe-zone padding.")
except Exception as e:
    print(f"An error occurred: {e}")
