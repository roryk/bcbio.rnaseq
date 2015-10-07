#!/bin/bash
if [ -d "$HOME/.linuxbrew" ]; then
    echo "homebrew found, skipping installation."
else
    echo -ne '\n' | ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/linuxbrew/go/install)"
fi
