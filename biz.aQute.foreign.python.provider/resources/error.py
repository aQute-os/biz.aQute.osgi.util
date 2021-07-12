import sys

print("Error", file=sys.stderr)

raise Exception("Some error")