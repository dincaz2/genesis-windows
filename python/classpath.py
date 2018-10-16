import sys
import os

# Get all classpaths of project
def get_classpaths(project_dir):
    ans = []
    path_to_reports = os.path.join(project_dir, 'target\\surefire-reports')
    if os.path.isdir(path_to_reports):
        ans.extend(parse_tests(path_to_reports))
    for filename in os.listdir(project_dir):
        file_abs_path = os.path.join(project_dir, filename)
        if os.path.isdir(file_abs_path) and filename not in ['src', '.git']:
            ans.extend(get_tests(file_abs_path))
    return ans


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print 'Must specify project location'
        exit(1)
    project_dir = sys.argv[1]