#!/bin/bash
if brew --version; then
    echo "homebrew found, skipping installation."
else
    echo -ne '\n' | ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/linuxbrew/go/install)"
fi
