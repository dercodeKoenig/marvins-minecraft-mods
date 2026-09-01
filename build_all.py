import os
import subprocess
import sys
from pathlib import Path

os.environ["JAVA_HOME"] = str(Path.home() / ".jdks" / "ms-21.0.10")

# Mods to build. These are the standalone Gradle projects in this repo;
# each ships its own `gradlew` wrapper and `build.gradle`.
MOD_FOLDERS = [
    "ARLib",
    "advRocketry",
    "aos_basic_fluid",
    "aos_core",
    #"ar_machines",
    "aos_workshop_expansion",
    "aw_generators",
    #"aw_npc",
    "aw_vehicles",
    #"aw_worksite",
    "better_pipes",
    "finite_water",
    #"research_station",
]


PROJECT_ROOT = Path(__file__).resolve().parent
os.chdir(PROJECT_ROOT)


def build_one(mod: str):
    wrapper_name = "gradlew.bat" if os.name == "nt" else "gradlew"

    wrapper_path = os.path.abspath(os.path.join(mod, wrapper_name))

    cmd = [wrapper_path, "build"]

    print(f"\n=== Building {mod} ===")
    print(f"$ {wrapper_name} build  (in {mod})")

    result = subprocess.run(cmd, cwd=mod)

    if result.returncode == 0:
        print(f"[+] {mod} built successfully")
        return True

    print(f"[-] {mod} build failed (exit code {result.returncode})")
    return False

def build_all_mods():
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Building {len(MOD_FOLDERS)} mod(s): {', '.join(MOD_FOLDERS)}\n")

    failures = []
    for mod in MOD_FOLDERS:
        if not build_one(mod):
            failures.append(mod)

    print("\n" + "=" * 50)
    if failures:
        print(f"Builds failed for: {', '.join(failures)}")
        print(f"({len(MOD_FOLDERS) - len(failures)}/{len(MOD_FOLDERS)} succeeded)")
    else:
        print(f"All {len(MOD_FOLDERS)} mods built successfully!")
    return len(failures) == 0

if __name__ == "__main__":
    success = build_all_mods()
    sys.exit(0 if success else 1)
