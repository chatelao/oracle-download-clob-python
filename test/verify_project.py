import os
import sys

def verify():
    required_files = [
        "CONCEPT.md",
        "DESIGN.md",
        "GEMINI.md",
        "TOP_ARCHITECTURE.puml",
        "src/install.sh",
        "test/install.sh"
    ]
    required_dirs = ["src", "test", "specification", "build"]

    missing = []
    for d in required_dirs:
        if not os.path.isdir(d):
            missing.append(f"Directory missing: {d}")

    for f in required_files:
        if not os.path.isfile(f):
            missing.append(f"File missing: {f}")

    if missing:
        print("\n".join(missing))
        sys.exit(1)
    else:
        print("Project structure and documentation files verified successfully.")

if __name__ == "__main__":
    verify()
