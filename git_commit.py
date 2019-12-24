import sys, time, subprocess, os

cmd = r"git config --global user.email"
os.system(cmd)

cmd = r'git add .'
os.system(cmd)

cmd = r'git config --global user.email "jerrylarryliu@gmail.com"'
os.system(cmd)

x = input('Enter commit message:')
# x = "update"

cmd = 'git commit -am "' + x + '"'
os.system(cmd)

time.sleep(1)

cmd = r'git push'
os.system(cmd)

# cmd = r'git config --global user.email "liuyal@sfu.ca"'
# os.system(cmd)

time.sleep(1)
