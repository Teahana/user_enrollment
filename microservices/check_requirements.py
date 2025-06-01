import subprocess
import sys
import importlib

REQUIRED_PACKAGES = {
    "flask": "flask",
    "mysql-connector-python": "mysql.connector",
    "fpdf": "fpdf"
}

def install_missing_packages():
    missing = []
    for pip_name, module_name in REQUIRED_PACKAGES.items():
        try:
            importlib.import_module(module_name)
        except ImportError:
            missing.append(pip_name)

    if missing:
        print(f"Installing missing packages: {missing}")
        subprocess.check_call([sys.executable, "-m", "pip", "install", *missing])
    else:
        print("All required packages are already installed.")

    install_missing_packages()
