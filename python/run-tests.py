import xml.etree.ElementTree as ET
import os
import sys


class Class_Test(object):

    def __init__(self, xml):
        root = ET.parse(xml).getroot()
        self.name = root.get('name')
        self.testcases = [Method_Test(testcase, self) for testcase in root.findall('testcase')]

    def __repr__(self):
        return self.name

    def __eq__(self, other):
        return isinstance(other, Class_Test) and self.name == other.name


class Method_Test(object):

    def __init__(self, testcase, parent):
        self.parent = parent
        self.name = testcase.get('name')
        self.passed = testcase.find('failure') is None and testcase.find('error') is None

    def get_class_relative_name(self):
        return self.name

    def __repr__(self):
        passed_string = 0 if self.passed else 1
        return '{} {} {}'.format(self.parent.name, self.name, passed_string)

    def __eq__(self, other):
        return isinstance(other, Method_Test) and self.name == other.name


# Return parsed tests of the reports dir
def parse_tests(path_to_reports):
    return [Class_Test(os.path.join(path_to_reports, filename)) for filename in os.listdir(path_to_reports) if filename.endswith(".xml")]


# Gets path to maven project directory and returns parsed
def get_tests(project_dir):
    ans = []
    path_to_reports = os.path.join(project_dir, 'target\\surefire-reports')
    if os.path.isdir(path_to_reports):
        ans.extend(parse_tests(path_to_reports))
    for filename in os.listdir(project_dir):
        file_abs_path = os.path.join(project_dir, filename)
        if os.path.isdir(file_abs_path) and filename not in ['src', '.git']:
            ans.extend(get_tests(file_abs_path))
    return ans


# Returns all testcases of given test classes
def get_testcases(test_classes):
    ans = []
    for testcases in [test_class.testcases for test_class in test_classes]:
        ans += testcases
    return ans


def export_as_txt(testcases, file_path):
    with open(file_path, 'w') as file:
        lines = [str(testcase) for testcase in testcases]
        file.writelines('\n'.join(lines))
        # file.writelines(lines)


if __name__ == '__main__':
    # os.chdir('..')
    if len(sys.argv) < 2:
        print 'Must specify project location'
        exit(1)
    project_dir = sys.argv[1]
    os.chdir(project_dir)

    with open('case.conf', 'r') as config:
        for line in config.read().splitlines():
            key, value = [a.strip(' ') for a in line.split('=')]
            if key == 'src':
                src = value
            elif key == 'testcase':
                testcases_file = value

    run_tests = False if len(sys.argv) < 3 else sys.argv[2] == 'run-tests'
    if run_tests:
        os.system('mvn clean test -fn')
    testcases = get_testcases(get_tests(src))
    export_as_txt(testcases, testcases_file)