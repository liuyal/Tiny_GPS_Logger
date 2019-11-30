import shutil

total, used, free = shutil.disk_usage("/")

print("Total: %d B" % (total))
print("Used: %d B" % (used))
print("Free: %d B" % (free))