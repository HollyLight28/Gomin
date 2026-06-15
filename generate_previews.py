import os
import re

def scale_path(path_data, tx, ty, sx, sy):
    # Tokenize path data into commands and numbers
    tokens = re.findall(r'([a-zA-Z]|-?\d+(?:\.\d+)?)', path_data)
    
    new_tokens = []
    current_cmd = ''
    coord_index = 0
    
    for token in tokens:
        if token.isalpha():
            current_cmd = token
            coord_index = 0
            new_tokens.append(token)
        else:
            val = float(token)
            is_x = (coord_index % 2 == 0)
            
            if current_cmd == 'M' and coord_index < 2:
                # First absolute move command (M)
                if is_x:
                    new_val = tx + val * sx
                else:
                    new_val = ty - val * sy
            else:
                # Relative commands (m, c, l)
                if is_x:
                    new_val = val * sx
                else:
                    new_val = -val * sy
            
            # Format to 1 decimal place to keep SVG size reasonable
            # If it's a whole number, format as int to save space
            formatted = f"{new_val:.1f}"
            if formatted.endswith(".0"):
                formatted = formatted[:-2]
            new_tokens.append(formatted)
            coord_index += 1
            
    return " ".join(new_tokens)

def create_previews_flattened():
    svg_path = "gomin.svg"
    with open(svg_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Extract the path d attribute content
    path_d_match = re.search(r'd="([^"]*)"', content)
    if not path_d_match:
        print("Error: Could not find path data in gomin.svg")
        return
    
    raw_path_d = path_d_match.group(1).strip()

    options = [
        {"tx": 0.0, "ty": 1024.0, "sx": 0.10, "sy": 0.10, "desc": "Оригінал (Замала пташка, scale 0.10)"},
        {"tx": -154.0, "ty": 1178.0, "sx": 0.13, "sy": 0.13, "desc": "30% Більша (Математичний центр, scale 0.13)"},
        {"tx": -205.0, "ty": 1229.0, "sx": 0.14, "sy": 0.14, "desc": "40% Більша (Математичний центр, scale 0.14)"},
        {"tx": -256.0, "ty": 1280.0, "sx": 0.15, "sy": 0.15, "desc": "50% Більша (Математичний центр, scale 0.15)"},
        {"tx": -305.0, "ty": 1279.0, "sx": 0.14, "sy": 0.14, "desc": "40% Більша (Зміщена, хвіст обрізається, scale 0.14)"},
        {"tx": -356.0, "ty": 1330.0, "sx": 0.15, "sy": 0.15, "desc": "50% Більша (Зміщена, хвіст обрізається, scale 0.15)"},
        {"tx": -458.0, "ty": 1432.0, "sx": 0.17, "sy": 0.17, "desc": "70% Більша (Гігантська, хвіст обрізається, scale 0.17)"}
    ]

    html_content = """<!DOCTYPE html>
<html>
<head>
    <title>Gomin SVG Scaling Preview (Flattened)</title>
    <style>
        body {
            background-color: #121212;
            color: #ffffff;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 40px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
            margin-top: 30px;
        }
        .card {
            background-color: #1e1e1e;
            border-radius: 12px;
            padding: 20px;
            text-align: center;
            box-shadow: 0 4px 15px rgba(0,0,0,0.3);
            border: 1px solid #333;
        }
        .icon-wrapper {
            background-color: #1a1a1a;
            width: 200px;
            height: 200px;
            margin: 20px auto;
            border-radius: 50%; /* Clip like adaptive icon circular mask */
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
            border: 2px solid #555;
            position: relative;
        }
        .icon-wrapper svg {
            width: 100%;
            height: 100%;
        }
        h3 {
            margin: 10px 0 5px 0;
            font-size: 16px;
        }
        p {
            font-size: 12px;
            color: #aaaaaa;
            margin: 0;
        }
        .badge {
            background-color: #007aff;
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 11px;
            display: inline-block;
            margin-bottom: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Gomin SVG Scaling & Centering (FLATTENED PATHS)</h1>
        <p style="font-size: 14px; color: #888;">Нижче показано, як виглядатиме нова пташка при різному масштабуванні та центруванні. Усі координати перераховані безпосередньо у шляху (без використання group transforms), що гарантує 100% точне відображення у браузері та на пристрої.</p>
        <div class="grid">
"""
    for opt in options:
        # Scale and flatten the path
        flattened_path = scale_path(raw_path_d, opt["tx"], opt["ty"], opt["sx"], opt["sy"])
        
        svg_inline = f"""<svg version="1.0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
<path d="{flattened_path}" fill="#ffffff" stroke="none" />
</svg>"""

        html_content += f"""
            <div class="card">
                <span class="badge">Масштаб {int(opt['sx']*1000)}</span>
                <div class="icon-wrapper">
                    {svg_inline}
                </div>
                <h3>{opt['desc']}</h3>
                <p>Трансляція: X={opt['tx']}, Y={opt['ty']}</p>
                <p>Масштаб: {opt['sx']}</p>
            </div>
"""
    html_content += """
        </div>
    </div>
</body>
</html>
"""
    with open("preview.html", "w", encoding="utf-8") as f:
        f.write(html_content)
    print("SUCCESS")

if __name__ == "__main__":
    create_previews_flattened()
