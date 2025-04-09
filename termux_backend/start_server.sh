#!/data/data/com.termux/files/usr/bin/bash

echo "🔁 Logging into Ubuntu..."
proot-distro login ubuntu << 'EOF'
cd ~

echo "💡 Activating Python virtual environment..."
source venv/bin/activate

echo "📁 Changing to llama.cpp directory..."
cd llama.cpp

echo "🚀 Starting the FastAPI server..."
uvicorn tinyllama_api:app --host 0.0.0.0 --port 8000
EOF
