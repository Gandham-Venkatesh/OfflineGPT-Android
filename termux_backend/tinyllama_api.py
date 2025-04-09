from fastapi import FastAPI
from pydantic import BaseModel
import subprocess
import signal

app = FastAPI()

MODEL_PATH = "./models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
LLAMA_CLI_PATH = "./build/bin/llama-cli"

class PromptRequest(BaseModel):
    prompt: str
    max_tokens: int = 100

@app.post("/generate")
async def generate_text(prompt_request: PromptRequest):
    prompt = prompt_request.prompt
    max_tokens = prompt_request.max_tokens

    # Dynamic timeout based on token count
    if max_tokens <= 100:
        timeout = 20  # app = 30s
    elif max_tokens <= 300:
        timeout = 45  # app = 60s
    else:
        timeout = 120  # app = 150s

    print(f"[INFO] Running llama-cli with {max_tokens} tokens and timeout {timeout}s...")

    try:
        # Run llama-cli
        process = subprocess.Popen(
            [LLAMA_CLI_PATH, "-m", MODEL_PATH, "-p", prompt, "--n-predict", str(max_tokens)],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        try:
            stdout, stderr = process.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            print("[WARNING] Timeout reached! Killing the process...")
            process.kill()
            stdout, stderr = process.communicate()

        output = stdout.strip()

        # Default fallback
        response = "Assistant response not found."

        # Extract assistant response
        if "<|assistant|>" in output:
            after_assistant = output.split("<|assistant|>", 1)[-1]
            lines = after_assistant.strip().splitlines()

            final_lines = []
            for line in lines:
                if line.strip() == ">":
                    break
                final_lines.append(line)

            response = "\n".join(final_lines).strip()

        return {"response": response}

    except Exception as e:
        print(f"[ERROR] Exception occurred: {str(e)}")
        return {"error": str(e)}

@app.get("/")
async def root():
    return {"message": "TinyLLaMA API is running!"}
