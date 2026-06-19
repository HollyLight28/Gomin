# Gemini Live API — WebSocket Reference (Official Google Docs)

**Source:** https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket  
**Saved:** 2026-06-19  
**API Version:** v1beta  

---

## Overview

The Gemini Live API allows for real-time, bidirectional interaction with Gemini models,
supporting audio, video, and text inputs and native audio outputs.

Key concepts:
- **WebSocket Endpoint**: The specific URL to connect to.
- **Message Format**: All communication is done via JSON messages conforming to
  `BidiGenerateContentClientMessage` and `BidiGenerateContentServerMessage` structures.
- **Session Management**: You are responsible for maintaining the WebSocket connection.

---

## Authentication

Authentication is handled by including your API key as a query parameter in the WebSocket URL.

### Standard endpoint:
```
wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=YOUR_API_KEY
```

### Ephemeral Tokens endpoint (v1alpha):
```
wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContentConstrained?access_token={short-lived-token}
```

---

## Connecting to the Live API

To start a live session, establish a WebSocket connection to the authenticated endpoint.
The first message sent over the WebSocket **must** be a `BidiGenerateContentSetup` containing
the `config`.

### Python Example:
```python
import asyncio
import websockets
import json

API_KEY = "YOUR_API_KEY"
MODEL_NAME = "gemini-3.1-flash-live-preview"
WS_URL = f"wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key={API_KEY}"

async def connect_and_configure():
    async with websockets.connect(WS_URL) as websocket:
        print("WebSocket Connected")

        # 1. Send the initial configuration
        setup_message = {
            "setup": {
                "model": f"models/{MODEL_NAME}",
                "responseModalities": ["AUDIO"],
                "systemInstruction": {
                    "parts": [{"text": "You are a helpful assistant."}]
                }
            }
        }
        await websocket.send(json.dumps(setup_message))
        print("Configuration sent")

        # Keep the session alive for further interactions
        await asyncio.sleep(3600)

async def main():
    await connect_and_configure()

if __name__ == "__main__":
    asyncio.run(main())
```

### JavaScript Example:
```javascript
const API_KEY = "YOUR_API_KEY";
const MODEL_NAME = "gemini-3.1-flash-live-preview";
const WS_URL = `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=${API_KEY}`;

const websocket = new WebSocket(WS_URL);

websocket.onopen = () => {
  console.log('WebSocket Connected');

  // 1. Send the initial configuration
  const setupMessage = {
    setup: {
      model: `models/${MODEL_NAME}`,
      responseModalities: ['AUDIO'],
      systemInstruction: {
        parts: [{ text: 'You are a helpful assistant.' }]
      }
    }
  };
  websocket.send(JSON.stringify(setupMessage));
  console.log('Configuration sent');
};

websocket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Received:', response);
  // Handle different types of responses here
};

websocket.onerror = (error) => {
  console.error('WebSocket Error:', error);
};

websocket.onclose = () => {
  console.log('WebSocket Closed');
};
```

---

## CRITICAL: Setup Payload Structure

**⚠️ `responseModalities` and `speechConfig` are DIRECT children of `"setup"`,
NOT nested inside `"generationConfig"`!**

### Correct structure (voice mode):
```json
{
  "setup": {
    "model": "models/gemini-3.1-flash-live-preview",
    "responseModalities": ["AUDIO"],
    "speechConfig": {
      "voiceConfig": {
        "prebuiltVoiceConfig": {
          "voiceName": "Puck"
        }
      }
    },
    "systemInstruction": {
      "parts": [{"text": "You are a helpful assistant."}]
    }
  }
}
```

### Correct structure (transcription mode):
```json
{
  "setup": {
    "model": "models/gemini-3.1-flash-live-preview",
    "responseModalities": ["TEXT"],
    "inputAudioTranscription": {}
  }
}
```

### ❌ WRONG (causes silent 30s timeout — server never sends setupComplete):
```json
{
  "setup": {
    "model": "models/gemini-3.1-flash-live-preview",
    "generationConfig": {
      "responseModalities": ["AUDIO"],
      "speechConfig": { ... }
    }
  }
}
```

---

## Sending Text

To send text input, construct a `BidiGenerateContentRealtimeInput` message with the `text` field.

### Python:
```python
async def send_text(websocket, text):
    text_message = {
        "realtimeInput": {
            "text": text
        }
    }
    await websocket.send(json.dumps(text_message))
    print(f"Sent text: {text}")
```

