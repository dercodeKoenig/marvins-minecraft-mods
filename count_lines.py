import os

def count_lines_in_file(file_path):
    """Counts the number of lines in a file."""
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            return sum(1 for _ in f)
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return 0

def count_all_lines(directory):
    """Counts lines in .java and .json files recursively in a directory."""
    total_lines = 0

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(('.java', '.json')):
                file_path = os.path.join(root, file)
                total_lines += count_lines_in_file(file_path)

    return total_lines


total_lines = count_all_lines(".")
print(f"Total lines of code in Java & JSON files: {total_lines}")
input("enter to continue")