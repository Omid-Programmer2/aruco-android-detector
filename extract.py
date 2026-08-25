import os
import sys

# Force UTF-8 encoding for output
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

# Folders to exclude from scanning
exclude = {'.venv', '.git', '.idea', '__pycache__'}

def process(path='.'):
    abs_path = os.path.abspath(path)
    for item in os.listdir(abs_path):
        full = os.path.join(abs_path, item)
        if os.path.isdir(full):
            if item not in exclude:
                process(full)
        elif item.endswith(('.txt', '.html', '.py', 'ipynb')) and not item.endswith('.pyc'):
            rel_path = os.path.relpath(full, start='.')
            print(f'===== {rel_path} =====')
            try:
                with open(full, 'r', encoding='utf-8') as f:
                    content = f.read()
                print(content)
            except UnicodeDecodeError:
                with open(full, 'r', encoding='latin-1') as f:
                    content = f.read()
                print(content)
            except Exception as e:
                print(f'Error reading file: {e}')

if __name__ == '__main__':
    process()