### JavaScript:
```javascript
function sendTextMessage(text) {
  if (websocket.readyState === WebSocket.OPEN) {
    const textMessage = {
      realtimeInput: {
        text: text
      }
    };
    websocket.send(JSON.stringify(textMessage));
    console.log('Text message sent:', text);
  } else {
    console.warn('WebSocket not open.');
  }
}
```

---

## Sending Audio

Audio needs to be sent as raw PCM data (raw 16-bit PCM audio, 16kHz, little-endian).
Construct a `BidiGenerateContentRealtimeInput` message with the audio data.
The `mimeType` is crucial.

### Python:
```python
async def send_audio_chunk(websocket, chunk_bytes):
    import base64
    encoded_data = base64.b64encode(chunk_bytes).decode('utf-8')
    audio_message = {
        "realtimeInput": {
            "audio": {
                "data": encoded_data,
                "mimeType": "audio/pcm;rate=16000"
            }
        }
    }
    await websocket.send(json.dumps(audio_message))
```

### JavaScript:
```javascript
function sendAudioChunk(chunk) {
  if (websocket.readyState === WebSocket.OPEN) {
    const audioMessage = {
      realtimeInput: {
        audio: {
          data: chunk.toString('base64'),
          mimeType: 'audio/pcm;rate=16000'
        }
      }
    };
    websocket.send(JSON.stringify(audioMessage));
  }
}
```

---

## Sending Video

Video frames are sent as individual images (e.g., JPEG or PNG).
Similar to audio, use `realtimeInput` with a `Blob`, specifying the correct `mimeType`.

### Python:
```python
async def send_video_frame(websocket, frame_bytes, mime_type="image/jpeg"):
    import base64
    encoded_data = base64.b64encode(frame_bytes).decode('utf-8')
    video_message = {
        "realtimeInput": {
            "video": {
                "data": encoded_data,
                "mimeType": mime_type
            }
        }
    }
    await websocket.send(json.dumps(video_message))
```

---

## Receiving Responses

The server responds with JSON objects that can contain:
- `setupComplete` — sent after the setup message is accepted
- `serverContent` — contains model output (audio, text, tool calls)

### Response structure:
```json
{
  "serverContent": {
    "modelTurn": {
      "parts": [
        {
          "inlineData": {
            "mimeType": "audio/pcm;rate=24000",
            "data": "<base64-encoded-audio>"
          }
        }
      ]
    }
  }
}
```

### Python handling:
```python
async for message in websocket:
    response = json.loads(message)
    if "setupComplete" in response:
        print("Setup complete!")
    elif "serverContent" in response:
        server_content = response["serverContent"]
        if "modelTurn" in server_content and "parts" in server_content["modelTurn"]:
            for part in server_content["modelTurn"]["parts"]:
                if "inlineData" in part:
                    audio_data_b64 = part["inlineData"]["data"]
                    # Process or play the base64 encoded audio data
```

### JavaScript handling:
```javascript
websocket.onmessage = (event) => {
  const response = JSON.parse(event.data);

  if (response.setupComplete) {
    console.log('Setup complete!');
  }

  if (response.serverContent) {
    const serverContent = response.serverContent;
    if (serverContent.modelTurn?.parts) {
      for (const part of serverContent.modelTurn.parts) {
        if (part.inlineData) {
          const audioData = part.inlineData.data;
          // Process or play audioData
          console.log(`Received audio data (base64 length: ${audioData.length})`);
        }
      }
    }
  }
};
```

---

## Tool Calls (Function Calling)

When the model requests a tool call, the `BidiGenerateContentServerMessage` will contain a
`toolCall` field. You must execute the function locally and send the result back to the
WebSocket using a `BidiGenerateContentToolResponse` message.

---

## Available Models

As of June 2026:
- `gemini-3.1-flash-live-preview` — recommended for real-time audio

---

## Audio Format Reference

| Direction | Sample Rate | Format | Channels |
|-----------|-------------|--------|----------|
| Input (mic → server) | 16000 Hz | PCM 16-bit LE | Mono |
| Output (server → speaker) | 24000 Hz | PCM 16-bit LE | Mono |

---

## Available Voices

Voice names for `prebuiltVoiceConfig.voiceName`:
- Puck
- Charon
- Kore
- Fenrir
- Aoede
- Leda
- Orus
- Zephyr

---

## Links

- [Live API Overview](https://ai.google.dev/gemini-api/docs/live-api)
- [Get Started with WebSockets](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket)
- [WebSockets API Reference](https://ai.google.dev/api/live)
- [Live API Capabilities Guide](https://ai.google.dev/gemini-api/docs/live-guide)
- [GitHub Example App](https://github.com/google-gemini/gemini-live-api-examples/tree/main/gemini-live-ephemeral-tokens-websocket)
