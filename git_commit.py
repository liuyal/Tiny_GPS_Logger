import sys, time, subprocess, os
from subprocess import Popen, PIPE


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


def cslog(msg, flag="info"):
    if flag == "info":
        print(bcolors.OKGREEN + msg + bcolors.ENDC)
    elif flag == "error":
        print(bcolors.FAIL + msg + bcolors.ENDC)


if __name__ == "__main__":

    p = Popen(['git', 'config', '--global', 'user.email'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog("Git Email: " + output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    p = Popen(['git', 'config', '--global', 'user.email', '"jerrylarryliu@gmail.com"'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog("Set Git Email: " + output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    p = Popen(['git', 'rm', '-r', '--cached', '.'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog("Clear git cache\n" + output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    p = Popen(['git', 'add', '.'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog("Git add\n" + output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    msg = input('Enter commit message:')
    p = Popen(['git', 'commit', '-am', "'" + msg + "'"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog(output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    time.sleep(1)

    p = Popen(['git', 'push'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    if output: cslog(output.decode('utf-8'))
    elif err: cslog(err.decode('utf-8'), "error")

    # p = Popen(['git', 'config', '--global', 'user.email', '"liuyal@sfu.ca"'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    # output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    # if output: cslog("Set Git Email: " + output.decode('utf-8'))
    # else: cslog(err.decode('utf-8'), "error")

    time.sleep(1)
