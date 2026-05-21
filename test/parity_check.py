import subprocess
import sys

import os

def run_command(cmd):
    env = os.environ.copy()
    env["PYTHONPATH"] = env.get("PYTHONPATH", "") + ":."
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True, env=env)
    # Some tools might output help to stderr (like Picocli when required options are missing)
    combined_output = result.stdout + result.stderr
    return result.returncode, combined_output, result.stderr

def test_help():
    print("Testing --help parity...")
    python_cmd = ["python3", "src/cli.py", "--help"]
    java_cmd = ["mvn", "-q", "compile", "exec:java", "-Dexec.mainClass=com.oracle.tool.CliCommand", "-Dexec.args=--help"]

    p_rc, p_out, p_err = run_command(python_cmd)
    j_rc, j_out, j_err = run_command(java_cmd)

    if p_rc != 0 or j_rc != 0:
        print(f"Error: Help command failed. Python RC: {p_rc}, Java RC: {j_rc}")
        return False

    # Check for key keywords in both outputs
    keywords = ["download", "upload", "clob"]
    for kw in keywords:
        if kw.lower() not in p_out.lower():
            print(f"Keyword '{kw}' missing from Python help output")
            return False
        if kw.lower() not in j_out.lower():
            print(f"Keyword '{kw}' missing from Java help output")
            return False

    print("Main help parity: OK")

    print("Testing 'download --help' parity...")
    python_cmd = ["python3", "src/cli.py", "download", "--help"]
    java_cmd = ["mvn", "-q", "compile", "exec:java", "-Dexec.mainClass=com.oracle.tool.CliCommand", "-Dexec.args=download --help"]

    p_rc, p_out, p_err = run_command(python_cmd)
    j_rc, j_out, j_err = run_command(java_cmd)

    keywords = ["csv-path", "output-dir", "dsn", "user", "password", "table", "id-column", "clob-column"]
    for kw in keywords:
        if kw.lower() not in p_out.lower():
            print(f"Keyword '{kw}' missing from Python download help output")
            return False
        if kw.lower() not in j_out.lower():
            print(f"Keyword '{kw}' missing from Java download help output")
            return False

    print("Download help parity: OK")

    print("Testing 'upload --help' parity...")
    python_cmd = ["python3", "src/cli.py", "upload", "--help"]
    java_cmd = ["mvn", "-q", "compile", "exec:java", "-Dexec.mainClass=com.oracle.tool.CliCommand", "-Dexec.args=upload --help"]

    p_rc, p_out, p_err = run_command(python_cmd)
    j_rc, j_out, j_err = run_command(java_cmd)

    keywords = ["csv-path", "input-dir", "dsn", "user", "password", "table", "id-column", "clob-column", "id-as-regex"]
    for kw in keywords:
        if kw.lower() not in p_out.lower():
            print(f"Keyword '{kw}' missing from Python upload help output")
            return False
        if kw.lower() not in j_out.lower():
            print(f"Keyword '{kw}' missing from Java upload help output")
            return False

    print("Upload help parity: OK")
    return True

def test_invalid_args():
    print("Testing invalid arguments parity...")
    python_cmd = ["python3", "src/cli.py", "download", "--invalid-arg"]
    java_cmd = ["mvn", "-q", "compile", "exec:java", "-Dexec.mainClass=com.oracle.tool.CliCommand", "-Dexec.args=download --invalid-arg"]

    p_rc, p_out, p_err = run_command(python_cmd)
    j_rc, j_out, j_err = run_command(java_cmd)

    if p_rc == 0 or j_rc == 0:
        print(f"Error: Invalid arguments should fail. Python RC: {p_rc}, Java RC: {j_rc}")
        return False

    print("Invalid arguments parity: OK")
    return True

def main():
    success = True
    success &= test_help()
    success &= test_invalid_args()

    if success:
        print("All parity checks passed (Basic).")
        sys.exit(0)
    else:
        print("Some parity checks failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
