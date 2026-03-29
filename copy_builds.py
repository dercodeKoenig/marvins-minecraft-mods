import shutil
import os
from pathlib import Path

# Configuration
MOD_FOLDERS = [
    "advRocketry", "aos_basic_fluid", "aos_core", "ar_machines",
    "ARLib", "aw_generators", "better_pipes", "finite_water"
]
TARGET_DIR = Path("./build")

def clean_and_copy():
    # Ensure the root destination exists
    TARGET_DIR.mkdir(exist_ok=True)
    
    for mod in MOD_FOLDERS:
        source_path = Path(mod) / "build" / "libs"
        
        if not source_path.exists():
            print(f"[-] Skipping {mod}: Path not found.")
            continue

        # 1. Get all .jar files in the source, sorted by modification time (oldest first)
        jars = sorted(
            source_path.glob("*.jar"), 
            key=os.path.getmtime
        )

        if not jars:
            print(f"[!] No jars found in {mod}")
            continue

        # 2. Delete all but the 2 most recent files in the source folder
        if len(jars) > 2:
            files_to_delete = jars[:-2]  # Everything except the last two
            for old_jar in files_to_delete:
                print(f"    [Deleting old] {old_jar.name}")
                old_jar.unlink()
            
            # Refresh the list after deletion for the copy step
            jars = jars[-2:]

        # 3. Copy the remaining (most recent) jars to the root build folder
        for jar in jars:
            print(f"[+] Copying: {jar.name} from {mod}")
            shutil.copy2(jar, TARGET_DIR / jar.name)

    print("\n--- Cleanup and Copy Complete ---")

if __name__ == "__main__":
    clean_and_copy()